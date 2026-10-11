package com.cleannrooster.rpg_minibosses.magic;

import com.cleannrooster.rpg_minibosses.magic.fx.CueArgs;
import com.cleannrooster.rpg_minibosses.magic.fx.FxCues;
import com.cleannrooster.rpg_minibosses.magic.fx.FxEmitter;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * How an ability looks and sounds at each moment of its life, server side: the wind-up (sound and
 * telegraph), the release (sound and the shape of it going off) and each landing (sound and shape).
 *
 * <p>Defaults come from the school ({@link BossSounds#of}) and the delivery: a nova for a circle or ring
 * around the caster, an explosion for one at the target, a cone or beam for those shapes, a falling
 * blast for a meteor. {@link AbilitySpec.Presentation} overrides any of them.</p>
 */
public final class AbilityPresentation {
    private static final float VOLUME = 1.5f;

    private AbilityPresentation() {
    }

    /**
     * The wind-up: the school's gathering sound on the caster, and a telegraph of where it will land if the
     * delivery has an area. {@code windupTicks} is when it lands.
     */
    public static void windup(ServerWorld world, LivingEntity caster, AbilitySpec spec, CastTarget target, int windupTicks) {
        var presentation = spec.presentation();
        sound(world, caster.getPos(), pick(presentation.windupSound(), BossSounds.of(spec.lookSchool()).windup()));
        if (!presentation.telegraph() || windupTicks <= 0) {
            return;
        }
        var area = telegraphedArea(caster, spec, target);
        if (area != null) {
            FxEmitter.telegraph(world, area, windupTicks, spec.intent() == AbilitySpec.Intent.HELPFUL);
        }
    }

    /** The moment it goes off: the release sound on the caster and the shape of the delivery. */
    public static void release(ServerWorld world, LivingEntity caster, AbilitySpec spec, CastTarget target) {
        var presentation = spec.presentation();
        sound(world, caster.getPos(), pick(presentation.releaseSound(), BossSounds.of(spec.lookSchool()).release()));
        long seed = world.random.nextLong();
        var school = spec.lookSchool();
        if (presentation.releaseCue() != null) {
            FxEmitter.emit(world, presentation.releaseCue(), CueArgs.between(school, caster.getEyePos(), target.position(), 1.0f, 1, seed));
            return;
        }
        switch (spec.delivery()) {
            case Delivery.Area area -> {
                var placed = area.shape().at(anchor(caster, area.anchor(), target), target.yaw());
                emitAreaShape(world, school, placed, area.anchor(), caster, seed);
            }
            case Delivery.Projectile projectile ->
                    FxEmitter.emit(world, FxCues.PULSE, CueArgs.at(school, caster.getEyePos(), 0.8f, 0, seed));
            case Delivery.Beam beam -> {
                var eye = caster.getEyePos();
                var direction = AbilityExecutor.beamDirection(caster, target);
                double length = beamLength(caster, beam, target);
                FxEmitter.emit(world, FxCues.BEAM, CueArgs.between(school, eye, eye.add(direction.multiply(length)), 0.6f, 1, seed));
                // The core is only the middle of it: the cone shows how wide it really hits.
                FxEmitter.emit(world, FxCues.CONE, CueArgs.cone(school, eye, direction, beam.halfAngle(), (float) length, 2, seed));
            }
            case Delivery.Direct direct -> {
            }
            case Delivery.Meteor meteor -> {
            }
            case Delivery.Cloud cloud -> {
            }
        }
    }

    /**
     * The warning for a beam that is about to fire along {@code target}: a thin line down its middle, four
     * lines along the edges of the cone it actually hits, and motes in a ring as wide as it is where it
     * reaches the aim. Meant to be repeated every few ticks of the wind-up, so it pulses; {@code strength}
     * (0 to 1) grows it toward the release.
     */
    public static void beamGuide(ServerWorld world, LivingEntity caster, AbilitySpec spec, CastTarget target, float strength) {
        if (!(spec.delivery() instanceof Delivery.Beam beam)) {
            throw new IllegalArgumentException(spec.id() + " does not deliver a beam");
        }
        var school = spec.lookSchool();
        long seed = world.random.nextLong();
        var eye = caster.getEyePos();
        var direction = AbilityExecutor.beamDirection(caster, target);
        double length = beamLength(caster, beam, target);
        FxEmitter.emit(world, FxCues.BEAM, CueArgs.between(school, eye, eye.add(direction.multiply(length)),
                0.08f + 0.12f * strength, 0, seed));

        var right = direction.crossProduct(new Vec3d(0.0, 1.0, 0.0));
        right = right.lengthSquared() < 1.0e-6 ? new Vec3d(1.0, 0.0, 0.0) : right.normalize();
        var up = right.crossProduct(direction).normalize();
        double cos = Math.cos(Math.toRadians(beam.halfAngle()));
        double sin = Math.sin(Math.toRadians(beam.halfAngle()));
        for (var side : new Vec3d[]{right, right.negate(), up, up.negate()}) {
            var edge = direction.multiply(cos).add(side.multiply(sin));
            FxEmitter.emit(world, FxCues.BEAM, CueArgs.between(school, eye, eye.add(edge.multiply(length / cos)),
                    0.05f + 0.05f * strength, 0, seed));
        }
        // Its width where it reaches the aim, in motes.
        double reach = target.entity() != null ? eye.distanceTo(target.entity().getBoundingBox().getCenter()) : eye.distanceTo(target.position());
        reach = Math.min(reach, beam.range());
        FxEmitter.emit(world, FxCues.WISP, CueArgs.at(school, eye.add(direction.multiply(reach)),
                (float) (reach * Math.tan(Math.toRadians(beam.halfAngle()))), 0, seed));
    }

    /** How far a beam's visuals run: a little past what it is aimed at, never past its range. */
    private static double beamLength(LivingEntity caster, Delivery.Beam beam, CastTarget target) {
        var eye = caster.getEyePos();
        var aim = target.entity() != null ? target.entity().getBoundingBox().getCenter() : target.position();
        return Math.min(beam.range(), eye.distanceTo(aim) + 4.0);
    }

    /**
     * Something landed at {@code at}: its sound and shape. {@code radius} is the area it covered (0 for a
     * single hit); {@code falling} for a meteor coming down.
     */
    public static void impact(ServerWorld world, AbilitySpec spec, Vec3d at, float radius, boolean falling) {
        var presentation = spec.presentation();
        sound(world, at, pick(presentation.impactSound(), BossSounds.of(spec.lookSchool()).impact()));
        long seed = world.random.nextLong();
        var school = spec.lookSchool();
        if (presentation.impactCue() != null) {
            FxEmitter.emit(world, presentation.impactCue(), CueArgs.at(school, at, Math.max(radius, 1.0f), 1, seed));
            return;
        }
        if (falling) {
            FxEmitter.emit(world, FxCues.BLAST_FALLING, CueArgs.at(school, at, Math.max(radius, 1.5f), 1, seed));
        }
        if (radius > 0.0f) {
            FxEmitter.emit(world, FxCues.EXPLOSION, CueArgs.at(school, at, radius, 1, seed));
        } else {
            FxEmitter.emit(world, FxCues.PULSE, CueArgs.at(school, at, 1.0f, 0, seed));
        }
    }

    /** One pulse of a cloud: motes over its area. */
    public static void cloudPulse(ServerWorld world, AbilitySpec spec, Vec3d at, float radius) {
        FxEmitter.emit(world, FxCues.WISP, CueArgs.at(spec.lookSchool(), at, radius, 1, world.random.nextLong()));
    }

    /** The area the caster's wind-up warns of, placed and turned, or null if the delivery has none. */
    static @Nullable TelegraphArea telegraphedArea(LivingEntity caster, AbilitySpec spec, CastTarget target) {
        return switch (spec.delivery()) {
            case Delivery.Area area -> area.shape().at(anchor(caster, area.anchor(), target), target.yaw());
            case Delivery.Cloud cloud -> cloud.spec().shape().at(anchor(caster, cloud.anchor(), target), target.yaw());
            case Delivery.Meteor meteor -> meteor.spec().burst() == null ? null
                    : meteor.spec().burst().shape().at(target.position(), target.yaw());
            case Delivery.Projectile projectile -> null;
            case Delivery.Beam beam -> null;
            case Delivery.Direct direct -> null;
        };
    }

    private static void emitAreaShape(ServerWorld world, MagicSchool school, TelegraphArea placed, Delivery.Anchor anchor,
                                      LivingEntity caster, long seed) {
        var origin = placed.origin();
        switch (placed) {
            case TelegraphArea.Circle circle -> FxEmitter.emit(world,
                    anchor == Delivery.Anchor.CASTER ? FxCues.NOVA : FxCues.EXPLOSION,
                    CueArgs.at(school, origin, circle.radius(), 1, seed));
            case TelegraphArea.Ring ring -> FxEmitter.emit(world, FxCues.NOVA, CueArgs.at(school, origin, ring.outer(), 1, seed));
            case TelegraphArea.Cone cone -> {
                var axis = TelegraphArea.forward(cone.yaw());
                var mouth = origin.add(0.0, caster.getHeight() * 0.6, 0.0);
                FxEmitter.emit(world, FxCues.CONE, CueArgs.cone(school, mouth, axis, cone.halfAngle(), cone.range(), 1, seed));
            }
            case TelegraphArea.Line line -> {
                var from = origin.add(0.0, 1.0, 0.0);
                var to = from.add(TelegraphArea.forward(line.yaw()).multiply(line.length()));
                FxEmitter.emit(world, FxCues.BEAM, CueArgs.between(school, from, to, line.halfWidth() * 2.0f, 1, seed));
            }
        }
    }

    static Vec3d anchor(LivingEntity caster, Delivery.Anchor anchor, CastTarget target) {
        return anchor == Delivery.Anchor.CASTER ? caster.getPos() : target.position();
    }

    private static SoundEvent pick(@Nullable SoundEvent override, SoundEvent fallback) {
        return override != null ? override : fallback;
    }

    private static void sound(ServerWorld world, Vec3d at, SoundEvent sound) {
        FxEmitter.sound(world, at, sound, VOLUME, 0.9f + world.random.nextFloat() * 0.2f);
    }

    /** For cues a caller supplies itself. */
    public static void emit(ServerWorld world, Identifier cue, CueArgs args) {
        FxEmitter.emit(world, cue, args);
    }
}

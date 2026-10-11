package com.cleannrooster.rpg_minibosses.magic;

import com.cleannrooster.rpg_minibosses.magic.entity.BossCloud;
import com.cleannrooster.rpg_minibosses.magic.entity.BossProjectile;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.OptionalDouble;

/**
 * Runs an {@link AbilitySpec}: resolves the caster's power once, then hands the impacts to whatever its
 * {@link Delivery} reaches. Server only.
 */
public final class AbilityExecutor {
    private AbilityExecutor() {
    }

    public static boolean cast(ServerWorld world, LivingEntity caster, AbilitySpec spec, CastTarget target) {
        return cast(world, caster, spec, target, OptionalDouble.empty());
    }

    /**
     * @param powerOverride use this base power instead of the caster's (debug casts, scripted moments)
     * @return false when there was nothing to aim at (a direct cast with no target)
     */
    public static boolean cast(ServerWorld world, LivingEntity caster, AbilitySpec spec, CastTarget target,
                               OptionalDouble powerOverride) {
        var context = ImpactContext.of(caster, spec.school(), caster.getPos(), null);
        if (powerOverride.isPresent()) {
            context = context.withBasePower(powerOverride.getAsDouble());
        }
        if (spec.delivery() instanceof Delivery.Direct
                && (target.entity() == null || !reaches(spec, caster, target.entity()))) {
            return false;
        }
        AbilityPresentation.release(world, caster, spec, target);
        return switch (spec.delivery()) {
            case Delivery.Direct direct -> {
                var entity = target.entity();
                applyAll(spec, context, entity);
                AbilityPresentation.impact(world, spec, entity.getBoundingBox().getCenter(), 0.0f, false);
                yield true;
            }
            case Delivery.Area area -> {
                applyArea(world, spec, context, area, anchor(caster, area.anchor(), target), target.yaw(), null);
                yield true;
            }
            case Delivery.Beam beam -> {
                var eye = caster.getEyePos();
                var direction = beamDirection(caster, target);
                double cos = Math.cos(Math.toRadians(beam.halfAngle()));
                var reach = new net.minecraft.util.math.Box(eye, eye).expand(beam.range());
                double rangeSq = beam.range() * beam.range();
                var struck = world.getEntitiesByClass(LivingEntity.class, reach, entity -> {
                    if (!reaches(spec, caster, entity)) {
                        return false;
                    }
                    var to = entity.getBoundingBox().getCenter().subtract(eye);
                    double lengthSq = to.lengthSquared();
                    return lengthSq <= rangeSq && (lengthSq < 1.0e-6 || to.normalize().dotProduct(direction) >= cos);
                });
                for (LivingEntity entity : struck) {
                    applyAll(spec, context, entity);
                }
                yield true;
            }
            case Delivery.Projectile projectile -> {
                var from = caster.getEyePos();
                var aim = target.entity() != null ? target.entity().getBoundingBox().getCenter() : target.position();
                BossProjectile.launch(world, caster, spec, projectile.spec(), context, from, aim.subtract(from), target.entity(), false);
                yield true;
            }
            case Delivery.Meteor meteor -> {
                var from = target.position().add(0.0, meteor.height(), 0.0);
                BossProjectile.launch(world, caster, spec, meteor.spec(), context, from, new Vec3d(0.0, -1.0, 0.0), null, true);
                yield true;
            }
            case Delivery.Cloud cloud -> {
                BossCloud.spawn(world, caster, spec, cloud.spec(), context, anchor(caster, cloud.anchor(), target), target.yaw());
                yield true;
            }
        };
    }

    /**
     * The wind-up of a cast that lands {@code windupTicks} from now: its sound, and a telegraph of its area.
     * {@code CombatAction} does this itself as an action starts; call it before a delayed {@link #cast}.
     */
    public static void windup(ServerWorld world, LivingEntity caster, AbilitySpec spec, CastTarget target, int windupTicks) {
        AbilityPresentation.windup(world, caster, spec, target, windupTicks);
    }

    /**
     * Fire one projectile of {@code spec} along {@code direction} rather than straight at the target — for a
     * brain that fans a volley out itself. {@code spec} must deliver a projectile; it still homes on the
     * target if its spec says so.
     */
    public static void castProjectile(ServerWorld world, LivingEntity caster, AbilitySpec spec, CastTarget target, Vec3d direction) {
        if (!(spec.delivery() instanceof Delivery.Projectile projectile)) {
            throw new IllegalArgumentException(spec.id() + " does not deliver a projectile");
        }
        AbilityPresentation.release(world, caster, spec, target);
        var context = ImpactContext.of(caster, spec.school(), caster.getPos(), null);
        BossProjectile.launch(world, caster, spec, projectile.spec(), context, caster.getEyePos(), direction, target.entity(), false);
    }

    /** Whether {@code spec} may reach {@code entity} at all, by its intent. */
    public static boolean reaches(AbilitySpec spec, LivingEntity caster, Entity entity) {
        return spec.intent() == AbilitySpec.Intent.HELPFUL
                ? Relations.canHelp(caster, entity)
                : Relations.canHarm(caster, entity);
    }

    /** Every impact of {@code spec} against one target. */
    public static void applyAll(AbilitySpec spec, ImpactContext context, LivingEntity target) {
        for (Impact impact : spec.impacts()) {
            impact.apply(context, target);
        }
    }

    /**
     * Applies {@code spec} to everyone {@code area} reaches when centred on {@code origin}.
     *
     * @param skip an entity already struck by this delivery (a projectile's direct hit), or null
     * @return who was struck
     */
    public static List<LivingEntity> applyArea(ServerWorld world, AbilitySpec spec, ImpactContext context,
                                               Delivery.Area area, Vec3d origin, float yaw, @Nullable Entity skip) {
        TelegraphArea placed = area.shape().at(origin, yaw);
        var struck = AbilityTargets.within(world, placed, area.height(),
                entity -> entity != skip && reaches(spec, context.caster(), entity));
        var atOrigin = context.withOrigin(origin);
        for (LivingEntity entity : struck) {
            applyAll(spec, atOrigin, entity);
        }
        return struck;
    }

    /** Unit direction of a beam: toward the target's middle, or along the caster's gaze without one. */
    public static Vec3d beamDirection(LivingEntity caster, CastTarget target) {
        var eye = caster.getEyePos();
        var aim = target.entity() != null ? target.entity().getBoundingBox().getCenter() : target.position();
        var direction = aim.subtract(eye);
        return direction.lengthSquared() < 1.0e-6 ? caster.getRotationVector() : direction.normalize();
    }

    private static Vec3d anchor(LivingEntity caster, Delivery.Anchor anchor, CastTarget target) {
        return anchor == Delivery.Anchor.CASTER ? caster.getPos() : target.position();
    }
}

package com.cleannrooster.rpg_minibosses.entity.combat.geometry;

import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

/**
 * The one place attack geometry is defined.
 *
 * <p>Hit detection and the particle slash both call into this class, which is what keeps them from
 * drifting apart. There is no separate "hitbox" and "visual" — there is a swept surface, and both read
 * from it:
 *
 * <ul>
 *   <li>{@code s} is progress through the swing, 0 at the start of the sweep and 1 at the end;</li>
 *   <li>{@code t} is distance along the blade, 0 at the inner radius and 1 at the outer reach.</li>
 * </ul>
 *
 * <p>{@link #surfacePoint} maps {@code (s, t)} to a world position for every shape family. Damage asks "is
 * this entity inside the slice swept since the last tick"; the emitter asks "give me a random point on
 * that same slice". Everything is computed from the captured frame — no marker entities, no per-attack
 * collision boxes to keep in sync.
 *
 * <p>Ported from the Divine Encounters combat system, trimmed to what these grounded mobs need.
 */
public final class AttackGeometry {
    /** Nudge so degenerate (zero-length) attacks cannot produce NaNs. */
    private static final double EPSILON = 1.0e-6;

    private AttackGeometry() {
    }

    // --- frame capture ---------------------------------------------------------------------------

    /**
     * Capture the frame for a swing about to commit: the swing's local yaw and pitch applied to the
     * attacker's facing, then the origin offset resolved against the resulting basis.
     *
     * <p>Handedness biases the lateral offset, so a right-handed cut leaves the right shoulder even though
     * the attack only states a magnitude.
     */
    public static AttackFrame capture(AttackVolume volume, LivingEntity attacker, boolean mirrored,
                                      float rollOffset) {
        var swing = volume.swing();
        var yaw = MathHelper.wrapDegrees(attacker.getYaw() + swing.yawOffset());
        // Vertical aim comes from the attacker's own aim channel, not from getPitch(). Vanilla's
        // LookControl zeroes pitch every tick, after custom AI has run, so pitch is not a value a mob
        // can own — see AttackAim. Attackers that do not opt in, and radial impacts that must not be
        // tilted at all, fall through to level, which is exactly what this did before.
        var aim = attacker instanceof AttackAim.Aiming aiming && AttackAim.appliesTo(volume)
                ? aiming.attackPitch()
                : 0.0f;
        var pitch = MathHelper.wrapDegrees(aim + swing.pitchOffset());
        var scale = attacker.getScaleFactor();
        var base = new AttackFrame(attacker.getPos(), yaw, pitch, rollOffset, mirrored, (float) scale);
        var lateral = volume.offsetLateral() * swing.handedness().lateralSign() * (mirrored ? -1.0 : 1.0);
        var origin = attacker.getPos()
                .add(base.forward().multiply(volume.offsetForward() * scale))
                .add(base.right().multiply(lateral * scale))
                .add(0.0, volume.offsetVertical() * scale, 0.0);
        return base.withOrigin(origin);
    }

    // --- the swept surface -----------------------------------------------------------------------

    /**
     * World position on the attack surface at swing progress {@code s} and blade fraction {@code t}.
     *
     * <p>This is the function that makes the visual and the hitbox the same object.
     */
    public static Vec3d surfacePoint(AttackVolume volume, AttackFrame frame, float s, float t) {
        var forward = frame.forward();
        return switch (volume.shape().family()) {
            case ARC -> {
                var angle = Math.toRadians(sweepAngleDegrees(volume, frame, s));
                var plane = planeAxis(volume, frame);
                var direction = forward.multiply(Math.cos(angle)).add(plane.multiply(Math.sin(angle)));
                yield frame.origin().add(direction.multiply(radiusAt(volume, frame, t)));
            }
            // A thrust extends: at progress s the lane reaches s of the way out, and t runs along whatever
            // is currently extended, so t = 1 sits at the advancing point.
            case LANE -> frame.origin().add(forward.multiply((volume.innerRadius()
                    + (volume.range() - volume.innerRadius()) * laneExtension(volume, s) * t) * frame.scale()));
            // A path re-anchors to the attacker each tick, so t runs backward along the facing vector from
            // the current position: t = 1 is the advancing front.
            case PATH -> frame.origin().add(forward.multiply(
                    (volume.range() * 0.5 * frame.scale()) * (t - 1.0)));
            // A radial impact has no swing direction, so s sweeps the full circle instead.
            case RADIAL -> {
                var angle = Math.toRadians(s * 360.0);
                var plane = planeAxis(volume, frame);
                var direction = forward.multiply(Math.cos(angle)).add(plane.multiply(Math.sin(angle)));
                yield frame.origin().add(direction.multiply(radiusAt(volume, frame, t)));
            }
        };
    }

    /**
     * Angle of the swing at progress {@code s}, in degrees from the facing vector.
     *
     * <p>The direction of travel comes from the {@link SwingPath} — {@code s} always runs forward and the
     * start/end angles carry the direction — so the arc sweeps the way the animation swings rather than the
     * way the entity happens to be facing.
     */
    public static float sweepAngleDegrees(AttackVolume volume, AttackFrame frame, float s) {
        var angle = volume.swing().angleAt(s);
        return frame.mirrored() ? -angle : angle;
    }

    /**
     * The direction the strike is travelling at progress {@code s}.
     *
     * <p>For an arc this is the tangent to the swing — the way the blade is actually moving, which for a
     * horizontal sweep is sideways, not outward. That gives knockback a direction of its own instead of
     * always shoving radially away from the attacker.
     */
    public static Vec3d strikeDirection(AttackVolume volume, AttackFrame frame, float s) {
        return switch (volume.shape().family()) {
            case ARC -> {
                var angle = Math.toRadians(sweepAngleDegrees(volume, frame, s));
                var plane = planeAxis(volume, frame);
                var tangent = frame.forward().multiply(-Math.sin(angle))
                        .add(plane.multiply(Math.cos(angle)));
                var forwardTravel = volume.swing().travelsPositive() != frame.mirrored();
                yield forwardTravel ? tangent : tangent.multiply(-1.0);
            }
            case LANE, PATH, RADIAL -> frame.forward();
        };
    }

    /**
     * How much of an attack's knockback follows {@link #strikeDirection} rather than pushing radially away
     * from the origin, 0-1. Derived from the shape, because the right answer is a property of what kind of
     * motion the attack is: a cleave's whole character is lateral travel, a lane drives you down its own
     * line, and a shockwave has no direction but outward.
     */
    public static double knockbackAlignment(AttackVolume volume) {
        return switch (volume.shape().family()) {
            case ARC -> 0.55;
            case LANE, PATH -> 0.75;
            case RADIAL -> 0.0;
        };
    }

    /** How far a lane has extended at progress {@code s}, 0-1 — the lane equivalent of the swing angle. */
    public static float laneExtension(AttackVolume volume, float s) {
        return volume.swing().dynamics().apply(s);
    }

    /** Distance from the origin at blade fraction {@code t}, in the frame's scale. */
    public static double radiusAt(AttackVolume volume, AttackFrame frame, float t) {
        return (volume.innerRadius() + (volume.range() - volume.innerRadius())
                * MathHelper.clamp(t, 0.0f, 1.0f)) * frame.scale();
    }

    public static double rangeOf(AttackVolume volume, AttackFrame frame) {
        return volume.range() * frame.scale();
    }

    public static double widthOf(AttackVolume volume, AttackFrame frame) {
        return volume.width() * frame.scale();
    }

    /** Half-thickness of the volume perpendicular to the swing plane. */
    public static double halfThickness(AttackVolume volume, AttackFrame frame) {
        return volume.verticalExtent() * 0.5 * frame.scale();
    }

    /** In-plane lateral axis: the direction positive swing angles point toward. */
    public static Vec3d planeAxis(AttackVolume volume, AttackFrame frame) {
        var roll = volume.planeRoll();
        return frame.planeAxis(frame.mirrored() ? -roll : roll);
    }

    /** Normal of the swing plane — the direction the damage volume has thickness in. */
    public static Vec3d planeNormal(AttackVolume volume, AttackFrame frame) {
        var roll = volume.planeRoll();
        return frame.planeNormal(frame.mirrored() ? -roll : roll);
    }

    // --- broad phase -----------------------------------------------------------------------------

    /** A box guaranteed to contain the whole attack, used to pre-filter candidate victims. */
    public static Box bounds(AttackVolume volume, AttackFrame frame, @Nullable Vec3d previousOrigin) {
        var pad = Math.max(volume.width(), volume.verticalExtent()) * 0.5 * frame.scale() + 1.0;
        return switch (volume.shape().family()) {
            case ARC, RADIAL -> {
                var reach = rangeOf(volume, frame) * 2 + pad;
                yield Box.of(frame.origin(), reach, reach, reach);
            }
            case LANE -> {
                var tip = frame.origin().add(frame.forward().multiply(rangeOf(volume, frame)));
                yield new Box(frame.origin(), tip).expand(pad);
            }
            case PATH -> {
                var from = previousOrigin != null ? previousOrigin : frame.origin();
                yield new Box(from, frame.origin()).expand(pad);
            }
        };
    }

    // --- hit detection ---------------------------------------------------------------------------

    /**
     * Whether {@code victimBox} is inside the slice of the attack surface swept between progress
     * {@code s0} and {@code s1}.
     *
     * <p>Server-authoritative and the sole arbiter of whether an attack connects. It reads the same volume
     * and frame the particle slash is drawn from, so a player who dodges what they saw has dodged what
     * actually existed.
     *
     * @param previousOrigin the attacker's origin last tick, required only by {@link AttackShape#CHARGE_PATH}
     */
    public static boolean hits(AttackVolume volume, AttackFrame frame, float s0, float s1,
                               Box victimBox, @Nullable Vec3d previousOrigin) {
        return switch (volume.shape().family()) {
            case ARC -> hitsArc(volume, frame, s0, s1, victimBox);
            case LANE -> hitsLane(volume, frame, s1, victimBox);
            case PATH -> hitsPath(volume, frame, victimBox, previousOrigin);
            case RADIAL -> hitsRadial(volume, frame, victimBox);
        };
    }

    private static boolean hitsArc(AttackVolume volume, AttackFrame frame, float s0, float s1, Box victim) {
        var forward = frame.forward();
        var plane = planeAxis(volume, frame);
        var normal = planeNormal(volume, frame);

        var toVictim = victim.getCenter().subtract(frame.origin());
        var along = toVictim.dotProduct(forward);
        var lateral = toVictim.dotProduct(plane);
        var offPlane = toVictim.dotProduct(normal);

        // Out of the swing plane by more than the volume's thickness plus the victim's own extent there.
        if (Math.abs(offPlane) > halfThickness(volume, frame) + boxExtent(victim, normal)) {
            return false;
        }

        var radius = Math.sqrt(along * along + lateral * lateral);
        if (radius < EPSILON) {
            // Standing exactly on the origin: inside everything that is not ring-shaped.
            return volume.innerRadius() <= 0.5;
        }

        var radialDir = forward.multiply(along / radius).add(plane.multiply(lateral / radius));
        var radialPad = boxExtent(victim, radialDir);
        if (radius > rangeOf(volume, frame) + radialPad
                || radius < volume.innerRadius() * frame.scale() - radialPad) {
            return false;
        }

        // The angular window swept this tick, widened by how much of an angle the victim subtends at this
        // radius. Without that widening a fast sweep would tunnel straight past a nearby target.
        var tangent = forward.multiply(-lateral / radius).add(plane.multiply(along / radius));
        var angularPad = (float) Math.toDegrees(Math.atan2(boxExtent(victim, tangent), radius));

        var angle = (float) Math.toDegrees(Math.atan2(lateral, along));
        var a0 = sweepAngleDegrees(volume, frame, s0);
        var a1 = sweepAngleDegrees(volume, frame, s1);
        var lowest = Math.min(a0, a1) - angularPad;
        var highest = Math.max(a0, a1) + angularPad;
        return angle >= lowest && angle <= highest;
    }

    private static boolean hitsLane(AttackVolume volume, AttackFrame frame, float extension, Box victim) {
        var forward = frame.forward();
        var plane = planeAxis(volume, frame);
        var normal = planeNormal(volume, frame);

        var toVictim = victim.getCenter().subtract(frame.origin());
        var along = toVictim.dotProduct(forward);
        var tip = radiusAt(volume, frame, laneExtension(volume, extension));

        if (along < volume.innerRadius() * frame.scale() - boxExtent(victim, forward)
                || along > tip + boxExtent(victim, forward)) {
            return false;
        }
        if (Math.abs(toVictim.dotProduct(plane)) > widthOf(volume, frame) * 0.5 + boxExtent(victim, plane)) {
            return false;
        }
        return Math.abs(toVictim.dotProduct(normal)) <= halfThickness(volume, frame)
                + boxExtent(victim, normal);
    }

    /** A travelling attack: sweeps the segment covered since last tick, so a fast dash cannot skip past. */
    private static boolean hitsPath(AttackVolume volume, AttackFrame frame, Box victim,
                                    @Nullable Vec3d previousOrigin) {
        var to = frame.origin();
        var from = previousOrigin != null ? previousOrigin : to;
        var padded = victim.expand(widthOf(volume, frame) * 0.5, halfThickness(volume, frame),
                widthOf(volume, frame) * 0.5);
        return padded.contains(to) || padded.contains(from) || padded.raycast(from, to).isPresent();
    }

    private static boolean hitsRadial(AttackVolume volume, AttackFrame frame, Box victim) {
        var closest = closestPointOnBox(victim, frame.origin());
        var reach = rangeOf(volume, frame);
        return closest.squaredDistanceTo(frame.origin()) <= reach * reach;
    }

    /** Half-extent of a box projected onto an axis — the right padding for a plane or radius test. */
    private static double boxExtent(Box box, Vec3d axis) {
        var hx = (box.maxX - box.minX) * 0.5;
        var hy = (box.maxY - box.minY) * 0.5;
        var hz = (box.maxZ - box.minZ) * 0.5;
        return Math.abs(axis.x) * hx + Math.abs(axis.y) * hy + Math.abs(axis.z) * hz;
    }

    private static Vec3d closestPointOnBox(Box box, Vec3d point) {
        return new Vec3d(
                MathHelper.clamp(point.x, box.minX, box.maxX),
                MathHelper.clamp(point.y, box.minY, box.maxY),
                MathHelper.clamp(point.z, box.minZ, box.maxZ));
    }

    // --- particle sampling -----------------------------------------------------------------------

    /** One sampled point on the attack surface, and the direction the blade is carrying it. */
    public record Sample(Vec3d position, Vec3d velocity, float bladeFraction) {
    }

    /**
     * Draw a random point on the slice of the attack surface swept between {@code s0} and {@code s1},
     * biased toward the tip where a real blade moves fastest.
     *
     * <p>Because {@code t} never leaves {@code [0, 1]} and {@code t = 1} is exactly the attack's real
     * reach, particles physically cannot appear beyond the damaging volume. The visible falloff comes from
     * the bias; the hard limit comes from the geometry.
     */
    public static Sample sample(AttackVolume volume, AttackFrame frame, float s0, float s1,
                                Random random, double speed, double scatter) {
        // sqrt biases toward 1, so the crescent reads as densest at the outer edge.
        var t = (float) Math.sqrt(random.nextDouble());
        var s = MathHelper.lerp(random.nextFloat(), s0, s1);

        var position = surfacePoint(volume, frame, s, t);
        var outward = outwardDirection(volume, frame, s);

        // Spread across the volume's thickness (and its width, for lanes) so the surface reads as solid.
        var normal = planeNormal(volume, frame);
        var plane = planeAxis(volume, frame);
        var thickness = halfThickness(volume, frame);
        position = position.add(normal.multiply((random.nextDouble() * 2.0 - 1.0) * thickness * 0.7));
        if (volume.shape().family() == AttackShape.Family.LANE
                || volume.shape().family() == AttackShape.Family.PATH) {
            position = position.add(plane.multiply(
                    (random.nextDouble() * 2.0 - 1.0) * widthOf(volume, frame) * 0.45));
        }
        if (scatter > 0.0) {
            position = position.add(
                    (random.nextDouble() * 2.0 - 1.0) * scatter,
                    (random.nextDouble() * 2.0 - 1.0) * scatter,
                    (random.nextDouble() * 2.0 - 1.0) * scatter);
        }
        return new Sample(position, outward.multiply(speed), t);
    }

    /** The direction "outward along the blade" at a point, so particles drift along the swing. */
    public static Vec3d outwardDirection(AttackVolume volume, AttackFrame frame, float s) {
        return switch (volume.shape().family()) {
            case ARC -> {
                var angle = Math.toRadians(sweepAngleDegrees(volume, frame, s));
                yield frame.forward().multiply(Math.cos(angle))
                        .add(planeAxis(volume, frame).multiply(Math.sin(angle)));
            }
            case RADIAL -> {
                var angle = Math.toRadians(s * 360.0);
                yield frame.forward().multiply(Math.cos(angle))
                        .add(planeAxis(volume, frame).multiply(Math.sin(angle)));
            }
            case LANE, PATH -> frame.forward();
        };
    }
}

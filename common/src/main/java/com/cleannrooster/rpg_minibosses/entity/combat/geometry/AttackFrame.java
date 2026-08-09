package com.cleannrooster.rpg_minibosses.entity.combat.geometry;

import net.minecraft.util.math.Vec3d;

/**
 * Everything about one particular swing that is not in the shared attack definition: where it started,
 * which way it was aimed, and at what scale.
 *
 * <p>Captured the moment an attack commits, and then frozen. That is the mechanism behind "you can dodge
 * this": the arc resolves against where the attacker was aiming when it committed, not against wherever it
 * has since turned to face.
 *
 * <p>The swing's <em>direction</em> is deliberately not in here — that belongs to the attack's
 * {@link SwingPath}, so the arc always travels the way its animation does. What the frame carries is only
 * where and how the attacker was standing.
 *
 * @param origin     world position the attack radiates from, already including the attack's offsets
 * @param yaw        facing yaw in degrees, including the swing's yaw offset
 * @param pitch      facing pitch in degrees, including the swing's pitch offset
 * @param rollOffset degrees added to the swing's plane roll, for per-swing variation
 * @param mirrored   play the swing from the opposite side
 * @param scale      attacker scale, so a resized body gets a resized reach
 */
public record AttackFrame(Vec3d origin, float yaw, float pitch, float rollOffset, boolean mirrored,
                          float scale) {
    private static final Vec3d WORLD_UP = new Vec3d(0.0, 1.0, 0.0);

    /** Facing vector. */
    public Vec3d forward() {
        return Vec3d.fromPolar(this.pitch, this.yaw);
    }

    /** The attacker's right-hand vector, perpendicular to {@link #forward()}. */
    public Vec3d right() {
        var candidate = forward().crossProduct(WORLD_UP);
        if (candidate.lengthSquared() < 1.0e-6) {
            // Aimed straight up or down: pick any stable perpendicular from the yaw alone.
            candidate = Vec3d.fromPolar(0.0f, this.yaw).crossProduct(WORLD_UP);
        }
        return candidate.normalize();
    }

    /** Completes the orthonormal basis. */
    public Vec3d up() {
        return right().crossProduct(forward()).normalize();
    }

    /**
     * The in-plane lateral axis. Rolling this about the facing vector is what turns one arc code path into
     * horizontal, diagonal and vertical swings.
     */
    public Vec3d planeAxis(float planeRollDegrees) {
        var roll = Math.toRadians(planeRollDegrees + this.rollOffset);
        return right().multiply(Math.cos(roll)).add(up().multiply(Math.sin(roll))).normalize();
    }

    /** Normal of the swing plane — the direction the damage volume has thickness in. */
    public Vec3d planeNormal(float planeRollDegrees) {
        var roll = Math.toRadians(planeRollDegrees + this.rollOffset);
        return right().multiply(-Math.sin(roll)).add(up().multiply(Math.cos(roll))).normalize();
    }

    /** The same frame relocated — used by travelling attacks, whose volume follows the attacker. */
    public AttackFrame withOrigin(Vec3d origin) {
        return new AttackFrame(origin, this.yaw, this.pitch, this.rollOffset, this.mirrored, this.scale);
    }
}

package com.cleannrooster.rpg_minibosses.entity.combat.geometry;

import net.minecraft.util.math.MathHelper;

/**
 * Explicit description of how a swing travels through space, so the damaging arc can be authored to match
 * the weapon animation instead of being inferred from entity facing.
 *
 * <p>What this replaces: a cone test centred on {@code getRotationVector()}, which had no direction of
 * travel at all. A right-to-left clip and a left-to-right clip produced identical damage, and neither
 * agreed with the blade. Here the attack states the swing outright — where it starts, where it finishes,
 * what plane it travels through — and {@link AttackGeometry} resolves both damage and the particle slash
 * against those same angles.
 *
 * <p><b>Angle convention.</b> Degrees within the swing plane, measured from the attacker's facing vector.
 * Positive is toward the attacker's right at {@code planeRoll = 0}, and toward up at {@code planeRoll = 90}.
 * So a right-to-left horizontal sweep is {@code start = +52, end = -52}; an overhead cut is roll 90 with
 * {@code start = +60, end = -60}.
 *
 * <p><b>Animation correspondence.</b> The v2 clips carry their swing in the {@code body} bone's yaw, and
 * AzureLib's Bedrock loading inverts that axis relative to world space — see the convention block in
 * {@code tools/gen_v2_animations.py}. {@code tools/verify_animations.py} checks the sign of a clip's body
 * yaw travel against the sign of its attack's swing, so a clip and its arc cannot silently disagree again.
 *
 * @param plane       how the swing reads on screen; also supplies a default roll
 * @param startAngle  in-plane angle the swing begins at, degrees
 * @param endAngle    in-plane angle the swing finishes at, degrees
 * @param planeRoll   rotation of the swing plane about the facing vector, degrees
 * @param yawOffset   local yaw applied to the whole attack before anything else, degrees
 * @param pitchOffset local pitch applied to the whole attack, degrees
 * @param handedness  which side of the attacker the weapon comes from
 * @param dynamics    how the swing's speed is distributed across its active window
 */
public record SwingPath(
        AttackPlane plane,
        float startAngle,
        float endAngle,
        float planeRoll,
        float yawOffset,
        float pitchOffset,
        Handedness handedness,
        SwingDynamics dynamics
) {
    /** Steady-speed swing. Attacks wanting a forceful blade opt in with {@link #withDynamics}. */
    public SwingPath(AttackPlane plane, float startAngle, float endAngle, float planeRoll,
                     float yawOffset, float pitchOffset, Handedness handedness) {
        this(plane, startAngle, endAngle, planeRoll, yawOffset, pitchOffset, handedness,
                SwingDynamics.STEADY);
    }

    /**
     * Angle at swing progress {@code s}. This one method is what makes the arc travel in the animated
     * direction: {@code s} always runs forward and the <em>angles</em> carry the direction.
     *
     * <p>{@code s} is reshaped by {@link SwingDynamics} first, so damage and the particle slash accelerate
     * together — the arc you see is still exactly the volume that can hit you, moving at exactly the speed
     * it appears to.
     */
    public float angleAt(float s) {
        return MathHelper.lerp(this.dynamics.apply(s), this.startAngle, this.endAngle);
    }

    /** Total angular span, always positive. */
    public float sweepDegrees() {
        return Math.abs(this.endAngle - this.startAngle);
    }

    /** True when the blade travels toward the attacker's right (or upward, in a vertical plane). */
    public boolean travelsPositive() {
        return this.endAngle >= this.startAngle;
    }

    /** The same motion from the other side, for authoring a mirrored variant as its own attack. */
    public SwingPath mirrored() {
        return new SwingPath(this.plane, -this.startAngle, -this.endAngle, -this.planeRoll,
                -this.yawOffset, this.pitchOffset, this.handedness.opposite(), this.dynamics);
    }

    public SwingPath withYawOffset(float degrees) {
        return new SwingPath(this.plane, this.startAngle, this.endAngle, this.planeRoll,
                degrees, this.pitchOffset, this.handedness, this.dynamics);
    }

    public SwingPath withPitchOffset(float degrees) {
        return new SwingPath(this.plane, this.startAngle, this.endAngle, this.planeRoll,
                this.yawOffset, degrees, this.handedness, this.dynamics);
    }

    public SwingPath withHandedness(Handedness handedness) {
        return new SwingPath(this.plane, this.startAngle, this.endAngle, this.planeRoll,
                this.yawOffset, this.pitchOffset, handedness, this.dynamics);
    }

    public SwingPath withDynamics(SwingDynamics dynamics) {
        return new SwingPath(this.plane, this.startAngle, this.endAngle, this.planeRoll,
                this.yawOffset, this.pitchOffset, this.handedness, dynamics);
    }

    // --- factories ---------------------------------------------------------------------------------

    /** Flat cut travelling right to left across the attacker's front. */
    public static SwingPath rightToLeft(float arcDegrees) {
        return new SwingPath(AttackPlane.HORIZONTAL, arcDegrees * 0.5f, -arcDegrees * 0.5f,
                AttackPlane.HORIZONTAL.defaultRoll(), 0.0f, 0.0f, Handedness.RIGHT);
    }

    /** Flat cut travelling left to right. */
    public static SwingPath leftToRight(float arcDegrees) {
        return new SwingPath(AttackPlane.HORIZONTAL, -arcDegrees * 0.5f, arcDegrees * 0.5f,
                AttackPlane.HORIZONTAL.defaultRoll(), 0.0f, 0.0f, Handedness.LEFT);
    }

    /** Overhead cut: starts raised and descends through the facing vector. */
    public static SwingPath overhead(float arcDegrees, AttackPlane plane) {
        return new SwingPath(plane, arcDegrees * 0.5f, -arcDegrees * 0.5f,
                plane.defaultRoll(), 0.0f, 0.0f, Handedness.RIGHT);
    }

    /** Rising cut: starts low and sweeps upward. */
    public static SwingPath rising(float arcDegrees, AttackPlane plane) {
        return new SwingPath(plane, -arcDegrees * 0.5f, arcDegrees * 0.5f,
                plane.defaultRoll(), 0.0f, 0.0f, Handedness.RIGHT);
    }

    /** A descending diagonal at an explicit plane roll — high on the right, finishing low on the left. */
    public static SwingPath diagonalDescending(float arcDegrees, float planeRoll) {
        return new SwingPath(AttackPlane.CUSTOM, arcDegrees * 0.5f, -arcDegrees * 0.5f,
                planeRoll, 0.0f, 0.0f, Handedness.RIGHT);
    }

    /** A symmetric span with no travel — for cones and impacts, which resolve everywhere at once. */
    public static SwingPath fixedSpan(float arcDegrees) {
        return new SwingPath(AttackPlane.HORIZONTAL, -arcDegrees * 0.5f, arcDegrees * 0.5f,
                0.0f, 0.0f, 0.0f, Handedness.CENTRED);
    }

    /**
     * For lanes and charge paths: no angular travel, but the plane roll still orients the volume, so a
     * roll near 90 turns a flat lane into a tall vertical blade.
     */
    public static SwingPath lane(float planeRoll) {
        return new SwingPath(AttackPlane.CUSTOM, 0.0f, 0.0f, planeRoll, 0.0f, 0.0f, Handedness.CENTRED);
    }
}

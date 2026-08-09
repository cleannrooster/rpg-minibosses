package com.cleannrooster.rpg_minibosses.entity.combat;

/**
 * The feel of one kind of movement, expressed as numbers rather than as code.
 *
 * <p>Acceleration and deceleration are deliberately separate values, and every profile here decelerates
 * faster than it accelerates. A single blend rate for both makes a heavy thing read as <em>floaty</em>,
 * because the lag that communicates mass on the way up communicates sliding on the way down. Splitting
 * them lets a mob keep a visible build-up into a burst while still stopping like something that simply
 * decided to stop.
 *
 * <p>Two further departures from a plain exponential blend, both in {@link GroundLocomotion#driveTo}:
 * <ul>
 *   <li>{@link #launchFloor()} — a proportional blend is slowest exactly when the gap is smallest, so the
 *       <em>start</em> of a burst is its weakest moment. The floor guarantees a minimum step per tick so a
 *       mob breaks away instead of oozing into motion.</li>
 *   <li>{@link #snap()} — an exponential approach never reaches its target, leaving a long tail of
 *       imperceptible drift after every stop. Under this threshold the velocity is set outright. The tail
 *       is deleted, not shortened.</li>
 * </ul>
 *
 * @param maxSpeed         cap on horizontal speed, blocks/tick (vanilla sprint is roughly 0.28)
 * @param accel            fraction of the remaining gap closed per tick while gaining speed, 0-1
 * @param arrest           fraction of the remaining gap closed per tick while losing speed, 0-1
 * @param launchFloor      minimum speed change per tick while gaining speed, blocks/tick
 * @param snap             gap below which velocity is set exactly instead of approached, blocks/tick
 * @param turnDegrees      cap on body-yaw change per tick while this profile is steering
 * @param lateralAuthority multiplier on the sideways component of a desired velocity, 0-1. Low values
 *                         make a mob poor at instantaneous redirection, which is how mass reads.
 */
public record MovementProfile(
        double maxSpeed,
        double accel,
        double arrest,
        double launchFloor,
        double snap,
        float turnDegrees,
        double lateralAuthority
) {

    /** Same profile at a different top speed — used for engagement scaling and phase changes. */
    public MovementProfile withMaxSpeed(double speed) {
        return new MovementProfile(speed, this.accel, this.arrest, this.launchFloor, this.snap,
                this.turnDegrees, this.lateralAuthority);
    }

    /** Same profile with everything time-related sharpened; for desperate/enraged phases. */
    public MovementProfile urgent(double speedScale, double accelScale, float turnScale) {
        return new MovementProfile(
                this.maxSpeed * speedScale,
                Math.min(1.0, this.accel * accelScale),
                Math.min(1.0, this.arrest * accelScale),
                this.launchFloor * accelScale,
                this.snap,
                this.turnDegrees * turnScale,
                this.lateralAuthority);
    }

    // --- archetype presets -----------------------------------------------------------------------
    //
    // Speeds are blocks/tick. For scale: a sprinting player is ~0.28, a walking one ~0.13.

    /**
     * Juggernaut. Slow to start, poor at redirecting, and stops like a dropped anvil. The low lateral
     * authority is the character: it can correct enough not to grind against a hitbox and no more.
     */
    public static final MovementProfile HEAVY_ADVANCE =
            new MovementProfile(0.24, 0.13, 0.55, 0.020, 0.030, 7.0f, 0.30);

    /** Juggernaut in Last Stand — the same mass, less hesitation about using it. */
    public static final MovementProfile HEAVY_DESPERATE =
            new MovementProfile(0.29, 0.20, 0.62, 0.030, 0.030, 9.0f, 0.38);

    /** Templar pressure. Faster than the Juggernaut and much more willing to keep walking forward. */
    public static final MovementProfile RELENTLESS =
            new MovementProfile(0.27, 0.22, 0.62, 0.030, 0.028, 11.0f, 0.55);

    /** Templar closing a gap the player opened. Speed is scaled further by distance at the call site. */
    public static final MovementProfile CONDEMN =
            new MovementProfile(0.36, 0.28, 0.66, 0.040, 0.028, 12.0f, 0.45);

    /** Trickster orbiting. High lateral authority — direction changes are its whole identity. */
    public static final MovementProfile AGILE_ORBIT =
            new MovementProfile(0.25, 0.34, 0.70, 0.040, 0.025, 16.0f, 1.00);

    /** Trickster committing to an ambush lane. */
    public static final MovementProfile AGILE_BURST =
            new MovementProfile(0.42, 0.45, 0.75, 0.060, 0.025, 13.0f, 0.85);

    /** Mercenary holding a firing band — small, deliberate corrections. */
    public static final MovementProfile DISCIPLINED =
            new MovementProfile(0.18, 0.26, 0.72, 0.030, 0.030, 14.0f, 0.80);

    /** Mercenary running to a firing position. Fast, straight, and it stops dead on arrival. */
    public static final MovementProfile TACTICAL_SPRINT =
            new MovementProfile(0.38, 0.30, 0.80, 0.050, 0.035, 10.0f, 0.40);

    /** Fire Mage maintaining spacing. Tangential by preference, never a flat-out sprint. */
    public static final MovementProfile ELASTIC =
            new MovementProfile(0.22, 0.30, 0.68, 0.035, 0.026, 15.0f, 0.95);

    /** Fire Mage's legs during a long cast — it may drift sideways, not run. */
    public static final MovementProfile CASTING_DRIFT =
            new MovementProfile(0.09, 0.22, 0.70, 0.020, 0.020, 9.0f, 1.00);

    /**
     * Used by every hard stop. Nothing accelerates under this profile, so only the arrest side of the
     * curve is ever consulted — but the numbers are stated anyway so the record stays readable.
     */
    public static final MovementProfile HARD_STOP =
            new MovementProfile(0.0, 0.5, 0.85, 0.050, 0.045, 10.0f, 0.0);
}

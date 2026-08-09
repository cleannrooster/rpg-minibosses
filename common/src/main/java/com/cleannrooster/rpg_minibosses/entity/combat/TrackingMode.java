package com.cleannrooster.rpg_minibosses.entity.combat;

/**
 * How much an attacker may re-aim at its target during a phase, as a cap on body-yaw change per tick.
 *
 * <p>The low settings exist for readability. A mob that keeps turning at full rate through its own active
 * frames is effectively homing, which makes every telegraph meaningless — so once an attack commits, the
 * definition, not the AI, decides how much it is still allowed to follow a sidestep.
 */
public enum TrackingMode {
    /** Ordinary pursuit turning. */
    FULL(20.0f),
    /** Slow correction: alive, but not enough to follow a strafe. */
    REDUCED(4.5f),
    /** Barely any correction — the last ticks of a heavy windup. */
    MINIMAL(1.2f),
    /** Facing frozen at whatever it was when this mode took effect. */
    LOCKED(0.0f);

    private final float degreesPerTick;

    TrackingMode(float degreesPerTick) {
        this.degreesPerTick = degreesPerTick;
    }

    public float degreesPerTick() {
        return this.degreesPerTick;
    }
}

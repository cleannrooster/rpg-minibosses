package com.cleannrooster.rpg_minibosses.entity.combat;

/**
 * How an attack relates to the attacker's body movement.
 *
 * <p>This is the classification the whole overhaul hangs on: an attack is not just damage on a timer, it
 * is a statement about where the body goes while the damage happens. Declaring it here means the runner
 * can take and release movement ownership correctly without every ability writing its own impulses.
 */
public enum AttackMotion {
    /** The body stops and stays stopped. Slam, Nova, Heavy Shot, Divine Fall. */
    PLANTED(true, true),
    /** The body keeps walking through the swing — the front line advances. Templar's basics. */
    PRESSING(true, false),
    /** The attack does not own movement at all; combat steering continues underneath it. */
    MOBILE(false, false),
    /** A heading is captured at commit and the body is thrown along it. Dash, Leap, Spin. */
    COMMITTED_VECTOR(true, true),
    /** A committed vector aimed <em>past</em> the target, so the attacker exits on the far side. */
    CROSS_THROUGH(true, true),
    /** A committed vector away from the target. Jump Back, roll-away. */
    RETREATING(true, true);

    private final boolean ownsMovement;
    private final boolean arrestsOnWindup;

    AttackMotion(boolean ownsMovement, boolean arrestsOnWindup) {
        this.ownsMovement = ownsMovement;
        this.arrestsOnWindup = arrestsOnWindup;
    }

    /** True when the runner should claim {@link MovementOwner#ATTACK_MOTION} for this attack. */
    public boolean ownsMovement() {
        return this.ownsMovement;
    }

    /**
     * True when the windup should bring the body to a genuine standstill first. The burst that follows
     * lands against stillness, and stillness is what makes it read as violent.
     */
    public boolean arrestsOnWindup() {
        return this.arrestsOnWindup;
    }

    /** True when the attack throws the body along a heading captured at commit time. */
    public boolean isCommitted() {
        return this == COMMITTED_VECTOR || this == CROSS_THROUGH || this == RETREATING;
    }
}

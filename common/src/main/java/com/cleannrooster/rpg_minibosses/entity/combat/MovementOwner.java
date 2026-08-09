package com.cleannrooster.rpg_minibosses.entity.combat;

/**
 * Who owns the mob's horizontal movement this tick.
 *
 * <p>Exactly one system may own it at a time. The whole point of naming the owner is that the three
 * systems can never argue: vanilla path navigation solves macro traversal, authored steering handles
 * the immediate combat envelope, and a committed attack overrides both while it runs.
 *
 * <p>{@link GroundLocomotion} is the arbiter — nothing else calls {@code setVelocity} on these mobs
 * during combat, and navigation is explicitly stopped whenever ownership leaves {@link #NAVIGATION}.
 */
public enum MovementOwner {
    /** Vanilla path navigation. Long approaches, obstacle solving, and the stall fallback. */
    NAVIGATION,
    /** Authored combat steering — pressure, orbits, retreats, repositions. */
    COMBAT_STEERING,
    /** A running attack is carrying the body. Steering requests are ignored until it releases. */
    ATTACK_MOTION
}

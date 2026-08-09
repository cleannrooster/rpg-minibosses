package com.cleannrooster.rpg_minibosses.entity.combat;

/**
 * Lifecycle of one {@link CombatAction} execution. Always runs in order and damage only ever resolves
 * during {@link #ACTIVE}, so "you can dodge this" is a property of the definition rather than of luck.
 */
public enum AttackPhase {
    /** Telegraph. No damage. Movement is usually being arrested or loaded here. */
    WINDUP,
    /** The committed window. Damage, projectiles and attack-owned translation happen here. */
    ACTIVE,
    /** The punish window. No damage, and movement authority is being handed back. */
    RECOVERY,
    /** Terminal. The runner drops the execution on the next tick. */
    FINISHED
}

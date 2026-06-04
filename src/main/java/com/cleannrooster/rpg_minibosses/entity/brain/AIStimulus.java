package com.cleannrooster.rpg_minibosses.entity.brain;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;

/**
 * Shared environmental context refreshed every server tick.
 * All mob brains read from one instance of this per brain.
 * Only refresh() on the server side.
 */
public final class AIStimulus {

    // ── Geometric ────────────────────────────────────────────────────────────
    public boolean targetInMeleeRange;
    public boolean targetInEngagementRange;
    public boolean targetFleeingDistance;
    public float   targetDistance;
    /** Dot product of owner's facing vector toward the target (1 = facing directly). */
    public float   targetFacingDot;
    public Vec3d   targetVelocity = Vec3d.ZERO;

    // ── Player reactive ───────────────────────────────────────────────────────
    public boolean playerIsBlocking;
    public boolean playerIsCasting;
    public boolean playerJustLanded;
    public boolean playerJustAttacked;
    /** Ticks since the target last attacked; resets to 0 on playerJustAttacked. */
    public int     timeSinceLastPlayerAttack  = 9999;
    /** Ticks since the target last used a defensive item; resets to 0 when blocking starts. */
    public int     timeSincePlayerUsedDefensive = 9999;
    public float   playerHealthPct;

    // ── Self ─────────────────────────────────────────────────────────────────
    public float   ownHealthPct;
    /** Ticks since onDamageTaken() was last called; increments each refresh. */
    public int     timeSinceLastHit = 9999;
    public float   lastDamageTaken;
    /** True if the last hit exceeded the per-mob damage threshold. */
    public boolean lastHitDamageExceeded;
    /** Ticks spent with the target inside engagement range (continuous). */
    public int     currentExposureDuration;

    // ── Context ───────────────────────────────────────────────────────────────
    public boolean lineOfSightToTarget;
    public int     nearbyAllyCount;
    public int     nearbyTrapCount;
    public boolean obstructionBetweenSelf;
    /** Set by RogueBrain each tick; null for all other mobs. */
    public Vec3d   lastAmbushVector;

    // ── Edge-detection snapshots ──────────────────────────────────────────────
    private boolean prevWasOnGround;
    private float   prevVelocityY;
    private float   prevAttackCooldown = 1.0f;
    private boolean prevIsBlocking;

    // ── Configuration ─────────────────────────────────────────────────────────
    private final MinibossEntity owner;
    private final float meleeThreshold;
    private final float engagementThreshold;
    private final float fleeThreshold;
    private final float damageExceededThreshold;

    public AIStimulus(MinibossEntity owner,
                      float meleeThreshold,
                      float engagementThreshold,
                      float fleeThreshold,
                      float damageExceededThreshold) {
        this.owner                  = owner;
        this.meleeThreshold         = meleeThreshold;
        this.engagementThreshold    = engagementThreshold;
        this.fleeThreshold          = fleeThreshold;
        this.damageExceededThreshold = damageExceededThreshold;
    }

    /** Call once per server tick from MobBrainGoal. Safe to call with a null target (no-ops). */
    public void refresh() {
        if (owner.getWorld().isClient()) return;
        LivingEntity target = owner.getTarget();
        if (target == null) return;

        // ── Geometric ────────────────────────────────────────────────────────
        targetDistance          = owner.distanceTo(target);
        targetInMeleeRange      = targetDistance <= meleeThreshold;
        targetInEngagementRange = targetDistance <= engagementThreshold;
        targetFleeingDistance   = targetDistance >= fleeThreshold;

        Vec3d toTarget   = target.getPos().subtract(owner.getPos()).normalize();
        targetFacingDot  = (float) owner.getRotationVector().dotProduct(toTarget);
        targetVelocity   = target.getVelocity();

        // ── Player — blocking ─────────────────────────────────────────────────
        boolean isBlocking = target.isBlocking();
        playerIsBlocking = isBlocking;
        if (!prevIsBlocking && isBlocking) {
            timeSincePlayerUsedDefensive = 0;
        }
        prevIsBlocking = isBlocking;

        // ── Player — casting (proxy: using item but not blocking) ─────────────
        playerIsCasting = target.isUsingItem() && !isBlocking;

        // ── Player — landing ──────────────────────────────────────────────────
        float currentVelocityY = (float) target.getVelocity().y;
        playerJustLanded = !prevWasOnGround && target.isOnGround() && prevVelocityY < -0.2f;
        prevWasOnGround  = target.isOnGround();
        prevVelocityY    = currentVelocityY;

        // ── Player — attack detection via attack cooldown ──────────────────────
        float currentAttackCooldown = 1.0f;
        if (target instanceof PlayerEntity player) {
            currentAttackCooldown = player.getAttackCooldownProgress(0);
            playerHealthPct       = player.getHealth() / player.getMaxHealth();
        } else {
            playerHealthPct = target.getHealth() / Math.max(1f, target.getMaxHealth());
        }
        playerJustAttacked = prevAttackCooldown > 0.7f && currentAttackCooldown < 0.2f;
        if (playerJustAttacked) {
            timeSinceLastPlayerAttack = 0;
        } else {
            timeSinceLastPlayerAttack++;
        }
        prevAttackCooldown = currentAttackCooldown;

        // ── Self ──────────────────────────────────────────────────────────────
        ownHealthPct    = owner.getHealth() / Math.max(1f, owner.getMaxHealth());
        timeSinceLastHit++;

        // ── Exposure duration ─────────────────────────────────────────────────
        if (targetInEngagementRange) {
            currentExposureDuration++;
        } else {
            currentExposureDuration = 0;
        }

        // ── Defensive cooldown tick ───────────────────────────────────────────
        if (!playerIsBlocking) {
            timeSincePlayerUsedDefensive++;
        }

        // ── Context ───────────────────────────────────────────────────────────
        lineOfSightToTarget = owner.canSee(target);

        // nearbyAllyCount and nearbyTrapCount are left for subclass override if needed.
        // They are reset here so brains that don't use them stay at 0.
        nearbyAllyCount = 0;
        nearbyTrapCount = 0;
    }

    /**
     * Call from MinibossEntity.damage() BEFORE super.damage() to record incoming hits.
     * Updates timeSinceLastHit and lastHitDamageExceeded.
     */
    public void notifyDamage(float amount) {
        this.lastDamageTaken      = amount;
        this.lastHitDamageExceeded = amount >= damageExceededThreshold;
        this.timeSinceLastHit     = 0;
    }
}

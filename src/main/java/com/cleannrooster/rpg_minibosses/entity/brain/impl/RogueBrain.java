package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.TricksterEntity;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.SoundHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;

import java.util.List;

/**
 * AI brain for the Rogue (TricksterEntity).
 *
 * Personality: Inattention and positional laziness gets punished.
 * Punishes: predictable patterns, lack of awareness, staying still.
 *
 * Stances:      SHADOW (invisible, orbiting), ASSAULT (exposed, striking), RECOIL (fleeing)
 * CombatStates: STALKING (orbital stealth), AMBUSHING (burst gap-close), WITHDRAWING (disengage)
 *
 * Approach windows open when the player is predictable (not attacking, just landed, low health).
 * Exposure timer forces WITHDRAWING after sustained contact — the rogue never commits to a long fight.
 * Dodge-on-hit remains in TricksterEntity.damage() via tryDodge() — it is purely reactive.
 */
public class RogueBrain extends MobBrain {

    public enum Stance implements BrainState {
        SHADOW, ASSAULT, RECOIL;
        @Override public String id() { return name(); }
    }

    public enum CombatState implements BrainState {
        STALKING, AMBUSHING, WITHDRAWING;
        @Override public String id() { return name(); }
    }

    private static final float MELEE_THRESHOLD       = 3.0f;
    private static final float ENGAGEMENT_THRESHOLD  = 16.0f;
    private static final float FLEE_THRESHOLD        = 20.0f;
    private static final float DAMAGE_EXCEEDED_THRESHOLD = 4.0f;
    private static final float RECOIL_HEALTH_PCT     = 0.35f;

    private static final int ROLL_CD       = 80;
    private static final int THROW_CD      = 80;
    private static final int DODGE_CD      = 80;
    private static final int AMBUSH_CD     = 200;
    private static final int AMBUSH_RESET_BUFFER = 40;
    /** 12-second cooldown on applying invisibility — entering STALKING without it is still valid. */
    private static final int STEALTH_CD    = 240;

    /** Basic melee attack cadence — fast dagger/sickle strikes. */
    private static final int   MELEE_ATTACK_CD      = 12;
    /** Arc parameters — narrow quick slash for a small agile attacker. */
    private static final float MELEE_ARC_HALF_ANGLE = 50f;
    private static final float MELEE_ARC_RANGE      = 2.5f;

    /** Radius the Rogue tries to orbit at while stalking. */
    private static final float ORBIT_RADIUS = 8.0f;
    /** Ticks of continuous open exposure before forcing WITHDRAWING. */
    private static final int MAX_EXPOSURE_TICKS = 100;

    private int exposureTicks = 0;

    private final TricksterEntity rogue;

    public RogueBrain(TricksterEntity entity) {
        super(entity, Stance.SHADOW, CombatState.STALKING);
        this.rogue = entity;
    }

    @Override
    protected AIStimulus createStimulus() {
        return new AIStimulus(entity, MELEE_THRESHOLD, ENGAGEMENT_THRESHOLD, FLEE_THRESHOLD, DAMAGE_EXCEEDED_THRESHOLD);
    }

    @Override
    protected List<Transition> stanceTransitions() {
        return List.of(
            // SHADOW → ASSAULT: approach window opens
            new Transition(Stance.SHADOW,   this::isApproachWindowOpen,             Stance.ASSAULT, 5),
            // ASSAULT → RECOIL: health too low to continue
            new Transition(Stance.ASSAULT,  s -> s.ownHealthPct < RECOIL_HEALTH_PCT, Stance.RECOIL,  10),
            // RECOIL → SHADOW: safe again, ready to stalk
            new Transition(Stance.RECOIL,   s -> s.timeSinceLastHit > 100,           Stance.SHADOW,  5)
        );
    }

    @Override
    protected List<Transition> combatStateTransitions() {
        return List.of(
            // STALKING → AMBUSHING: window open and ambush sequence ready
            new Transition(CombatState.STALKING,
                s -> isApproachWindowOpen(s) && cooldowns.isReady("ambush"),
                CombatState.AMBUSHING, 10),

            // AMBUSHING → WITHDRAWING: over-exposed or health dropped
            new Transition(CombatState.AMBUSHING,
                s -> exposureTicks > MAX_EXPOSURE_TICKS || s.ownHealthPct < RECOIL_HEALTH_PCT,
                CombatState.WITHDRAWING, 10),

            // AMBUSHING → STALKING: ambush sequence fired, give time before re-evaluating
            new Transition(CombatState.AMBUSHING,
                s -> !cooldowns.isReady("ambush") && exposureTicks > 40,
                CombatState.STALKING, 5),

            // WITHDRAWING → STALKING: broken line of sight and recovered
            new Transition(CombatState.WITHDRAWING,
                s -> (!s.lineOfSightToTarget || (s.timeSinceLastHit > 160 )) && cooldowns.isReady("ambush_reset"),
                CombatState.STALKING, 10)
        );
    }

    @Override
    protected List<PhaseTransition> definePhaseTransitions() {
        return List.of();
    }

    @Override
    protected void onCombatStateEnter(BrainState state) {
        if (state == CombatState.STALKING) {
            exposureTicks = 0;
            // Stealth is gated — if the cooldown hasn't expired the rogue stalks visibly
            if (cooldowns.isReady("stealth")) {
                cooldowns.trigger("stealth", STEALTH_CD);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 320, 0));
            }
        } else if (state == CombatState.AMBUSHING) {
            entity.removeStatusEffect(StatusEffects.INVISIBILITY);
            cooldowns.trigger("ambush", AMBUSH_CD);
            exposureTicks = 0;
        } else if (state == CombatState.WITHDRAWING) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1));
            cooldowns.trigger("ambush_reset", AMBUSH_RESET_BUFFER);
            // Immediately roll away from the enemy if the roll is available
            if (cooldowns.isReady("roll") && entity.getTarget() != null) {
                performRollAway();
            }
        }
    }

    @Override
    protected void executeCurrentCombatState() {
        if (entity.getTarget() == null) return;

        if (currentCombatState == CombatState.STALKING) {
            tickStalking();
        } else if (currentCombatState == CombatState.AMBUSHING) {
            tickAmbushing();
        } else {
            tickWithdrawing();
        }
    }

    // ── STALKING ──────────────────────────────────────────────────────────────

    private void tickStalking() {
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);

        float dist = stimulus.targetDistance;

        if (dist > ORBIT_RADIUS + 2) {
            // Approach to orbit radius
            entity.getNavigation().startMovingTo(
                entity.getTarget().getX(), entity.getTarget().getY(), entity.getTarget().getZ(), 1.0);
        } else if (dist < ORBIT_RADIUS - 2) {
            // Back off to maintain orbit distance
            Vec3d away = entity.getPos().subtract(entity.getTarget().getPos()).normalize();
            entity.getNavigation().startMovingTo(
                entity.getX() + away.x * 3, entity.getY(), entity.getZ() + away.z * 3, 1.0);
        } else {
            // In orbit band — strafe around target
            (entity.getMoveControl()).strafeTo(
                0F,
                stimulus.targetFacingDot > 0 ? -0.8F : 0.8F);
        }

        // Knife throw from stealth when target is in sight
        if (!entity.performing && cooldowns.isReady("throw")
                && stimulus.lineOfSightToTarget && dist > 4 && dist < 20) {
            performKnifeThrow();
        }
    }

    // ── AMBUSHING ─────────────────────────────────────────────────────────────

    private void tickAmbushing() {
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);
        exposureTicks++;

        float dist = stimulus.targetDistance;
        if (dist > MELEE_THRESHOLD) {
            // Gap-close at double speed
            entity.getNavigation().startMovingTo(
                entity.getTarget().getX(), entity.getTarget().getY(), entity.getTarget().getZ(), 1.6);
        } else {
            entity.getNavigation().stop();
            // In melee — maintain pressure with strafing
            (entity.getMoveControl()).strafeTo(
                -1.0F,
                stimulus.targetFacingDot > 0 ? -0.6F : 0.6F);
        }

        // Melee attack when in close range
        if (!entity.performing && cooldowns.isReady("melee") && dist <= MELEE_THRESHOLD) {
            performMeleeAttack();
        }

        // Roll to maintain unpredictability
        if (!entity.performing && cooldowns.isReady("roll") && entity.isAttacking()) {
            performRoll();
        }
    }

    private void performMeleeAttack() {
        if (entity.getTarget() == null) return;
        cooldowns.trigger("melee", MELEE_ATTACK_CD);
        entity.tryAttack(entity.getTarget());
        performArcDamage(entity.getTarget(), MELEE_ARC_HALF_ANGLE, MELEE_ARC_RANGE);
    }

    // ── WITHDRAWING ───────────────────────────────────────────────────────────

    private void tickWithdrawing() {
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);

        // Move away — TricksterNavigation's RogueNodeMaker biases toward open/occluded space
        Vec3d away = entity.getPos().subtract(entity.getTarget().getPos()).normalize();
        entity.getNavigation().startMovingTo(
            entity.getX() + away.x * 12,
            entity.getY(),
            entity.getZ() + away.z * 12,
            1.2);
    }

    // ── Abilities ─────────────────────────────────────────────────────────────

    private void performKnifeThrow() {
        int cd = Math.max(40, THROW_CD - (int)(THROW_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("throw", cd);
        entity.performing = true;
        entity.resetIndicator();
        entity.dispatcher.setPrepare();

        ((WorldScheduler) entity.getWorld()).schedule(20, () -> {
            if (entity.getTarget() == null) { entity.performing = false; return; }
            entity.dispatcher.throw1();
            entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entity.getTarget().getEyePos());
            SoundHelper.playSound(entity.getWorld(), entity,
                new Sound(Identifier.of("minecraft:entity.player.attack.sweep")));
            SpellHelper.shootProjectile(entity.getWorld(), entity, entity.getTarget(),
                SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "knifethrow")).get(),
                new SpellHelper.ImpactContext()
                    .power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, entity))
                    .position(entity.getPos()));
            ParticleHelper.sendBatches(entity,
                SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "knifethrow")).release.particles);

            ((WorldScheduler) entity.getWorld()).schedule(10, () -> {
                if (entity.getTarget() == null) { entity.performing = false; return; }
                entity.dispatcher.throw2();
                entity.performing = false;
                entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, entity.getTarget().getEyePos());
                SoundHelper.playSound(entity.getWorld(), entity,
                    new Sound(Identifier.of("minecraft:entity.player.attack.sweep")));
                SpellHelper.shootProjectile(entity.getWorld(), entity, entity.getTarget(),
                    SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "knifethrow")).get(),
                    new SpellHelper.ImpactContext()
                        .power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, entity))
                        .position(entity.getPos()));
                ParticleHelper.sendBatches(entity,
                    SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "knifethrow")).release.particles);
            });
        });
    }

    private void performRoll() {
        int cd = Math.max(40, ROLL_CD - (int)(ROLL_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("roll", cd);
        entity.dispatcher.roll();
        entity.addVelocity(entity.getRotationVector().multiply(2F));
    }

    private void performRollAway() {
        int cd = Math.max(40, ROLL_CD - (int)(ROLL_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("roll", cd);
        entity.dispatcher.roll();
        Vec3d away = entity.getPos().subtract(entity.getTarget().getPos()).normalize();
        entity.setVelocity(away.x * 2, entity.getVelocity().y, away.z * 2);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isApproachWindowOpen(AIStimulus s) {
        return s.timeSinceLastPlayerAttack > 16
            || (s.playerJustLanded && s.targetDistance < 16)
            || s.playerHealthPct < 0.4f;
    }

    /**
     * Called from TricksterEntity.damage() to check/trigger the dodge cooldown.
     * Returns true if dodge should fire this hit.
     */
    public boolean tryDodge() {
        if (cooldowns.isReady("dodge")) {
            int cd = Math.max(40, DODGE_CD - (int)(DODGE_CD * entity.getCooldownCoeff()));
            cooldowns.trigger("dodge", cd);
            return true;
        }
        return false;
    }

    @Override
    public void onTargetLost() {
        entity.removeStatusEffect(StatusEffects.INVISIBILITY);

        super.onTargetLost();
    }
}

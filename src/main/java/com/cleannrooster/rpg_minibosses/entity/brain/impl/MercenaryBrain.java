package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.entity.TrapCleann;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import net.minecraft.entity.ai.NoPenaltyTargeting;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.internals.SpellHelper;
import net.spell_power.api.SpellPower;

import java.util.List;

/**
 * AI brain for the Mercenary (ArtilleristEntity).
 *
 * Personality: Harassment + opportunism + punishment of mistakes.
 * Punishes: recklessness and readable patterns.
 *
 * Stances:   PROBING (default), OPPORTUNIST (player defensive on cooldown)
 * States:    PROBING (mid-range holding + trap placement), PUNISHING (burst window)
 *
 * The ArtilleristCrossbowAttackGoal handles shot timing independently.
 * This brain manages movement positioning, trap deployment, and when to escalate.
 */
public class MercenaryBrain extends MobBrain {

    public enum Stance implements BrainState {
        PROBING, OPPORTUNIST;
        @Override public String id() { return name(); }
    }

    public enum CombatState implements BrainState {
        PROBING, PUNISHING;
        @Override public String id() { return name(); }
    }

    private static final float MELEE_THRESHOLD      = 4.0f;
    private static final float ENGAGEMENT_THRESHOLD  = 16.0f;
    private static final float FLEE_THRESHOLD        = 20.0f;

    /** Mid-range band the Mercenary tries to hold (8–14 blocks from target). */
    private static final float OPTIMAL_NEAR = 8.0f;
    private static final float OPTIMAL_FAR  = 12.0f;

    private static final int PUNISH_BURST_DURATION = 40;
    private static final int TRAP_PLACE_CD         = 160;
    private static final int REPOSITION_CD         = 180;

    private final ArtilleristEntity mercenary;
    /** Nearby trap count updated each tick in executeCurrentCombatState. */
    private int nearbyTrapCount = 0;

    public MercenaryBrain(ArtilleristEntity entity) {
        super(entity, Stance.PROBING, CombatState.PROBING);
        this.mercenary = entity;
    }

    @Override
    protected AIStimulus createStimulus() {
        return new AIStimulus(entity, MELEE_THRESHOLD, ENGAGEMENT_THRESHOLD, FLEE_THRESHOLD, 10.0f);
    }

    @Override
    protected List<Transition> stanceTransitions() {
        return List.of(
            // PROBING → OPPORTUNIST: player used defensive item recently (window open)
            new Transition(Stance.PROBING,     s -> s.timeSincePlayerUsedDefensive < 100, Stance.OPPORTUNIST, 1),
            // OPPORTUNIST → PROBING: enough time has passed since the defensive
            new Transition(Stance.OPPORTUNIST, s -> s.timeSincePlayerUsedDefensive > 200, Stance.PROBING,     1)
        );
    }

    @Override
    protected List<Transition> combatStateTransitions() {
        return List.of(
            // PROBING → PUNISHING: punishable window detected
            new Transition(CombatState.PROBING, this::isPunishableWindow, CombatState.PUNISHING, 10),
            // PUNISHING → PROBING: burst window expired
            new Transition(CombatState.PUNISHING, s -> cooldowns.isReady("punish_burst"), CombatState.PROBING, 10)
        );
    }

    @Override
    protected List<PhaseTransition> definePhaseTransitions() {
        return List.of();
    }

    @Override
    protected void onCombatStateEnter(BrainState state) {
        if (state == CombatState.PUNISHING) {
            cooldowns.trigger("punish_burst", PUNISH_BURST_DURATION);
            mercenary.startRunning = false;
            mercenary.getDataTracker().set(ArtilleristEntity.RUNNING, false);
        }
        if (state == CombatState.PROBING) {
            // Nothing special on re-entry to probing
        }
    }

    @Override
    protected void executeCurrentCombatState() {
        if (entity.getTarget() == null) return;

        // Update local trap count before state evaluation uses it
        nearbyTrapCount = entity.getWorld()
            .getEntitiesByClass(TrapCleann.class,
                entity.getBoundingBox().expand(OPTIMAL_FAR + 4),
                e -> e.isAlive())
            .size();

        if (currentCombatState == CombatState.PROBING) {
            tickProbing();
        } else {
            tickPunishing();
        }
    }

    // ── PROBING ───────────────────────────────────────────────────────────────

    private void tickProbing() {
        if (mercenary.startRunning) return; // let the running behavior run its course

        entity.getLookControl().lookAt(entity.getTarget(), 360, 390);

        float dist = stimulus.targetDistance;


        // Maintain mid-range band
        if (dist < OPTIMAL_NEAR) {
            // Too close — move away
            if (cooldowns.isReady("reposition")) {
                triggerReposition();
            }
        } else if (dist > OPTIMAL_FAR) {
            // Too far — close slightly
            entity.getNavigation().startMovingTo(
                entity.getTarget().getX(),
                entity.getTarget().getY(),
                entity.getTarget().getZ(), 1.2);
        } else {
            // In optimal band — hold position
            entity.getNavigation().stop();
        }

        // Periodic trap placement
        if (cooldowns.isReady("trap_place") && entity.canSee(entity.getTarget())) {
            placePredictiveTraps();
        }
    }

    private void triggerReposition() {
        cooldowns.trigger("reposition", REPOSITION_CD - (int)(REPOSITION_CD * entity.getCooldownCoeff()));
        mercenary.startRunning = true;
        mercenary.getDataTracker().set(ArtilleristEntity.RUNNING, true);
        mercenary.runningTick = 80;
        mercenary.resetIndicator();
    }

    private void placePredictiveTraps() {
        int cd = Math.max(80, TRAP_PLACE_CD - (int)(TRAP_PLACE_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("trap_place", cd);
        mercenary.trapCooldown = cd;

        Vec3d targetVelocity = stimulus.targetVelocity;
        Vec3d predictedPos = entity.getTarget().getPos().add(targetVelocity.multiply(20));

        for (int i = 0; i < 3; i++) {
            TrapCleann trap = new TrapCleann(
                RPGMinibossesEntities.TRAP, entity, entity.getWorld(),
                Identifier.of(RPGMinibosses.MOD_ID, "explosion"),
                new SpellHelper.ImpactContext().power(
                    SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_RANGED, entity)));
            trap.setPosition(entity.getEyePos());
            double angle = Math.toRadians(i * 60 - 60);
            Vec3d dir = new Vec3d(Math.cos(angle), -0.1, Math.sin(angle)).normalize().multiply(0.15);
            trap.setVelocity(dir);
            trap.setYaw(entity.getYaw());
            trap.prevYaw = entity.getYaw();
            entity.getWorld().spawnEntity(trap);
            entity.getWorld().playSound((PlayerEntity) null,
                trap.getX(), trap.getY(), trap.getZ(),
                SoundEvents.ENTITY_ARMOR_STAND_PLACE, SoundCategory.BLOCKS, 0.75F, 0.8F);
        }
    }

    // ── PUNISHING ─────────────────────────────────────────────────────────────

    private void tickPunishing() {
        if(mercenary.startRunning) return;
        entity.getLookControl().lookAt(entity.getTarget(), 360, 390);

        // Hold optimal range — crossbow goal handles the shooting
        float dist = stimulus.targetDistance;
        if (dist > OPTIMAL_FAR) {
            entity.getNavigation().startMovingTo(
                entity.getTarget().getX(),
                entity.getTarget().getY(),
                entity.getTarget().getZ(), 1.6);
        } else if (dist < OPTIMAL_NEAR) {
            // Back off slightly
            Vec3d away = entity.getPos().subtract(entity.getTarget().getPos()).normalize();
            entity.getNavigation().startMovingTo(
                entity.getX() + away.x * 3,
                entity.getY(),
                entity.getZ() + away.z * 3, 1.2);
        } else {
            entity.getNavigation().stop();
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isPunishableWindow(AIStimulus s) {
        return !mercenary.startRunning &&(s.timeSinceLastPlayerAttack > 15
            || s.playerJustLanded
            || s.playerIsCasting
            || s.playerHealthPct < 0.3f
            || (nearbyTrapCount > 0 && s.targetDistance < 5));
    }
}

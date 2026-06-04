package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.minecraft.server.world.ServerWorld;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineParticles;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;

import java.util.List;

/**
 * AI brain for the Juggernaut (JuggernautEntity).
 *
 * Personality: Brute force + defend when necessary + Last Stand.
 * Punishes: panic and retreat instinct.
 *
 * Stances:   ADVANCING (default), LAST_STAND (≤20% HP — phase transition)
 * States:    ADVANCING (always move), BRACING (reactive block, 50-tick limit)
 *
 * Last Stand locks out BRACING entirely — the Juggernaut stops defending
 * and becomes a pure damage race.
 */
public class JuggernautBrain extends MobBrain {

    public enum Stance implements BrainState {
        ADVANCING, LAST_STAND;
        @Override public String id() { return name(); }
    }


    public enum CombatState implements BrainState {
        ADVANCING, BRACING;
        @Override public String id() { return name(); }
    }

    private static final float MELEE_THRESHOLD      = 3.0f;
    private static final float ENGAGEMENT_THRESHOLD  = 16.0f;
    private static final float FLEE_THRESHOLD        = 20.0f;
    private static final float DAMAGE_EXCEEDED_THRESHOLD = 8.0f;

    private static final int SLAM_CD  = 140;
    private static final int SPIN_CD  = 460;
    private static final int LEAP_CD  = 160;
    private static final int BRACE_DURATION = 50;

    /** Basic melee attack cadence — heavy hammer, medium swing rate. */
    private static final int   MELEE_ATTACK_CD      = 20;
    /** Arc parameters — medium sweep for a large hammer wielder. */
    private static final float MELEE_ARC_HALF_ANGLE = 80f;
    private static final float MELEE_ARC_RANGE      = 3.5f;
    /** Spin reaches slightly further than normal melee; 180° covers the full rotation. */
    private static final float SPIN_ARC_RANGE       = 5.0f;

    // Tracks previous displacement to detect knockback
    private float prevVelocityY = 0f;

    private final JuggernautEntity juggernaut;

    public JuggernautBrain(JuggernautEntity entity) {
        super(entity, Stance.ADVANCING, CombatState.ADVANCING);
        this.juggernaut = entity;
    }

    @Override
    protected AIStimulus createStimulus() {
        return new AIStimulus(entity, MELEE_THRESHOLD, ENGAGEMENT_THRESHOLD, FLEE_THRESHOLD, DAMAGE_EXCEEDED_THRESHOLD);
    }

    @Override
    protected List<Transition> stanceTransitions() {
        // Stance transitions happen via phase transition at 20% HP; no normal transitions needed.
        return List.of();
    }

    @Override
    protected List<Transition> combatStateTransitions() {
        return List.of(
            // ADVANCING → BRACING: big hit received OR health dropped low (not in Last Stand)
            new Transition(CombatState.ADVANCING, s ->
                (s.lastHitDamageExceeded || s.ownHealthPct < 0.4f)
                && currentStance != Stance.LAST_STAND,
                CombatState.BRACING, 10),

            // BRACING → ADVANCING: brace cooldown expired (fixed duration)
            new Transition(CombatState.BRACING, s -> cooldowns.isReady("brace"),
                CombatState.ADVANCING, 10)
        );
    }

    @Override
    protected List<PhaseTransition> definePhaseTransitions() {
        return List.of(
            new PhaseTransition(0.20f, Stance.LAST_STAND,
                List.of(CombatState.BRACING), // BRACING locked out — pure aggression from here
                List.of(),
                () -> {
                    // Reset every offensive cooldown so Last Stand opens at full aggression
                    cooldowns.clear("slam");
                    cooldowns.clear("spin");
                    cooldowns.clear("leap");
                    cooldowns.clear("melee");
                })
        );
    }

    @Override
    protected void executeCurrentCombatState() {
        if (entity.getTarget() == null) return;

        if (currentCombatState == CombatState.ADVANCING) {
            tickAdvancing();
        } else {
            tickBracing();
        }

        prevVelocityY = (float) entity.getVelocity().y;
    }

    @Override
    protected void onCombatStateEnter(BrainState state) {
        if (state == CombatState.BRACING) {
            // Stop movement, start brace duration timer
            entity.getNavigation().stop();
            cooldowns.trigger("brace", BRACE_DURATION);
        } else if (state == CombatState.ADVANCING) {
            // Brief speed burst after brace ends (to close gap)
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 30, 0, false, false));
        }
    }

    // ── ADVANCING ─────────────────────────────────────────────────────────────

    private void tickAdvancing() {
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);

        // Always advance — no pauses, no idle beats
        if (entity.getTarget().distanceTo(entity) > MELEE_THRESHOLD) {
            entity.getNavigation().startMovingTo(
                entity.getTarget().getX(),
                entity.getTarget().getY(),
                entity.getTarget().getZ(), 1.0);
        } else {
            // In melee: strafe to maintain pressure
            (entity.getMoveControl())
                .strafeTo(-1.0F,
                    entity.getTarget().getPos().subtract(entity.getPos())
                        .crossProduct(new Vec3d(0, 1, 0))
                        .dotProduct(entity.getRotationVector()) > 0 ? -0.6F : 0.6F);
        }

        if (entity.performing) return;

        float dist = entity.distanceTo(entity.getTarget());

        // Ability priority: leap (far) → slam (close) → spin (wide arc) → basic melee
        if (dist >= 6 && cooldowns.isReady("leap") && entity.canSee(entity.getTarget())) {
            performLeap();
        } else if (dist <= MELEE_THRESHOLD && cooldowns.isReady("slam") && entity.isAttacking()) {
            performSlam();
        } else if (dist <= 10 && cooldowns.isReady("spin") && entity.canSee(entity.getTarget()) && entity.isAttacking()) {
            performSpin();
        } else if (dist <= MELEE_THRESHOLD && cooldowns.isReady("melee")) {
            performMeleeAttack();
        }
    }

    private void performMeleeAttack() {
        if (entity.getTarget() == null) return;
        cooldowns.trigger("melee", MELEE_ATTACK_CD);
        entity.tryAttack(entity.getTarget());
        performArcDamage(entity.getTarget(), MELEE_ARC_HALF_ANGLE, MELEE_ARC_RANGE);
    }

    // ── BRACING ───────────────────────────────────────────────────────────────

    private void tickBracing() {
        // Stand planted — movement stopped in onCombatStateEnter
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);

        // Emit shield particles every 4 ticks while braced
        if (entity.age % 4 == 0 && entity.getWorld() instanceof ServerWorld sw) {
            sw.spawnParticles(
                SpellEngineParticles.MagicParticles.get(
                    SpellEngineParticles.MagicParticles.Shape.SPARK,
                    SpellEngineParticles.MagicParticles.Motion.BURST).particleType(),
                entity.getX(), entity.getY() + entity.getHeight() * 0.5, entity.getZ(),
                6, 0.5, 0.6, 0.5, 0.02);
        }
    }

    // ── Abilities ─────────────────────────────────────────────────────────────

    private void performSlam() {
        int cd = Math.max(70, SLAM_CD - (int)(SLAM_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("slam", cd);
        entity.performing = true;
        entity.resetIndicator();

        ((WorldScheduler) entity.getWorld()).schedule(20, () -> {
            entity.dispatcher.setSlam();
            ((WorldScheduler) entity.getWorld()).schedule(20, () -> {
                ParticleHelper.sendBatches(entity,
                    SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);
                for (Entity target : TargetHelper.targetsFromArea(entity, 6, new Spell.Target.Area(), null)) {
                    SpellHelper.performImpacts(entity.getWorld(), entity, target, entity,
                        SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "pound")).get(),
                        SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).impacts,
                        new SpellHelper.ImpactContext()
                            .power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, entity))
                            .position(entity.getPos()));
                }
                entity.playSound(net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE.value());
                entity.performing = false;
            });
        });
    }

    private void performSpin() {
        int cd = Math.max(230, SPIN_CD - (int)(SPIN_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("spin", cd);
        entity.performing = true;
        entity.resetIndicator();

        // Immediate hit on anything in contact range the moment the spin starts
        spinPulse();

        entity.dispatcher.setWave();
        ((WorldScheduler) entity.getWorld()).schedule(60, () -> {
            spinPulse();
        });
        for (int i = 3; i <= 8; i++) {
            final int tick = 20 * i;
            ((WorldScheduler) entity.getWorld()).schedule(tick, this::spinPulse);
        }
        ((WorldScheduler) entity.getWorld()).schedule(40, () -> {
            spinPulse();
            entity.dispatcher.setSpin();
            entity.addVelocity(entity.getRotationVector().subtract(0, entity.getRotationVector().getY(), 0).multiply(2));
        });
        for (int i = 2; i <= 4; i++) {
            final int tick = 40 * i;
            ((WorldScheduler) entity.getWorld()).schedule(tick, () -> {
                spinPulse();
                entity.dispatcher.setSpin();
                entity.addVelocity(entity.getRotationVector().subtract(0, entity.getRotationVector().getY(), 0).multiply(2));
            });
        }
        ((WorldScheduler) entity.getWorld()).schedule(40 * 2 + 4 * 40, () -> {
            ((WorldScheduler) entity.getWorld()).schedule(20, () -> entity.performing = false);
        });
    }

    private void spinPulse() {
        ParticleHelper.sendBatches(entity,
            SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);
        // Full 360° melee hit — null primary so all nearby entities are included
        performArcDamage(null, 180f, SPIN_ARC_RANGE);
    }

    private void performLeap() {
        int cd = Math.max(80, LEAP_CD - (int)(LEAP_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("leap", cd);
        entity.performing = true;
        entity.resetIndicator();

        ((WorldScheduler) entity.getWorld()).schedule(20, () -> {
            entity.dispatcher.setLeap();
            ((WorldScheduler) entity.getWorld()).schedule(10, () ->
                entity.addVelocity(
                    entity.getRotationVector().subtract(0, entity.getRotationVector().getY(), 0)
                        .multiply(2).add(0, 0.5, 0)));

            ((WorldScheduler) entity.getWorld()).schedule(28, () -> {
                ParticleHelper.sendBatches(entity,
                    SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).release.particles);
                for (Entity target : TargetHelper.targetsFromArea(entity, 6, new Spell.Target.Area(), null)) {
                    SpellHelper.performImpacts(entity.getWorld(), entity, target, entity,
                        SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.MOD_ID, "pound")).get(),
                        SpellRegistry.from(entity.getWorld()).get(Identifier.of(RPGMinibosses.MOD_ID, "pound")).impacts,
                        new SpellHelper.ImpactContext()
                            .power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, entity))
                            .position(entity.getPos()));
                }
                entity.playSound(net.minecraft.sound.SoundEvents.ENTITY_GENERIC_EXPLODE.value());
                entity.performing = false;
            });
        });
    }
}

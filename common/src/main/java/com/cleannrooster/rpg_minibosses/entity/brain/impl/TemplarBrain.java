package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.TemplarEntity;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_engine.utils.WorldScheduler;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;
import net.minecraft.sound.SoundEvents;

import java.util.List;

/**
 * AI brain for the Templar (TemplarEntity).
 *
 * Personality: Overwhelming pressure + punish cowardice + collapse engagements.
 * Punishes: range-seeking and disengagement.
 *
 * Stances/States are unified — the Templar has only two modes:
 *   CRUSHING  — melee pressure, advance + attack simultaneously
 *   CONDEMNING — pursuit at scaling speed, no attacks during gap-close
 *
 * Engagement duration scales damage in CRUSHING.
 * Condemning speed scales with target distance (the farther they run, the faster it closes).
 */
public class TemplarBrain extends MobBrain {

    public enum Stance implements BrainState {
        CRUSHING, CONDEMNING;
        @Override public String id() { return name(); }
    }

    // Reuse Stance as CombatState — they're unified for the Templar
    private static final float MELEE_THRESHOLD      = 4.0f;
    private static final float ENGAGEMENT_THRESHOLD  = 10.0f;
    /** Distance that triggers CONDEMNING */
    private static final float CONDEMN_THRESHOLD    = 8.0f;
    private static final float FLEE_THRESHOLD       = 12.0f;
    private static final float MAX_FLEE_DISTANCE    = 24.0f;
    private static final float SPEED_SCALAR         = 1.5f;

    private static final int DIVINE_FALL_CD = 300;
    private static final int DASH_CD        = 80;

    /** Templar attacks while advancing within this range (both CRUSHING and CONDEMNING). */
    private static final float MELEE_ATTACK_RANGE   = 8.0f;
    /** 1 second between attacks as specified for the advancing attack behaviour. */
    private static final int   MELEE_ATTACK_CD      = 20;
    /** Arc parameters — large sweep for a claymore/glaive wielder. */
    private static final float MELEE_ARC_HALF_ANGLE = 90f;
    private static final float MELEE_ARC_RANGE      = 4.0f;

    /** Ticks continuously spent in CRUSHING — used for damage scaling. */
    private int engagementDurationTicks = 0;

    private final TemplarEntity templar;

    public TemplarBrain(TemplarEntity entity) {
        super(entity, Stance.CRUSHING, Stance.CRUSHING);
        this.templar = entity;
    }

    @Override
    protected AIStimulus createStimulus() {
        return new AIStimulus(entity, MELEE_THRESHOLD, ENGAGEMENT_THRESHOLD, FLEE_THRESHOLD, 8.0f);
    }

    @Override
    protected List<Transition> stanceTransitions() {
        return List.of(
            // CRUSHING → CONDEMNING: player fled beyond condemn threshold
            new Transition(Stance.CRUSHING,   s -> s.targetDistance > CONDEMN_THRESHOLD, Stance.CONDEMNING, 10),
            // CONDEMNING → CRUSHING: player is in melee range again
            new Transition(Stance.CONDEMNING, s -> s.targetInMeleeRange,                 Stance.CRUSHING,   10)
        );
    }

    @Override
    protected List<Transition> combatStateTransitions() {
        // Combat state mirrors stance for the Templar — handled via setStance/setCombatState sync
        return List.of();
    }

    @Override
    protected List<PhaseTransition> definePhaseTransitions() {
        return List.of();
    }

    @Override
    protected void onStanceEnter(BrainState stance) {
        // Keep combatState in sync with stance
        currentCombatState = stance;

        if (stance == Stance.CRUSHING) {
            engagementDurationTicks = 0; // reset on re-entry
        }
    }

    @Override
    protected void executeCurrentCombatState() {
        if (entity.getTarget() == null) return;

        if (currentStance == Stance.CRUSHING) {
            tickCrushing();
        } else {
            tickCondemning();
        }
    }

    // ── CRUSHING ─────────────────────────────────────────────────────────────

    private void tickCrushing() {
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);
        engagementDurationTicks++;

        // Always advance — swing forward momentum included
        if (entity.distanceTo(entity.getTarget()) > MELEE_THRESHOLD) {
            entity.getNavigation().startMovingTo(
                entity.getTarget().getX(),
                entity.getTarget().getY(),
                entity.getTarget().getZ(),
                getEngagementSpeedBonus());
        }

        if (entity.performing) return;
        boolean canSee = stimulus.lineOfSightToTarget;
        if (!canSee) return;

        // Divine fall ability (staff attack)
        if (cooldowns.isReady("divine_fall") && entity.canSee(entity.getTarget())) {
            performDivineFall();
        }
        // Dash toward target if slightly out of reach and dash is ready
        else if (cooldowns.isReady("dash") && stimulus.targetDistance > 5 && stimulus.targetDistance < 12) {
            performDash();
        }
        // Always swing while within advancing range — 1 second between attacks
        else if (cooldowns.isReady("melee") && stimulus.targetDistance <= MELEE_ATTACK_RANGE) {
            performMeleeAttack();
        }
    }

    /** Slight speed bonus that scales with engagement duration (caps at +30%). */
    private double getEngagementSpeedBonus() {
        return 1.0 + Math.min(0.30, engagementDurationTicks * 0.0003);
    }

    // ── CONDEMNING ────────────────────────────────────────────────────────────

    private void tickCondemning() {
        entity.getLookControl().lookAt(entity.getTarget(), 360, 360);
        engagementDurationTicks = 0; // reset — not in engagement

        // Speed scales with distance: the farther the player, the faster the Templar
        double speedBonus = computeCondemnSpeed();
        entity.getNavigation().startMovingTo(
            entity.getTarget().getX(),
            entity.getTarget().getY(),
            entity.getTarget().getZ(),
            speedBonus);

        // Keep swinging while advancing when within 8 blocks — 1 second between attacks
        if (!entity.performing && cooldowns.isReady("melee") && stimulus.targetDistance <= MELEE_ATTACK_RANGE) {
            performMeleeAttack();
        }
    }

    private double computeCondemnSpeed() {
        float dist = stimulus.targetDistance;
        float t = MathHelper.clamp((dist - CONDEMN_THRESHOLD) / (MAX_FLEE_DISTANCE - CONDEMN_THRESHOLD), 0f, 1f);
        return 1.0 + t * SPEED_SCALAR;
    }

    // ── Abilities ─────────────────────────────────────────────────────────────

    private void performDivineFall() {
        int cd = Math.max(150, DIVINE_FALL_CD - (int)(DIVINE_FALL_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("divine_fall", cd);
        entity.performing = true;
        entity.getNavigation().stop();
        entity.dispatcher.setStaff();

        var spell = SpellRegistry.from(entity.getWorld()).getEntry(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "divine_fall"));
        for (int i = 0; i < 5; i++) {
            final int idx = i;
            ((WorldScheduler) entity.getWorld()).schedule(20 * (idx + 1), () -> {
                if (entity.getTarget() != null && entity.canSee(entity.getTarget())) {
                    SpellHelper.ImpactContext context = new SpellHelper.ImpactContext(
                        1.0F, 1.0F, entity.getTarget().getPos(),
                        SpellPower.getSpellPower(SpellSchools.HEALING, entity),
                        SpellTarget.FocusMode.DIRECT, 0);
                    net.spell_engine.utils.SoundHelper.playSound(entity.getWorld(), entity,
                        new net.spell_engine.api.spell.fx.Sound(net.spell_engine.fx.SpellEngineSounds.GENERIC_HEALING_RELEASE.id()));
                    ((WorldScheduler) entity.getWorld()).schedule(25, () -> {
                        if (entity.getTarget() != null && entity.canSee(entity.getTarget())) {
                            net.spell_engine.internals.SpellHelper.fallProjectile(
                                entity.getWorld(), entity, entity.getTarget(),
                                entity.getTarget().getPos(), spell.get(), context);
                        }
                    });
                }
            });
        }

        ((WorldScheduler) entity.getWorld()).schedule(160, () -> {
            entity.performing = false;
        });
    }

    private void performDash() {
        int cd = Math.max(40, DASH_CD - (int)(DASH_CD * entity.getCooldownCoeff()));
        cooldowns.trigger("dash", cd);
        entity.performing = true;

        Vec3d toward = entity.getTarget().getPos().subtract(entity.getPos()).normalize();
        entity.setVelocity(toward.x * 1.5, 0.2, toward.z * 1.5);

        ((WorldScheduler) entity.getWorld()).schedule(10, () -> {
            entity.performing = false;
        });
    }

    private void performMeleeAttack() {
        if (entity.getTarget() == null) return;
        cooldowns.trigger("melee", MELEE_ATTACK_CD);
        entity.tryAttack(entity.getTarget());
        performArcDamage(entity.getTarget(), MELEE_ARC_HALF_ANGLE, MELEE_ARC_RANGE);
    }

    /**
     * Called from TemplarEntity.damage() to check/trigger the parry cooldown.
     * Returns true if parry should fire this hit.
     */
    public boolean tryParry() {
        if (cooldowns.isReady("parry")) {
            cooldowns.trigger("parry", Math.max(80, 160 - (int)(160 * entity.getCooldownCoeff())));
            return true;
        }
        return false;
    }
}

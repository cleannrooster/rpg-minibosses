package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossAnimationProvider;
import com.cleannrooster.rpg_minibosses.entity.TemplarEntity;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import com.cleannrooster.rpg_minibosses.entity.combat.ActionContext;
import com.cleannrooster.rpg_minibosses.entity.combat.AttackMotion;
import com.cleannrooster.rpg_minibosses.entity.combat.CombatAction;
import com.cleannrooster.rpg_minibosses.entity.combat.SlashProfile;
import com.cleannrooster.rpg_minibosses.entity.combat.SlashVisual;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackPlane;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackShape;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackVolume;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.SwingDynamics;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.SwingPath;
import net.minecraft.particle.ParticleTypes;
import com.cleannrooster.rpg_minibosses.entity.combat.MovementProfile;
import com.cleannrooster.rpg_minibosses.entity.combat.TrackingMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.internals.target.SpellTarget;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchools;

import java.util.List;

/**
 * AI brain for the Templar.
 *
 * <p><b>Identity: relentless forward pressure.</b> Of the five, this is the closest grounded analogue to
 * the Visage of War's philosophy — it advances the front line and its attacks move <em>through</em> the
 * target rather than stopping at it. The loop is deliberately never
 * {@code attack -> stand still -> restart navigation}: every sweep's recovery already biases into the
 * next step, and the clip itself ends leaning forward rather than back at neutral.
 *
 * <p>Preserved: CRUSHING and CONDEMNING, engagement scaling, Dash, Divine Fall, the sweeping melee and
 * Parry. What changed is that Dash is now an explicit committed charge that flows straight into a cut,
 * and Condemning expresses itself through steering authority rather than a navigation speed multiplier.
 */
public class TemplarBrain extends MobBrain {

    public enum Stance implements BrainState {
        CRUSHING, CONDEMNING;
        @Override public String id() { return name(); }
    }

    private static final float MELEE_THRESHOLD      = 4.0f;
    private static final float ENGAGEMENT_THRESHOLD  = 10.0f;
    /** Distance that triggers CONDEMNING */
    private static final float CONDEMN_THRESHOLD    = 8.0f;
    private static final float FLEE_THRESHOLD       = 12.0f;
    private static final float MAX_FLEE_DISTANCE    = 24.0f;

    /** Arc parameters — a large sweep for a claymore/glaive wielder. */
    private static final float MELEE_ARC_HALF_ANGLE = 90f;
    private static final float MELEE_ARC_RANGE      = 4.0f;
    /** The Templar wants to be inside this, and keeps walking to stay there. */
    private static final double PREFERRED_RANGE     = 2.8;

    /** Ticks continuously spent in CRUSHING — used for engagement scaling. */
    private int engagementDurationTicks = 0;
    /** Alternates the sweep direction so consecutive basics read as different moves. */
    private boolean sweepRightToLeft = true;

    private final TemplarEntity templar;

    private final CombatAction sweepRl;
    private final CombatAction sweepLr;
    private final CombatAction dash;
    private final CombatAction dashCut;
    private final CombatAction divineFall;

    public TemplarBrain(TemplarEntity entity) {
        super(entity, Stance.CRUSHING, Stance.CRUSHING);
        this.templar = entity;

        var family = MinibossAnimationProvider.MinibossAnimationDispatcher.TEMPLAR;

        this.sweepRl = sweep("templar_sweep_rl", family + "sweep_rl", true);
        this.sweepLr = sweep("templar_sweep_lr", family + "sweep_lr", false);

        // The follow-up a dash flows into. Short and cheap because the dash already paid the telegraph.
        // Declared before the dash so the dash's chain hook can reference an already-assigned field.
        this.dashCut = CombatAction.builder("templar_dash_cut")
                .animation(family + "dash_cut")
                .timing(6, 4, 8)
                .motion(AttackMotion.PRESSING)
                .advance(0.75)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(40)
                // A rising cross-cut: the clip drives it from low on the right up to high on the left, so
                // the arc is a rising sweep through a steep diagonal plane rather than another flat swipe.
                .volume(new AttackVolume(AttackShape.DIAGONAL_ARC,
                        SwingPath.rising(120f, AttackPlane.DIAGONAL_STEEP)
                                .withDynamics(SwingDynamics.FORCEFUL),
                        MELEE_ARC_RANGE, 0.7, 2.0, 2.2, 0.3, 0.35, 1.2))
                .weaponDamage()
                .knockback(0.5, 0.2)
                .slash(SlashVisual.light(ParticleTypes.ENCHANTED_HIT))
                .ribbon(SlashProfile.crescent(1.0f, 0.96f, 0.78f))
                .build();

        // Dash: a short plant with a visible forward load, then a committed burst with no late steering.
        // If it arrives in reach, it flows straight into a cut — dash then attack, never dash, idle,
        // reconsider, attack.
        this.dash = CombatAction.builder("templar_dash")
                .animation(family + "dash")
                .timing(8, 10, 5)
                .motion(AttackMotion.COMMITTED_VECTOR)
                .committed(0.62, 10, 0.0, 0.0)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(160)
                .urgentRecoveryTrim(3)
                // The pass itself is damaging: a volume carried along with the charge, sweeping the segment
                // covered since last tick so a fast dash cannot skip past someone standing in the lane.
                .volume(AttackVolume.path(4.0, 2.4, 2.6))
                .scaledDamage(0.4f)
                .knockback(0.7, 0.25)
                .multiHit(8)
                .slash(SlashVisual.light(ParticleTypes.ENCHANTED_HIT).withCount(10))
                // A lane, so the trail draws the length the charge actually sweeps.
                .ribbon(SlashProfile.streak(0.86f, 0.93f, 1.0f))
                .onStart(ctx ->
                        ctx.entity().playSound(SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), 0.9f, 0.8f))
                .onRecoveryStart(ctx -> {
                    var target = ctx.liveTarget();
                    if (target != null && entity.distanceTo(target) <= MELEE_ARC_RANGE + 1.5f) {
                        chain(this.dashCut);
                    }
                })
                .build();

        // Divine Fall. The one moment the otherwise relentless Templar voluntarily becomes still, which is
        // exactly why it is worth keeping planted: the contrast is the telegraph. The clip is authored to
        // the exact length of the channel, so the planted pose holds for all of it.
        this.divineFall = CombatAction.builder("templar_divine_fall")
                .animation(family + "divine_fall")
                .timing(16, 60, 14)
                .motion(AttackMotion.PLANTED)
                .tracking(TrackingMode.MINIMAL, TrackingMode.LOCKED)
                .cooldown(600)
                .onStart(ctx -> ctx.locomotion().arrest(MovementProfile.HARD_STOP))
                .onActiveTick(ctx -> {
                    if (ctx.phaseTick() % 15 == 1) {
                        callDown(ctx);
                    }
                })
                .onFinish(ctx -> {
                    // Completion is not a pause. It reacquires pressure immediately and accelerates back
                    // toward the fight; if the player is already close it simply swings.
                    var target = ctx.liveTarget();
                    if (target != null && entity.distanceTo(target) <= MELEE_ARC_RANGE + 1.0f) {
                        chain(nextSweep());
                    }
                })
                .build();
    }

    /**
     * One advancing sweep. Both directions share timing and geometry; only the clip and the arc's visual
     * travel differ, which is what makes back-to-back basics read as a combination.
     */
    private CombatAction sweep(String id, String animation, boolean rightToLeft) {
        return CombatAction.builder(id)
                .animation(animation)
                .timing(10, 5, 7)
                .motion(AttackMotion.PRESSING)
                .windupDrift(0.25)   // it keeps walking through the wind-back
                .advance(1.25)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(52)
                .urgentRecoveryTrim(3)
                // Direction is stated, not inferred: sweepRl travels right to left and sweepLr the other
                // way, matching their two clips. The hand-rolled particle crescent this replaces was drawn
                // from an unrelated parametric curve; the arc is now sampled from the surface that
                // damages, so the two cannot disagree.
                .volume(AttackVolume.sweep(
                        (rightToLeft ? SwingPath.rightToLeft(MELEE_ARC_HALF_ANGLE * 2f)
                                     : SwingPath.leftToRight(MELEE_ARC_HALF_ANGLE * 2f))
                                .withDynamics(SwingDynamics.FORCEFUL),
                        MELEE_ARC_RANGE, 3.0))
                .weaponDamage()
                .knockback(0.55, 0.12)
                .slash(SlashVisual.light(ParticleTypes.ENCHANTED_HIT).withCount(22))
                .ribbon(SlashProfile.heavyCrescent(1.0f, 0.96f, 0.78f))
                .build();
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
        var target = entity.getTarget();
        if (target == null) return;

        if (actionOwnsMovement()) {
            return;
        }
        if (tryChain()) {
            return;
        }

        if (currentStance == Stance.CRUSHING) {
            tickCrushing(target);
        } else {
            tickCondemning(target);
        }
    }

    // ── CRUSHING ─────────────────────────────────────────────────────────────

    private void tickCrushing(LivingEntity target) {
        engagementDurationTicks++;
        var profile = crushProfile();
        locomotion.faceTarget(target, profile.turnDegrees());

        float distance = stimulus.targetDistance;
        if (distance > 12.0f || !stimulus.lineOfSightToTarget) {
            locomotion.navigateTo(target.getX(), target.getY(), target.getZ(), 1.0);
        } else {
            locomotion.setFallbackGoal(target.getPos(), 1.1);
            locomotion.pressure(target, PREFERRED_RANGE, profile);
        }

        if (actions.isBusy() || !stimulus.lineOfSightToTarget) {
            return;
        }

        if (cooldowns.isReady("templar_divine_fall") && entity.canSee(target)) {
            startAction(this.divineFall);
        } else if (cooldowns.isReady("templar_dash") && distance > 5.0f && distance < 12.0f) {
            startAction(this.dash);
        } else if (distance <= MELEE_ARC_RANGE + 1.0f) {
            startAction(nextSweep());
        }
    }

    /**
     * Engagement scaling, expressed as movement authority rather than as a navigation multiplier: the
     * longer it has been on top of the player, the more speed and turning it commits to staying there.
     */
    private MovementProfile crushProfile() {
        var scale = 1.0 + Math.min(0.28, engagementDurationTicks * 0.0004);
        return MovementProfile.RELENTLESS.withMaxSpeed(MovementProfile.RELENTLESS.maxSpeed() * scale);
    }

    // ── CONDEMNING ────────────────────────────────────────────────────────────

    private void tickCondemning(LivingEntity target) {
        engagementDurationTicks = 0; // reset — not in engagement
        var profile = condemnProfile();
        locomotion.faceTarget(target, profile.turnDegrees());

        float distance = stimulus.targetDistance;
        if (distance > 14.0f || !stimulus.lineOfSightToTarget) {
            locomotion.navigateTo(target.getX(), target.getY(), target.getZ(), 1.4);
        } else {
            locomotion.setFallbackGoal(target.getPos(), 1.4);
            locomotion.pressure(target, PREFERRED_RANGE, profile);
        }

        if (actions.isBusy()) {
            return;
        }
        // Condemning is pursuit, but not a passive one: a dash is the correct answer to a fleeing player,
        // and it still swings the moment the target is inside reach.
        if (cooldowns.isReady("templar_dash") && distance > 5.0f && distance < 14.0f
                && stimulus.lineOfSightToTarget) {
            startAction(this.dash, 0.7);
        } else if (distance <= MELEE_ARC_RANGE + 1.0f) {
            startAction(nextSweep());
        }
    }

    /**
     * The farther the player runs, the harder the pursuit response — more speed, more acceleration, less
     * hesitation. Capped so it never becomes an unavoidable snap.
     */
    private MovementProfile condemnProfile() {
        float dist = stimulus.targetDistance;
        float t = MathHelper.clamp((dist - CONDEMN_THRESHOLD) / (MAX_FLEE_DISTANCE - CONDEMN_THRESHOLD),
                0f, 1f);
        return MovementProfile.CONDEMN
                .withMaxSpeed(MovementProfile.CONDEMN.maxSpeed() * (1.0 + t * 0.35))
                .urgent(1.0, 1.0 + t * 0.4, 1.0f);
    }

    // ── Abilities ─────────────────────────────────────────────────────────────

    private CombatAction nextSweep() {
        this.sweepRightToLeft = !this.sweepRightToLeft;
        return this.sweepRightToLeft ? this.sweepRl : this.sweepLr;
    }

    /** One Divine Fall strike on the target's current position. */
    private void callDown(ActionContext ctx) {
        var target = ctx.liveTarget();
        if (target == null || !entity.canSee(target) || entity.getWorld().isClient()) {
            return;
        }
        var spell = SpellRegistry.from(entity.getWorld())
                .getEntry(Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "divine_fall"));
        var context = new SpellHelper.ImpactContext(
                1.0F, 1.0F, target.getPos(),
                SpellPower.getSpellPower(SpellSchools.HEALING, entity),
                SpellTarget.FocusMode.DIRECT, 0);
        net.spell_engine.utils.SoundHelper.playSound(entity.getWorld(), entity,
                new net.spell_engine.api.spell.fx.Sound(
                        net.spell_engine.fx.SpellEngineSounds.GENERIC_HEALING_RELEASE.id()));
        SpellHelper.fallProjectile(entity.getWorld(), entity, target, target.getPos(),
                spell.get(), context);
    }

    /**
     * Called from {@link TemplarEntity#damage} to check/trigger the parry.
     *
     * <p>Reactive and deliberately outside the action runner: a parry that occupied the runner would
     * block whatever the Templar was already committed to, and freezing the AI on every blocked hit is
     * precisely the dead time this pass exists to remove. It plays its own short clip instead.
     */
    public boolean tryParry() {
        if (!cooldowns.isReady("parry")) {
            return false;
        }
        cooldowns.trigger("parry", Math.max(160, 320 - (int)(320 * entity.getCooldownCoeff())));
        entity.dispatcher.play("dash",
                MinibossAnimationProvider.MinibossAnimationDispatcher.TEMPLAR + "parry", false, 1.0f);
        return true;
    }
}

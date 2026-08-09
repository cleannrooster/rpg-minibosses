package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossAnimationProvider;
import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import com.cleannrooster.rpg_minibosses.entity.combat.ActionContext;
import com.cleannrooster.rpg_minibosses.entity.combat.AttackMotion;
import com.cleannrooster.rpg_minibosses.entity.combat.CombatAction;
import com.cleannrooster.rpg_minibosses.entity.combat.CombatDebug;
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
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.Spell;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.minecraft.server.world.ServerWorld;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineParticles;
import net.spell_engine.internals.SpellHelper;
import net.spell_engine.utils.TargetHelper;
import net.spell_power.api.SpellPower;

import java.util.List;

/**
 * AI brain for the Juggernaut.
 *
 * <p><b>Identity: mass and commitment.</b> It is not fast and it is not agile. Its urgency comes from the
 * fact that once it decides something, it happens — a long acceleration into a walk that will not stop, a
 * hammer that carries its whole body weight through the swing, and stops that are absolute rather than
 * gradual. Nothing here is made more threatening by increasing a speed number.
 *
 * <p>Movement: strong forward bias, low lateral authority (see {@link MovementProfile#HEAVY_ADVANCE}), so
 * it is genuinely bad at redirecting — but it retains just enough sideways correction at contact range to
 * slide around a body-blocking player instead of grinding into them.
 *
 * <p>Preserved from the original brain: the ADVANCING/BRACING states, the LAST_STAND phase transition at
 * 20% health with BRACING locked out, and the Slam / Spin / Leap kit. What changed is that each of those
 * is now an explicit windup/active/recovery action with stated movement ownership, rather than a chain of
 * scheduled callbacks firing velocity impulses next to a navigator that was still dragging the mob.
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

    private static final int BRACE_DURATION = 50;

    /** Arc parameters — a broad sweep for a large hammer wielder. */
    private static final float MELEE_ARC_HALF_ANGLE = 80f;
    private static final float MELEE_ARC_RANGE      = 3.8f;
    /** Spin reaches slightly further than normal melee; 180° covers the full rotation. */
    private static final float SPIN_ARC_RANGE       = 5.0f;
    /** The band it wants to fight in. Inside this, the hammer is already in reach. */
    private static final double PREFERRED_RANGE     = 2.6;
    /**
     * Ticks the leap spends in the air. This <b>must</b> equal the leap action's active window, so the arc,
     * the clip's landing frame and the impact all end on the same tick.
     */
    private static final int LEAP_FLIGHT_TICKS      = 14;

    private final JuggernautEntity juggernaut;

    // Actions are built per mob so their hooks can close over this brain directly. There are only ever a
    // handful of these alive, and the alternative — statics plus a cast back to the brain in every hook —
    // buys nothing but noise.
    private final CombatAction heavySwing;
    private final CombatAction shoulderCheck;
    private final CombatAction slam;
    private final CombatAction leap;
    private final CombatAction spin;

    public JuggernautBrain(JuggernautEntity entity) {
        super(entity, Stance.ADVANCING, CombatState.ADVANCING);
        this.juggernaut = entity;

        var family = MinibossAnimationProvider.MinibossAnimationDispatcher.JUGG;

        // The ordinary hammer swing. A long wind-back over the right shoulder, then a broad arc with a
        // short attack-owned forward step so the body visibly commits its weight into the blow — the
        // stationary arm-only swing this replaces was the single least convincing thing it did.
        this.heavySwing = CombatAction.builder("jugg_heavy_swing")
                .animation(family + "heavy_swing")
                .timing(14, 5, 9)
                .motion(AttackMotion.PRESSING)
                .windupDrift(-0.15)   // a small load backwards before the weight goes forward
                .advance(1.05)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(68)
                .urgentRecoveryTrim(4)
                // The clip winds back over the right shoulder and carries across to the left, so the arc
                // is authored to travel the same way. FORCEFUL matches the clip's own easing: two thirds of
                // the blade's travel happens in the middle third of the window, which is where the
                // animation puts its impact frame.
                .volume(AttackVolume.sweep(
                        SwingPath.rightToLeft(MELEE_ARC_HALF_ANGLE * 2f)
                                .withDynamics(SwingDynamics.FORCEFUL),
                        MELEE_ARC_RANGE, 2.8))
                .weaponDamage()
                .knockback(0.6, 0.12)
                .slash(SlashVisual.heavy(ParticleTypes.CRIT))
                // A broad, thick crescent of exactly the hammer's reach and arc.
                .ribbon(SlashProfile.heavyCrescent(1.0f, 0.90f, 0.66f))
                .onStart(ctx -> ctx.entity().playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 0.5f, 0.6f))
                .build();

        // New: point-blank spacing tool. The hammer has a dead zone, and standing inside it used to be
        // free. This is deliberately cheap, short and low-damage — it exists to move a player, not to
        // kill one, and it fills the one spacing niche the kit genuinely lacked.
        this.shoulderCheck = CombatAction.builder("jugg_shoulder_check")
                .animation(family + "shoulder_check")
                .timing(6, 3, 8)
                .motion(AttackMotion.PRESSING)
                .advance(0.7)
                .tracking(TrackingMode.FULL, TrackingMode.REDUCED)
                .cooldown(140)
                // A wedge, not a blade: it resolves its whole span at once, because with a shove the
                // motion is the read and there is no travelling edge to follow.
                .volume(AttackVolume.cone(62f, 2.6))
                .scaledDamage(0.45f)
                .knockback(1.15, 0.28)
                .slash(SlashVisual.blunt(ParticleTypes.LARGE_SMOKE))
                .onActiveStart(ctx ->
                        ctx.entity().playSound(SoundEvents.ENTITY_IRON_GOLEM_ATTACK, 0.9f, 0.7f))
                .build();

        // Slam stays the planted major attack. The rhythm is the point: advance, violent arrest, hammer
        // raised, impact, short heavy recovery, straight back into pressure. Nothing may drift during it.
        this.slam = CombatAction.builder("jugg_slam")
                .animation(family + "slam")
                .timing(22, 4, 14)
                .motion(AttackMotion.PLANTED)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(280)
                .urgentRecoveryTrim(6)
                .onActiveStart(ctx -> groundImpact(6f))
                .build();

        // Leap. The destination is captured at the moment of launch and then never revisited — mid-air
        // correction is what turns a leap into a homing missile, and dodging after commitment is the
        // entire counterplay. The arc is derived from the captured distance so the landing is honest.
        this.leap = CombatAction.builder("jugg_leap")
                .animation(family + "leap")
                .timing(16, 14, 12)
                .motion(AttackMotion.COMMITTED_VECTOR)
                .committed(0.5, 14, 0.5, 0.0)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(320)
                .urgentRecoveryTrim(4)
                .onActiveStart(this::launchLeap)
                .onRecoveryStart(ctx -> {
                    groundImpact(6f);
                    // Landing next to a player is a slam opportunity; the chain is what removes the beat
                    // of nothing that used to sit between a leap and its follow-up.
                    var target = ctx.liveTarget();
                    if (target != null && entity.distanceTo(target) <= 4.5f && cooldowns.isReady("jugg_slam")) {
                        chain(this.slam);
                    }
                })
                .build();

        // Spin. One coherent moving attack rather than a series of independent speed boosts: it commits to
        // a heading, turns slowly, and the damage pulses correspond to the rotating weapon. The clip is
        // looped for the duration, which is why it starts turning on the spot during the windup — the
        // rotation builds first, then the rotation carries it.
        this.spin = CombatAction.builder("jugg_spin")
                .animation(family + "spin")
                .animationLoops(true)
                .timing(16, 62, 16)
                .motion(AttackMotion.COMMITTED_VECTOR)
                .committed(0.185, 62, 0.0, 0.028)   // a slow, heavy drift with barely any steering authority
                .tracking(TrackingMode.MINIMAL, TrackingMode.LOCKED)
                .cooldown(920)
                .onActiveStart(ctx -> {
                    var target = ctx.liveTarget();
                    if (target != null) {
                        ctx.locomotion().setCommittedAim(target.getPos());
                    }
                    spinPulse();
                })
                .onActiveTick(ctx -> {
                    // Pulses land on the beats the hammer passes through, not on an arbitrary timer.
                    if (ctx.phaseTick() % 8 == 0) {
                        spinPulse();
                    }
                    var target = ctx.liveTarget();
                    if (target != null) {
                        ctx.locomotion().setCommittedAim(target.getPos());
                    }
                })
                .onRecoveryStart(ctx -> {
                    // Strong winddown then a hard arrest — the momentum runs out against the ground.
                    ctx.entity().dispatcher.play("attacks", family + "spin_end", false, 1.0f);
                    ctx.locomotion().cancelCommitted();
                    spinPulse();
                })
                .build();
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
                && currentStance != Stance.LAST_STAND
                && !actions.isBusy(),
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
                    cooldowns.clear("jugg_slam");
                    cooldowns.clear("jugg_spin");
                    cooldowns.clear("jugg_leap");
                    cooldowns.clear("jugg_heavy_swing");
                    // Last Stand is not "the same thing, faster". The telegraphs are untouched; what
                    // changes is the dead space around them and how quickly it launches.
                    actions.setUrgent(true);
                    entity.playSound(SoundEvents.ENTITY_RAVAGER_ROAR, 1.4f, 0.55f);
                })
        );
    }

    @Override
    protected void executeCurrentCombatState() {
        var target = entity.getTarget();
        if (target == null) return;

        if (currentCombatState == CombatState.ADVANCING) {
            tickAdvancing(target);
        } else {
            tickBracing(target);
        }
    }

    @Override
    protected void onCombatStateEnter(BrainState state) {
        if (state == CombatState.BRACING) {
            // Snap to zero and settle into the defensive silhouette. No coasting into a block.
            actions.cancel();
            locomotion.arrest(MovementProfile.HARD_STOP);
            entity.getNavigation().stop();
            entity.dispatcher.play("attacks",
                    MinibossAnimationProvider.MinibossAnimationDispatcher.JUGG + "brace", false, 1.0f);
            cooldowns.trigger("brace", BRACE_DURATION);
        } else if (state == CombatState.ADVANCING) {
            // Brace does not end in a pause. It ends in an advancing action, or failing that, in movement.
            var target = entity.getTarget();
            if (target != null && entity.distanceTo(target) <= MELEE_ARC_RANGE + 1.0f) {
                chain(this.heavySwing);
            }
        }
    }

    // ── ADVANCING ─────────────────────────────────────────────────────────────

    private void tickAdvancing(LivingEntity target) {
        var profile = profile();

        if (actionOwnsMovement()) {
            return; // the attack is carrying the body; nothing else may touch it
        }
        if (tryChain()) {
            return;
        }

        locomotion.faceTarget(target, profile.turnDegrees());
        float distance = entity.distanceTo(target);

        // Macro traversal is still the pathfinder's job — direct steering is for the combat envelope.
        // Handing off at range is what stops it from walking face-first into terrain on a long approach.
        if (distance > 12.0f || !stimulus.lineOfSightToTarget) {
            locomotion.navigateTo(target.getX(), target.getY(), target.getZ(), 1.0);
        } else {
            locomotion.setFallbackGoal(target.getPos(), 1.0);
            locomotion.pressure(target, PREFERRED_RANGE, profile);
        }

        if (actions.isBusy()) {
            return;
        }

        // Priority reads as a spacing ladder: leap closes a real gap, spin punishes a mid-range circle,
        // slam punishes standing still, the shoulder check punishes hugging, and the swing is the rhythm.
        if (distance >= 6.0f && cooldowns.isReady("jugg_leap") && entity.canSee(target)) {
            startAction(this.leap);
        } else if (distance <= 10.0f && distance >= 3.0f && cooldowns.isReady("jugg_spin")
                && entity.canSee(target)) {
            startAction(this.spin);
        } else if (distance <= MELEE_THRESHOLD && cooldowns.isReady("jugg_slam")) {
            startAction(this.slam);
        } else if (distance <= 1.9f && cooldowns.isReady("jugg_shoulder_check")) {
            startAction(this.shoulderCheck);
        } else if (distance <= MELEE_ARC_RANGE) {
            startAction(this.heavySwing);
        }
    }

    /** Last Stand keeps the same mass; it simply stops hesitating. */
    private MovementProfile profile() {
        return currentStance == Stance.LAST_STAND
                ? MovementProfile.HEAVY_DESPERATE
                : MovementProfile.HEAVY_ADVANCE;
    }

    // ── BRACING ───────────────────────────────────────────────────────────────

    private void tickBracing(LivingEntity target) {
        locomotion.faceTarget(target, 6.0f);
        locomotion.arrest(MovementProfile.HARD_STOP);

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

    // ── Ability implementation ────────────────────────────────────────────────

    /**
     * Fix the leap's destination at the instant of launch and derive the arc from it.
     *
     * <p>The whole shape of the jump — how long it is airborne, how fast it travels, how high it rises —
     * comes from the distance captured here. That is what makes it land where it was aimed instead of
     * being a fixed impulse along whatever vector the body happened to be facing.
     */
    private void launchLeap(ActionContext ctx) {
        var target = ctx.liveTarget();
        var destination = target != null ? target.getPos() : ctx.committedOrigin()
                .add(ctx.committedForward().multiply(6.0));
        if (!CombatDebug.isSaneDestination(entity, destination)) {
            destination = ctx.committedOrigin().add(ctx.committedForward().multiply(6.0));
        }

        var toDestination = new Vec3d(destination.x - entity.getX(), 0.0, destination.z - entity.getZ());
        var distance = MathHelper.clamp(toDestination.length(), 2.0, 14.0);

        // The flight lasts exactly the action's active window, and the distance is spent by varying the
        // speed rather than the duration. Deriving the duration from the distance instead — which is what
        // this did — desynchronised everything: a long leap ran past the active phase, so entering recovery
        // cancelled the committed motion and dropped the Juggernaut out of its own jump, and the landing
        // impact fired in mid-air. Now the clip's landing frame, the end of the arc and the impact are the
        // same tick by construction.
        var speed = Math.min(1.05, distance / LEAP_FLIGHT_TICKS);
        // Vertical impulse for that flight time, given vanilla's 0.08/tick gravity: the mob is airborne for
        // roughly 2*v0/g ticks, so this is the height the arc actually needs rather than a guess.
        var lift = MathHelper.clamp(0.042 * LEAP_FLIGHT_TICKS, 0.42, 0.78);

        ctx.locomotion().beginCommittedMotion(toDestination, speed, LEAP_FLIGHT_TICKS, lift, 0.0);
        entity.playSound(SoundEvents.ENTITY_RAVAGER_STEP, 1.2f, 0.5f);
    }

    /** Shared impact for Slam and the Leap landing. */
    private void groundImpact(float radius) {
        if (entity.getWorld().isClient()) return;
        var world = entity.getWorld();
        var id = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "pound");
        ParticleHelper.sendBatches(entity, SpellRegistry.from(world).get(id).release.particles);
        // Filtered by the same predicate the melee arcs use. Passing null here meant Slam and the Leap
        // landing hit every living thing in six blocks — the mob's own owner and its allies included.
        for (Entity struck : TargetHelper.targetsFromArea(entity, radius, new Spell.Target.Area(),
                entity::canHarm)) {
            SpellHelper.performImpacts(world, entity, struck, entity,
                SpellRegistry.from(world).getEntry(id).get(),
                SpellRegistry.from(world).get(id).impacts,
                new SpellHelper.ImpactContext()
                    .power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, entity))
                    .position(entity.getPos()));
        }
        entity.playSound(SoundEvents.ENTITY_GENERIC_EXPLODE.value());
    }

    /**
     * One beat of the spin.
     *
     * <p>A full ring rather than an arc, because by this point in the turn the hammer has been everywhere:
     * a pulse summarises half a revolution rather than one blade position. Resolving it as a radial volume
     * means the reach that damages is exactly the reach that gets drawn.
     */
    private void spinPulse() {
        if (entity.getWorld().isClient()) return;
        strikeVolume(AttackVolume.radial(SPIN_ARC_RANGE), 0.6f, 0.7,
                SlashVisual.heavy(ParticleTypes.CRIT).withCount(40));
    }
}

package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossAnimationProvider;
import com.cleannrooster.rpg_minibosses.entity.TricksterEntity;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import com.cleannrooster.rpg_minibosses.entity.combat.AttackMotion;
import com.cleannrooster.rpg_minibosses.entity.combat.CombatAction;
import com.cleannrooster.rpg_minibosses.entity.combat.GroundLocomotion;
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
import net.minecraft.command.argument.EntityAnchorArgumentType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.api.spell.fx.Sound;
import net.spell_engine.api.spell.registry.SpellRegistry;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.internals.SpellExecution;
import net.spell_engine.internals.impact.SpellImpacts;
import net.spell_engine.internals.delivery.ProjectileLauncher;
import net.spell_engine.internals.delivery.CloudPlacer;
import net.spell_engine.utils.SoundHelper;
import net.spell_power.api.SpellPower;

import java.util.List;

/**
 * AI brain for the Trickster.
 *
 * <p><b>Identity: directional agility and positional attacks.</b> Its threat is where it attacks from,
 * not how fast it runs. Everything here is built around routing: it orbits rather than approaches, it
 * commits to an <em>attack lane</em> rather than to the player's hitbox, its basic melee crosses through
 * and exits past the shoulder rather than stopping face to face, and it leaves an engagement at an angle
 * instead of running backwards in a straight line.
 *
 * <p>Preserved: SHADOW/ASSAULT/RECOIL, STALKING/AMBUSHING/WITHDRAWING, invisibility gating, the two-knife
 * throw, melee, roll and the reactive dodge.
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

    private static final int AMBUSH_CD     = 400;
    private static final int AMBUSH_RESET_BUFFER = 40;
    /** 12-second cooldown on applying invisibility — entering STALKING without it is still valid. */
    private static final int STEALTH_CD    = 480;

    /** Arc parameters — a narrow quick slash for a small agile attacker. */
    private static final float MELEE_ARC_HALF_ANGLE = 55f;
    private static final float MELEE_ARC_RANGE      = 2.8f;

    /** Radius the Trickster orbits at while stalking. */
    private static final float ORBIT_RADIUS = 8.0f;
    /** Ticks of continuous open exposure before forcing WITHDRAWING. */
    private static final int MAX_EXPOSURE_TICKS = 100;
    /** How far out an ambush lane sits from the target. */
    private static final double LANE_RADIUS = 3.4;

    private int exposureTicks = 0;
    /** Which way it is currently circling. Flipped on a timer and whenever an approach fails. */
    private int orbitDirection = 1;
    private int orbitFlipTimer = 0;
    /** The lane chosen for the current ambush, in world space, and which side of the target it is on. */
    private Vec3d ambushLane;
    private int ambushSide = 1;

    private final TricksterEntity rogue;

    private final CombatAction crossSlashLeft;
    private final CombatAction crossSlashRight;
    private final CombatAction knifeThrow;
    private final CombatAction roll;

    public RogueBrain(TricksterEntity entity) {
        super(entity, Stance.SHADOW, CombatState.STALKING);
        this.rogue = entity;

        var family = MinibossAnimationProvider.MinibossAnimationDispatcher.TRICKSTER;

        // Roll: a committed locomotion action, not an impulse. The direction is captured at the start, the
        // duration is defined, and the clip's length matches the translation exactly. Declared first so the
        // crossing slashes can chain into an already-assigned field.
        this.roll = CombatAction.builder("rogue_roll")
                .animation(family + "roll")
                .timing(3, 13, 3)
                .motion(AttackMotion.COMMITTED_VECTOR)
                .committed(0.44, 13, 0.0, 0.0)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(160)
                .onStart(ctx -> ctx.entity().playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP,
                        0.5f, 1.4f))
                .build();

        this.crossSlashLeft  = crossSlash("rogue_cross_slash_l", family + "cross_slash_l", -1);
        this.crossSlashRight = crossSlash("rogue_cross_slash_r", family + "cross_slash_r", 1);

        // Knife throw stays a two-throw harassment tool, and stays mobile: freezing in place for a
        // lightweight ranged poke is exactly the kind of dead beat this pass removes. The clip only
        // animates the upper body, so the orbit underneath it still reads.
        this.knifeThrow = CombatAction.builder("rogue_throw")
                .animation(family + "throw_mobile")
                .timing(5, 9, 5)
                .motion(AttackMotion.MOBILE)
                .tracking(TrackingMode.FULL, TrackingMode.REDUCED)
                .cooldown(160)
                .onActiveStart(ctx -> throwKnife())
                .onActiveTick(ctx -> {
                    if (ctx.phaseTick() == 5) {
                        ctx.entity().dispatcher.play("attacks", family + "throw_mobile", false, 1.0f);
                        throwKnife();
                    }
                })
                .build();
    }

    /**
     * A crossing slash. The body bursts into range, cuts, and keeps going past the target's shoulder, so
     * the Trickster is never left standing directly in front of the player after an attack.
     */
    private CombatAction crossSlash(String id, String animation, int side) {
        return CombatAction.builder(id)
                .animation(animation)
                .timing(5, 8, 7)
                .motion(AttackMotion.CROSS_THROUGH)
                .committed(0.40, 8, 0.0, 0.0)
                .tracking(TrackingMode.FULL, TrackingMode.LOCKED)
                .cooldown(52)
                // The blade sweeps across the whole pass rather than landing on a single frame, which is
                // what makes crossing through someone hurt instead of being a free escape. It travels the
                // way its clip does: the left-exit slash cuts right to left.
                .volume(AttackVolume.sweep(
                        (side > 0 ? SwingPath.rightToLeft(MELEE_ARC_HALF_ANGLE * 2f)
                                  : SwingPath.leftToRight(MELEE_ARC_HALF_ANGLE * 2f))
                                .withDynamics(SwingDynamics.FORCEFUL),
                        MELEE_ARC_RANGE, 2.2)
                        .withInnerRadius(0.4))
                .weaponDamage()
                .knockback(0.3, 0.08)
                .slash(SlashVisual.light(ParticleTypes.CRIT))
                .ribbon(SlashProfile.crescent(0.80f, 0.90f, 1.0f))
                .onStart(ctx -> ctx.setScratch(side))
                .onFinish(ctx -> {
                    // Exit routing: a crossing attack hands straight into either another pass or a roll
                    // out, never into standing still and waiting to be hit.
                    if (exposureTicks > 60 && cooldowns.isReady("rogue_roll")) {
                        chain(this.roll);
                    }
                })
                .build();
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
            this.ambushLane = null;
            // Stealth is gated — if the cooldown hasn't expired the rogue stalks visibly
            if (cooldowns.isReady("stealth")) {
                cooldowns.trigger("stealth", STEALTH_CD);
                entity.addStatusEffect(new StatusEffectInstance(StatusEffects.INVISIBILITY, 320, 0));
            }
        } else if (state == CombatState.AMBUSHING) {
            entity.removeStatusEffect(StatusEffects.INVISIBILITY);
            cooldowns.trigger("ambush", AMBUSH_CD);
            exposureTicks = 0;
            this.ambushLane = chooseAmbushLane();
        } else if (state == CombatState.WITHDRAWING) {
            entity.addStatusEffect(new StatusEffectInstance(StatusEffects.SPEED, 60, 1));
            cooldowns.trigger("ambush_reset", AMBUSH_RESET_BUFFER);
            this.ambushLane = null;
            // Leave on a roll when one is available — a disengage that starts with a burst is much harder
            // to punish than one that starts with a jog.
            if (cooldowns.isReady("rogue_roll") && entity.getTarget() != null && !actions.isBusy()) {
                startRollAway();
            }
        }
    }

    @Override
    protected void executeCurrentCombatState() {
        var target = entity.getTarget();
        if (target == null) return;

        if (--orbitFlipTimer <= 0) {
            // A deliberate direction change every few seconds, not per-tick jitter. Random strafing reads
            // as indecision; a committed reversal reads as repositioning.
            orbitFlipTimer = 60 + entity.getRandom().nextInt(60);
            if (entity.getRandom().nextFloat() < 0.5f) {
                orbitDirection = -orbitDirection;
            }
        }

        if (actionOwnsMovement()) {
            return;
        }
        if (tryChain()) {
            return;
        }

        if (currentCombatState == CombatState.STALKING) {
            tickStalking(target);
        } else if (currentCombatState == CombatState.AMBUSHING) {
            tickAmbushing(target);
        } else {
            tickWithdrawing(target);
        }
    }

    // ── STALKING ──────────────────────────────────────────────────────────────

    private void tickStalking(LivingEntity target) {
        var profile = MovementProfile.AGILE_ORBIT;
        locomotion.faceTarget(target, profile.turnDegrees());

        float dist = stimulus.targetDistance;
        if (dist > ORBIT_RADIUS + 6 || !stimulus.lineOfSightToTarget) {
            locomotion.navigateTo(target.getX(), target.getY(), target.getZ(), 1.0);
        } else {
            locomotion.setFallbackGoal(target.getPos(), 1.0);
            locomotion.orbit(target, ORBIT_RADIUS, orbitDirection, profile);
        }

        // Throwing does not interrupt the orbit — the action is MOBILE, so steering continues underneath.
        if (!actions.isBusy() && cooldowns.isReady("rogue_throw")
                && stimulus.lineOfSightToTarget && dist > 4 && dist < 20) {
            startAction(this.knifeThrow);
        }
    }

    // ── AMBUSHING ─────────────────────────────────────────────────────────────

    private void tickAmbushing(LivingEntity target) {
        exposureTicks++;
        var profile = MovementProfile.AGILE_BURST;

        float dist = stimulus.targetDistance;

        // Burst toward the chosen lane rather than straight at the target: the lane is what makes the
        // approach come from somewhere the player has to have been watching.
        var lane = this.ambushLane;
        if (lane != null && dist > MELEE_THRESHOLD) {
            var toLane = GroundLocomotion.horizontal(lane.subtract(entity.getPos()));
            if (toLane.length() < 1.0) {
                this.ambushLane = null;
            } else {
                locomotion.faceTarget(target, profile.turnDegrees());
                locomotion.setFallbackGoal(lane, 1.6);
                locomotion.seekPosition(lane, profile);
                tryLaneAttack(target, dist);
                return;
            }
        }

        locomotion.faceTarget(target, profile.turnDegrees());
        if (dist > MELEE_THRESHOLD) {
            locomotion.setFallbackGoal(target.getPos(), 1.6);
            locomotion.pressure(target, 2.0, profile);
        } else {
            locomotion.crossPast(target, ambushSide, MovementProfile.AGILE_ORBIT);
        }
        tryLaneAttack(target, dist);
    }

    /** Attack selection while ambushing: cross through from whichever side the lane approached on. */
    private void tryLaneAttack(LivingEntity target, float distance) {
        if (actions.isBusy()) {
            return;
        }
        if (distance <= MELEE_ARC_RANGE + 0.6f) {
            startAction(ambushSide >= 0 ? this.crossSlashRight : this.crossSlashLeft);
        } else if (distance > 5.0f && cooldowns.isReady("rogue_roll") && exposureTicks > 20) {
            // Closing the last stretch of an ambush with a roll keeps the approach unreadable.
            startRollToward(target);
        }
    }

    // ── WITHDRAWING ───────────────────────────────────────────────────────────

    private void tickWithdrawing(LivingEntity target) {
        var profile = MovementProfile.AGILE_ORBIT;
        locomotion.faceTarget(target, profile.turnDegrees());

        // The retreat vector is deliberately diagonal — see GroundLocomotion#retreatVector. Running
        // straight backwards from a player is the easiest thing in the game to chase down.
        var exit = entity.getPos().add(locomotion.retreatVector(target).multiply(10.0));
        locomotion.setFallbackGoal(exit, 1.3);
        locomotion.retreatFrom(target, 14.0, profile.withMaxSpeed(profile.maxSpeed() * 1.25));

        if (!actions.isBusy() && cooldowns.isReady("rogue_throw")
                && stimulus.lineOfSightToTarget && stimulus.targetDistance > 5) {
            startAction(this.knifeThrow);
        }
    }

    // ── Abilities ─────────────────────────────────────────────────────────────

    private void throwKnife() {
        var target = entity.getTarget();
        if (target == null || entity.getWorld().isClient()) return;
        var id = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "knifethrow");
        entity.lookAt(EntityAnchorArgumentType.EntityAnchor.EYES, target.getEyePos());
        SoundHelper.playSound(entity.getWorld(), entity,
                new Sound(Identifier.of("minecraft:entity.player.attack.sweep")));
        ProjectileLauncher.shootProjectile(entity.getWorld(), entity, target,
                SpellRegistry.from(entity.getWorld()).getEntry(id).get(),
                new SpellExecution.ImpactContext()
                        .power(SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_MELEE, entity))
                        .position(entity.getPos()));
        ParticleHelper.sendBatches(entity, SpellRegistry.from(entity.getWorld()).get(id).release.visuals.particles);
    }

    /** Roll toward the target — an approach, not an escape. */
    private void startRollToward(LivingEntity target) {
        if (!startAction(this.roll)) return;
        var heading = GroundLocomotion.horizontal(target.getPos().subtract(entity.getPos()));
        locomotion.snapFacing(heading);
    }

    /** Roll out of the engagement along the angled retreat vector. */
    private void startRollAway() {
        var target = entity.getTarget();
        if (target == null || !startAction(this.roll)) return;
        locomotion.snapFacing(locomotion.retreatVector(target));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isApproachWindowOpen(AIStimulus s) {
        return s.timeSinceLastPlayerAttack > 16
            || (s.playerJustLanded && s.targetDistance < 16)
            || s.playerHealthPct < 0.4f;
    }

    /**
     * Pick an attack lane relative to the <em>target's own facing</em>: left shoulder, right shoulder,
     * rear-left or rear-right. Sprinting straight at a player is the one approach they are always already
     * looking at; the rear lanes are weighted higher precisely because they are not.
     *
     * <p>Lanes that are not standable are rejected, so this never commits to walking into a wall.
     */
    private Vec3d chooseAmbushLane() {
        var target = entity.getTarget();
        if (target == null) return null;

        var look = GroundLocomotion.horizontal(target.getRotationVector());
        if (look.lengthSquared() < 1.0e-4) {
            look = new Vec3d(0, 0, 1);
        }
        look = look.normalize();

        // Offsets in degrees from the target's facing, paired with the side the exit should cross to.
        float[] offsets = { 62f, -62f, 132f, -132f };
        int[] weights = { 2, 2, 3, 3 };

        Vec3d best = null;
        int bestScore = -1;
        for (int i = 0; i < offsets.length; i++) {
            var direction = look.rotateY((float) Math.toRadians(offsets[i]));
            var candidate = target.getPos().add(direction.multiply(LANE_RADIUS));
            if (!isStandable(candidate)) {
                continue;
            }
            // Prefer lanes it is already closer to, so the burst continues its motion rather than
            // reversing across the arena.
            var proximity = (int) Math.max(0, 12 - entity.getPos().distanceTo(candidate));
            var score = weights[i] * 3 + proximity + entity.getRandom().nextInt(4);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
                this.ambushSide = offsets[i] > 0 ? 1 : -1;
            }
        }
        return best;
    }

    private boolean isStandable(Vec3d position) {
        var pos = BlockPos.ofFloored(position);
        var world = entity.getWorld();
        return world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty();
    }

    /**
     * Called from {@link TricksterEntity#damage} — a reactive evade.
     *
     * <p>It picks a <em>lateral</em> vector with clearance rather than rolling forward: the read is
     * "it got out of the way", which a forward roll never communicates. Whatever it was doing is
     * cancelled, because a dodge that waits its turn is not a dodge.
     */
    public boolean tryDodge() {
        if (!cooldowns.isReady("dodge") || entity.getTarget() == null) {
            return false;
        }
        int cd = Math.max(80, 160 - (int)(160 * entity.getCooldownCoeff()));
        cooldowns.trigger("dodge", cd);

        var target = entity.getTarget();
        var toTarget = GroundLocomotion.horizontal(target.getPos().subtract(entity.getPos()));
        if (toTarget.lengthSquared() < 1.0e-4) {
            toTarget = locomotion.facing();
        }
        var forward = toTarget.normalize();
        var right = new Vec3d(-forward.z, 0.0, forward.x);

        // Prefer the side with clearance; fall back to the angled retreat if both are blocked.
        int side = locomotion.slipDirection(target);
        Vec3d escape = right.multiply(side);
        if (!isStandable(entity.getPos().add(escape.multiply(2.5)))) {
            escape = right.multiply(-side);
            side = -side;
            if (!isStandable(entity.getPos().add(escape.multiply(2.5)))) {
                escape = locomotion.retreatVector(target);
            }
        }

        actions.cancel();
        entity.dispatcher.play("dash",
                MinibossAnimationProvider.MinibossAnimationDispatcher.TRICKSTER
                        + (side >= 0 ? "dodge_right" : "dodge_left"), false, 1.0f);
        locomotion.beginCommittedMotion(escape, 0.46, 9, 0.0, 0.0);
        entity.playSound(net.minecraft.sound.SoundEvents.ENTITY_PLAYER_ATTACK_SWEEP, 0.7f, 1.5f);
        return true;
    }

    @Override
    public void onTargetLost() {
        entity.removeStatusEffect(StatusEffects.INVISIBILITY);
        this.ambushLane = null;
        super.onTargetLost();
    }

}

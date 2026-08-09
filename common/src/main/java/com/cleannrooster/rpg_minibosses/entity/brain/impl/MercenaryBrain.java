package com.cleannrooster.rpg_minibosses.entity.brain.impl;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossAnimationProvider;
import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.entity.TrapCleann;
import com.cleannrooster.rpg_minibosses.entity.brain.*;
import com.cleannrooster.rpg_minibosses.entity.combat.AttackMotion;
import com.cleannrooster.rpg_minibosses.entity.combat.CombatAction;
import com.cleannrooster.rpg_minibosses.entity.combat.GroundLocomotion;
import com.cleannrooster.rpg_minibosses.entity.combat.MovementProfile;
import com.cleannrooster.rpg_minibosses.entity.combat.TrackingMode;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.spell_engine.api.spell.ExternalSpellSchools;
import net.spell_engine.internals.SpellHelper;
import net.spell_power.api.SpellPower;

import java.util.List;

/**
 * AI brain for the Mercenary / Artillerist.
 *
 * <p><b>Identity: disciplined relocation and firing positions.</b> It is not a strafing turret and it is
 * not a chaser. Its rhythm is <em>relocate hard, plant exactly, fire deliberately</em> — the urgency comes
 * from how decisively it moves between firing positions, never from shortening the aim.
 *
 * <p>Preserved: PROBING / OPPORTUNIST stances, PROBING / PUNISHING states, the heavy shot, the four-shot
 * burst, traps, and relocation. What changed: relocation now runs to a <em>chosen destination</em> with
 * line of sight and a meaningfully different angle rather than for an arbitrary eighty ticks, trap
 * placement actually consumes the target-velocity prediction it computes, and every shot is a phase-driven
 * action so nothing fires while the body is still moving.
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

    /** The firing band it wants to hold. */
    private static final double OPTIMAL_NEAR = 8.0;
    private static final double OPTIMAL_FAR  = 13.0;

    private static final int PUNISH_BURST_DURATION = 40;
    private static final int TRAP_PLACE_CD         = 320;
    private static final int REPOSITION_CD         = 360;
    /** A relocation that has not arrived by now has gone wrong; recompute or fall back. */
    private static final int RELOCATE_TIMEOUT      = 110;
    /** Minimum angular change, in degrees, that makes a new firing position worth running to. */
    private static final double MIN_ANGLE_CHANGE   = 45.0;

    private final ArtilleristEntity mercenary;
    /** Nearby trap count updated each tick in executeCurrentCombatState. */
    private int nearbyTrapCount = 0;

    // Relocation state.
    /**
     * Set when something has invalidated the current firing position — a finished burst, a punish window
     * opening. Read and cleared on the next hold-band evaluation. A plain flag rather than a one-tick
     * cooldown, because cooldowns are decremented at the top of the brain tick and a one-tick entry is
     * already expired by the time anything reads it.
     */
    private boolean wantsRepositionReview;
    private Vec3d relocateGoal;
    private int relocateTicks;
    private boolean relocateRecomputed;

    private final CombatAction aimedShot;
    private final CombatAction burst;
    private final CombatAction snapShot;
    private final CombatAction trapThrow;

    public MercenaryBrain(ArtilleristEntity entity) {
        super(entity, Stance.PROBING, CombatState.PROBING);
        this.mercenary = entity;

        var family = MinibossAnimationProvider.MinibossAnimationDispatcher.MERC;

        // Heavy aimed shot. Fully planted: the locomotion arrests, the body settles into a stable stance,
        // and only then does it fire. The long hold is the telegraph and it is never trimmed.
        this.aimedShot = CombatAction.builder("merc_aimed_shot")
                .animation(family + "aim_heavy")
                .timing(17, 3, 8)
                .motion(AttackMotion.PLANTED)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(92)
                .onStart(ctx -> ctx.locomotion().arrest(MovementProfile.HARD_STOP))
                .onActiveStart(ctx -> mercenary.fireBolt(ctx.liveTarget()))
                .build();

        // The four-shot burst, with the shots on the same beats the recoil animation kicks on.
        this.burst = CombatAction.builder("merc_burst")
                .animation(family + "burst")
                .timing(8, 24, 12)
                .motion(AttackMotion.PLANTED)
                .tracking(TrackingMode.REDUCED, TrackingMode.LOCKED)
                .cooldown(300)
                .onStart(ctx -> ctx.locomotion().arrest(MovementProfile.HARD_STOP))
                .onActiveTick(ctx -> {
                    if ((ctx.phaseTick() - 3) % 6 == 0 && ctx.phaseTick() <= 21) {
                        mercenary.fireBolt(ctx.liveTarget());
                    }
                })
                .onFinish(ctx -> {
                    // Finishing a burst is the natural moment to ask whether this position is still good.
                    this.wantsRepositionReview = true;
                })
                .build();

        // New: a cheap suppressing shot fired from the hip while moving into position. It exists so the
        // transit between firing positions is not free for the player, and it is the only shot in the kit
        // that does not require the mob to be planted.
        this.snapShot = CombatAction.builder("merc_snap_shot")
                .animation(family + "snap_shot")
                .timing(4, 2, 4)
                .motion(AttackMotion.MOBILE)
                .tracking(TrackingMode.FULL, TrackingMode.REDUCED)
                .cooldown(140)
                .onActiveStart(ctx -> mercenary.fireBolt(ctx.liveTarget()))
                .build();

        // Traps go out mid-sprint. Only the off hand and torso animate, so the tactical sprint underneath
        // keeps reading and the relocation is not interrupted for a throw.
        this.trapThrow = CombatAction.builder("merc_trap_throw")
                .animation(family + "trap_throw")
                .timing(3, 3, 5)
                .motion(AttackMotion.MOBILE)
                .tracking(TrackingMode.FULL, TrackingMode.FULL)
                .cooldown(TRAP_PLACE_CD)
                .onActiveStart(ctx -> placePredictiveTraps())
                .build();
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
            // Punishing does not turn it into a melee chaser. It just stops hesitating: quicker relocation
            // decisions and a stronger willingness to close into the near edge of the firing band.
            this.wantsRepositionReview = true;
        }
    }

    @Override
    protected void executeCurrentCombatState() {
        var target = entity.getTarget();
        if (target == null) return;

        nearbyTrapCount = entity.getWorld()
            .getEntitiesByClass(TrapCleann.class,
                entity.getBoundingBox().expand(OPTIMAL_FAR + 4),
                e -> e.isAlive())
            .size();

        if (actionOwnsMovement()) {
            return; // a planted shot is running; the body stays exactly where it is
        }
        if (tryChain()) {
            return;
        }

        if (this.relocateGoal != null) {
            tickRelocating(target);
            return;
        }
        if (currentCombatState == CombatState.PROBING) {
            tickProbing(target);
        } else {
            tickPunishing(target);
        }
    }

    // ── Holding a firing position ─────────────────────────────────────────────

    private void tickProbing(LivingEntity target) {
        holdBand(target, MovementProfile.DISCIPLINED);
        fireIfPlanted(target, false);
    }

    private void tickPunishing(LivingEntity target) {
        holdBand(target, MovementProfile.DISCIPLINED.urgent(1.15, 1.2, 1.1f));
        fireIfPlanted(target, true);
    }

    /**
     * Hold the band. Small corrections only — the answer to a bad position is a relocation, not a slow
     * shuffle, so the shuffle is deliberately weak.
     */
    private void holdBand(LivingEntity target, MovementProfile profile) {
        locomotion.faceTarget(target, profile.turnDegrees());
        double distance = stimulus.targetDistance;

        boolean wantsNewPosition = distance < OPTIMAL_NEAR - 1.0
                || distance > OPTIMAL_FAR + 4.0
                || !stimulus.lineOfSightToTarget
                || this.wantsRepositionReview;

        if (wantsNewPosition && cooldowns.isReady("reposition")) {
            this.wantsRepositionReview = false;
            beginRelocation(target);
            return;
        }
        this.wantsRepositionReview = false;

        if (distance > OPTIMAL_FAR) {
            locomotion.setFallbackGoal(target.getPos(), 1.2);
            locomotion.pressure(target, OPTIMAL_FAR, profile);
        } else if (distance < OPTIMAL_NEAR) {
            locomotion.retreatFrom(target, OPTIMAL_NEAR, profile);
        } else {
            // In the band: stop. A shooter that is always drifting never looks like it is aiming.
            locomotion.arrest(profile);
        }
    }

    private void fireIfPlanted(LivingEntity target, boolean punishing) {
        if (actions.isBusy() || !stimulus.lineOfSightToTarget) {
            return;
        }
        // Planted shots require the body to actually be still, not merely to have been told to stop.
        if (locomotion.speed() > 0.04) {
            return;
        }
        double distance = stimulus.targetDistance;
        if (cooldowns.isReady("merc_burst") && (punishing || distance > OPTIMAL_NEAR)) {
            startAction(this.burst, punishing ? 0.75 : 1.0);
        } else if (cooldowns.isReady("merc_aimed_shot")) {
            startAction(this.aimedShot, punishing ? 0.7 : 1.0);
        }
    }

    // ── Relocation ────────────────────────────────────────────────────────────

    /**
     * Choose a firing position and commit to running there.
     *
     * <p>A candidate has to earn it: inside the preferred band, standable, with line of sight to the
     * target, at a meaningfully different bearing from where the mob is now, and clear of the traps it has
     * already laid. Running somewhere that fails those tests is what made the old loose eighty-tick sprint
     * read as panic rather than as tactics.
     */
    private void beginRelocation(LivingEntity target) {
        var destination = chooseFiringPosition(target);
        if (destination == null) {
            // Nothing better available: hold, and try again shortly rather than thrashing.
            cooldowns.trigger("reposition", 40);
            return;
        }
        cooldowns.trigger("reposition",
                REPOSITION_CD - (int) (REPOSITION_CD * entity.getCooldownCoeff() * 0.4));
        this.relocateGoal = destination;
        this.relocateTicks = 0;
        this.relocateRecomputed = false;
        actions.cancel();
        mercenary.startRunning = true;
        mercenary.getDataTracker().set(ArtilleristEntity.RUNNING, true);
    }

    private void tickRelocating(LivingEntity target) {
        var profile = MovementProfile.TACTICAL_SPRINT;
        this.relocateTicks++;

        var goal = this.relocateGoal;
        var remaining = GroundLocomotion.horizontal(goal.subtract(entity.getPos())).length();

        if (remaining < 0.9) {
            arriveAtPosition(target);
            return;
        }
        if (this.relocateTicks > RELOCATE_TIMEOUT) {
            if (!this.relocateRecomputed) {
                // One recompute, then give up and shoot from wherever it managed to get to. Prefer
                // finishing the movement to looping on an unreachable point.
                this.relocateRecomputed = true;
                this.relocateTicks = 0;
                var retry = chooseFiringPosition(target);
                if (retry != null) {
                    this.relocateGoal = retry;
                    return;
                }
            }
            arriveAtPosition(target);
            return;
        }

        // Face where it is going, not the target: the crossbow comes back up on arrival.
        locomotion.facePosition(goal.x, goal.z, profile.turnDegrees());
        locomotion.setFallbackGoal(goal, 1.5);
        locomotion.seekPosition(goal, profile);

        if (actions.isBusy()) {
            return;
        }
        // Traps go out in transit, and a suppressing shot keeps the transit from being free.
        if (cooldowns.isReady("merc_trap_throw") && entity.canSee(target)) {
            startAction(this.trapThrow);
        } else if (cooldowns.isReady("merc_snap_shot") && stimulus.lineOfSightToTarget
                && this.relocateTicks > 12) {
            startAction(this.snapShot);
        }
    }

    /** Arrive: hard arrest, plant, and start firing on the next decision. */
    private void arriveAtPosition(LivingEntity target) {
        this.relocateGoal = null;
        this.relocateTicks = 0;
        mercenary.startRunning = false;
        mercenary.getDataTracker().set(ArtilleristEntity.RUNNING, false);
        actions.cancel();
        locomotion.cancelCommitted();
        locomotion.arrest(MovementProfile.HARD_STOP);
        entity.getNavigation().stop();
        locomotion.faceTarget(target, 40f);
        entity.dispatcher.play("dash",
                MinibossAnimationProvider.MinibossAnimationDispatcher.MERC + "plant", false, 1.0f);
        // Ready to shoot immediately — the run was the pause.
        cooldowns.clear("merc_aimed_shot");
    }

    /** Sample the ring around the target for a position worth running to. */
    private Vec3d chooseFiringPosition(LivingEntity target) {
        var world = entity.getWorld();
        var current = GroundLocomotion.horizontal(entity.getPos().subtract(target.getPos()));
        var currentAngle = Math.toDegrees(Math.atan2(current.z, current.x));

        Vec3d best = null;
        double bestScore = Double.NEGATIVE_INFINITY;
        for (int i = 0; i < 12; i++) {
            var angle = Math.toRadians(i * 30 + entity.getRandom().nextInt(12));
            var radius = OPTIMAL_NEAR + entity.getRandom().nextDouble() * (OPTIMAL_FAR - OPTIMAL_NEAR);
            var candidate = target.getPos().add(Math.cos(angle) * radius, 0, Math.sin(angle) * radius);
            candidate = groundAt(candidate);
            if (candidate == null) {
                continue;
            }

            var angleChange = Math.abs(net.minecraft.util.math.MathHelper.wrapDegrees(
                    (float) (Math.toDegrees(angle) - currentAngle)));
            if (angleChange < MIN_ANGLE_CHANGE) {
                continue;
            }
            if (!hasLineOfSight(candidate, target)) {
                continue;
            }

            var score = angleChange * 0.05;
            // Prefer higher ground — a shooter that owns the elevation is much harder to close on.
            score += (candidate.y - target.getY()) * 0.4;
            // And prefer not to cluster on top of its own minefield.
            var trapsNear = world.getEntitiesByClass(TrapCleann.class,
                    entity.getBoundingBox().offset(candidate.subtract(entity.getPos())).expand(3.5),
                    e -> e.isAlive()).size();
            score -= trapsNear * 1.5;
            score += entity.getRandom().nextDouble() * 0.6;

            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    /** Drop a candidate onto solid ground near the mob's own level, or reject it. */
    private Vec3d groundAt(Vec3d candidate) {
        var world = entity.getWorld();
        var origin = BlockPos.ofFloored(candidate);
        for (int dy = 3; dy >= -4; dy--) {
            var pos = origin.up(dy);
            var below = pos.down();
            if (!world.getBlockState(below).isSolidBlock(world, below)) {
                continue;
            }
            if (!world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()) {
                continue;
            }
            if (!world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()) {
                continue;
            }
            return new Vec3d(candidate.x, pos.getY(), candidate.z);
        }
        return null;
    }

    private boolean hasLineOfSight(Vec3d from, LivingEntity target) {
        var eye = from.add(0, entity.getStandingEyeHeight(), 0);
        var hit = entity.getWorld().raycast(new net.minecraft.world.RaycastContext(
                eye, target.getEyePos(),
                net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                net.minecraft.world.RaycastContext.FluidHandling.NONE, entity));
        return hit.getType() != net.minecraft.util.hit.HitResult.Type.BLOCK;
    }

    // ── Traps ─────────────────────────────────────────────────────────────────

    /**
     * Lay a fan of traps biased toward where the target is actually going.
     *
     * <p>The old code computed a predicted position and then threw the traps at fixed 60-degree offsets
     * from the mob's own facing, so the prediction changed nothing. Here the fan is centred on the
     * bearing to the predicted position, which is what makes them read as anticipating a run rather than
     * as decoration.
     */
    private void placePredictiveTraps() {
        var target = entity.getTarget();
        if (target == null || entity.getWorld().isClient()) return;

        var velocity = GroundLocomotion.horizontal(stimulus.targetVelocity);
        // A player moving at full sprint covers roughly 5-6 blocks in a second; leading by ~20 ticks puts
        // the fan where they will be if they keep going, and collapses onto their feet if they stop.
        var predicted = target.getPos().add(velocity.multiply(20));
        var toPredicted = GroundLocomotion.horizontal(predicted.subtract(entity.getPos()));
        if (toPredicted.lengthSquared() < 1.0e-4) {
            toPredicted = locomotion.facing();
        }
        var centre = toPredicted.normalize();
        // Fan wider when the target is moving fast: more ground to cover, less certainty about the line.
        var spread = Math.toRadians(24 + Math.min(46.0, velocity.length() * 190));

        for (int i = -1; i <= 1; i++) {
            var direction = centre.rotateY((float) (i * spread));
            var trap = new TrapCleann(
                RPGMinibossesEntities.TRAP, entity, entity.getWorld(),
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "explosion"),
                new SpellHelper.ImpactContext().power(
                    SpellPower.getSpellPower(ExternalSpellSchools.PHYSICAL_RANGED, entity)));
            trap.setPosition(entity.getEyePos());
            // Throw harder toward a farther prediction so the fan lands near it rather than at its feet.
            var throwSpeed = net.minecraft.util.math.MathHelper.clamp(toPredicted.length() * 0.022, 0.16, 0.42);
            trap.setVelocity(direction.multiply(throwSpeed).add(0, 0.12, 0));
            trap.setYaw(entity.getYaw());
            trap.prevYaw = entity.getYaw();
            entity.getWorld().spawnEntity(trap);
            entity.getWorld().playSound((PlayerEntity) null,
                trap.getX(), trap.getY(), trap.getZ(),
                SoundEvents.ENTITY_ARMOR_STAND_PLACE, SoundCategory.BLOCKS, 0.75F, 0.8F);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private boolean isPunishableWindow(AIStimulus s) {
        return this.relocateGoal == null && (s.timeSinceLastPlayerAttack > 15
            || s.playerJustLanded
            || s.playerIsCasting
            || s.playerHealthPct < 0.3f
            || (nearbyTrapCount > 0 && s.targetDistance < 5));
    }

    @Override
    public void onTargetLost() {
        this.relocateGoal = null;
        mercenary.startRunning = false;
        mercenary.getDataTracker().set(ArtilleristEntity.RUNNING, false);
        super.onTargetLost();
    }
}

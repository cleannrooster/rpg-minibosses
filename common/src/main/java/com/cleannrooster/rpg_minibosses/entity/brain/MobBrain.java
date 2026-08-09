package com.cleannrooster.rpg_minibosses.entity.brain;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.combat.ActionRunner;
import com.cleannrooster.rpg_minibosses.entity.combat.AttackPhase;
import com.cleannrooster.rpg_minibosses.entity.combat.CombatAction;
import com.cleannrooster.rpg_minibosses.entity.combat.CombatDebug;
import com.cleannrooster.rpg_minibosses.entity.combat.GroundLocomotion;
import com.cleannrooster.rpg_minibosses.entity.combat.SlashVisual;
import com.cleannrooster.rpg_minibosses.entity.combat.SwingExecution;
import com.cleannrooster.rpg_minibosses.entity.combat.TrackingMode;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackGeometry;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackVolume;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Abstract three-layer AI state machine base.
 *
 * <p>Layer cadences (server ticks):
 * <ul>
 *   <li>Phase transitions – every tick (health is critical)</li>
 *   <li>Stance evaluation – every 10 ticks (~2/sec)</li>
 *   <li>CombatState evaluation – every 2 ticks (~10/sec)</li>
 *   <li>Action dispatch – every tick</li>
 * </ul>
 */
public abstract class MobBrain {

    protected final MinibossEntity entity;
    public    final AIStimulus stimulus;
    public    final AbilityCooldownRegistry cooldowns = new AbilityCooldownRegistry();

    /**
     * Authored combat steering. Every brain drives this instead of talking to the navigator directly
     * inside the combat envelope; see {@link GroundLocomotion} for why.
     */
    public    final GroundLocomotion locomotion;
    /** Phase-driven attack execution. One action at a time, per mob. */
    public    final ActionRunner actions;

    protected BrainState currentStance;
    protected BrainState currentCombatState;

    /** Action queued to fire the instant the running one finishes — the chaining mechanism. */
    private @Nullable CombatAction chainNext;

    private int stanceEvalCountdown      = 0;
    private int combatStateEvalCountdown = 0;

    /** Tracks which PhaseTransitions have already fired to prevent re-entry. */
    private final Set<PhaseTransition> firedTransitions = new HashSet<>();

    protected MobBrain(MinibossEntity entity,
                       BrainState initialStance,
                       BrainState initialCombatState) {
        this.entity           = entity;
        this.currentStance    = initialStance;
        this.currentCombatState = initialCombatState;
        this.stimulus         = createStimulus();
        this.locomotion       = new GroundLocomotion(entity);
        this.actions          = new ActionRunner(entity, this.locomotion);
    }

    // ── Abstract hooks ────────────────────────────────────────────────────────

    /** Build and return the AIStimulus with per-mob distance/damage thresholds. */
    protected abstract AIStimulus createStimulus();

    /** Transitions that may change the current Stance. */
    protected abstract List<Transition> stanceTransitions();

    /** Transitions that may change the current CombatState. */
    protected abstract List<Transition> combatStateTransitions();

    /** Optional one-way phase transitions triggered by health thresholds. */
    protected abstract List<PhaseTransition> definePhaseTransitions();

    /** Called every tick: dispatch movement, attacks, and other actions based on currentCombatState. */
    protected abstract void executeCurrentCombatState();

    // ── Tick ─────────────────────────────────────────────────────────────────

    public void tick() {
        if (entity.getWorld().isClient()) return;

        cooldowns.tick();
        stimulus.refresh();
        checkPhaseTransitions();

        // Movement bookkeeping first, then the running action, then the brain's own decision. This order
        // matters: an attack's contribution has to land in the same tick the brain steers, or the two
        // systems end up describing different ticks of the same movement.
        locomotion.beginTick();
        actions.tick();
        // The legacy `performing` flag is now simply a view of the runner, so no ability can leave it
        // stuck true after its scheduled callbacks are dropped.
        entity.performing = actions.isBusy();
        applyActionTracking();

        if (--stanceEvalCountdown <= 0) {
            stanceEvalCountdown = 10;
            evaluateLayer(stanceTransitions(), true);
        }
        if (--combatStateEvalCountdown <= 0) {
            combatStateEvalCountdown = 2;
            evaluateLayer(combatStateTransitions(), false);
        }

        executeCurrentCombatState();

        locomotion.endTick();
        CombatDebug.verify(entity, locomotion, actions);
    }

    /**
     * Honour the running action's turn cap. Tracking is a property of the attack, not of the AI — faster
     * movement must never buy a mob better tracking, or its telegraphs stop meaning anything.
     */
    private void applyActionTracking() {
        if (!actions.isBusy()) {
            return;
        }
        var target = entity.getTarget();
        if (target == null) {
            return;
        }
        var tracking = actions.tracking();
        if (tracking != TrackingMode.LOCKED) {
            locomotion.faceTarget(target, tracking.degreesPerTick());
        }
    }

    // ── Action helpers ────────────────────────────────────────────────────────

    /**
     * Fire an action if its cooldown is clear and nothing else is running. The cooldown is keyed on the
     * action id and scaled by the mob's crowd coefficient, matching the existing ability pacing.
     */
    protected boolean startAction(CombatAction action) {
        return startAction(action, 1.0);
    }

    protected boolean startAction(CombatAction action, double cooldownScale) {
        if (actions.isBusy() || !cooldowns.isReady(action.id())) {
            return false;
        }
        CombatDebug.verifyAction(action);
        if (!actions.start(action, entity.getTarget())) {
            return false;
        }
        var base = (int) (action.cooldownTicks() * cooldownScale);
        cooldowns.trigger(action.id(), Math.max(1, base - (int) (base * entity.getCooldownCoeff() * 0.4)));
        onActionStarted(action);
        return true;
    }

    /** Fire an action ignoring its cooldown — used by chains, which have already paid for the window. */
    protected boolean forceAction(CombatAction action) {
        if (actions.isBusy()) {
            return false;
        }
        CombatDebug.verifyAction(action);
        if (!actions.start(action, entity.getTarget())) {
            return false;
        }
        cooldowns.trigger(action.id(), Math.max(1, action.cooldownTicks()));
        onActionStarted(action);
        return true;
    }

    /** Queue a follow-up for the moment the running action finishes. Cleared as soon as it is consumed. */
    protected void chain(@Nullable CombatAction action) {
        this.chainNext = action;
    }

    /**
     * Consume a queued chain. Called by brains at the top of their idle beat, which is the only moment a
     * chain may fire — so chains remove AI dead time without removing the punish window that follows an
     * attack's recovery.
     */
    protected boolean tryChain() {
        // A chain is only consumed when it can actually fire. Popping it while the previous action is
        // still running would silently drop the follow-up, which is the failure mode that turns a chain
        // into an intermittent one.
        if (this.chainNext == null || actions.isBusy()) {
            return false;
        }
        var next = this.chainNext;
        this.chainNext = null;
        return forceAction(next);
    }

    protected boolean hasChain() {
        return this.chainNext != null;
    }

    /** True while an action is committed — the brain must not issue its own movement intent. */
    protected boolean actionOwnsMovement() {
        return actions.ownsMovement();
    }

    /** True during the recovery of the running action: the window a brain may start biasing back. */
    protected boolean inRecovery() {
        return actions.phase() == AttackPhase.RECOVERY;
    }

    /** Hook for brains that want to react to their own commitments (indicator resets, sounds). */
    protected void onActionStarted(CombatAction action) {
        entity.performing = true;
    }

    /** Called from {@link MinibossEntity} when something shoves the mob. */
    public void onKnockback() {
        locomotion.notifyExternalImpulse(4);
    }

    // ── Internal evaluation ───────────────────────────────────────────────────

    private void evaluateLayer(List<Transition> transitions, boolean isStance) {
        BrainState current = isStance ? currentStance : currentCombatState;
        transitions.stream()
            .filter(t -> t.from() == null || t.from().equals(current))
            .filter(t -> !isLockedOut(t.to()))
            .filter(t -> t.condition().test(stimulus))
            .max(java.util.Comparator.comparingInt(Transition::priority))
            .ifPresent(t -> {
                if (isStance) setStance(t.to());
                else          setCombatState(t.to());
            });
    }

    private boolean isLockedOut(BrainState state) {
        for (PhaseTransition pt : firedTransitions) {
            if (pt.lockedOutStates().contains(state)) return true;
        }
        return false;
    }

    private void checkPhaseTransitions() {
        float hpPct = entity.getHealth() / Math.max(1f, entity.getMaxHealth());
        for (PhaseTransition pt : definePhaseTransitions()) {
            if (!firedTransitions.contains(pt) && pt.isTriggered(hpPct)) {
                firedTransitions.add(pt);
                setStance(pt.forcedStance());
                if (pt.onEnter() != null) pt.onEnter().run();
                break; // one phase transition per tick
            }
        }
    }

    // ── State transitions ─────────────────────────────────────────────────────

    protected void setStance(BrainState newStance) {
        if (!newStance.equals(currentStance)) {
            onStanceExit(currentStance);
            currentStance = newStance;
            onStanceEnter(newStance);
        }
    }

    protected void setCombatState(BrainState newState) {
        if (!newState.equals(currentCombatState)) {
            onCombatStateExit(currentCombatState);
            currentCombatState = newState;
            onCombatStateEnter(newState);
        }
    }

    // ── Optional callbacks ────────────────────────────────────────────────────

    protected void onStanceEnter(BrainState stance) {}
    protected void onStanceExit(BrainState stance) {}
    protected void onCombatStateEnter(BrainState state) {}
    protected void onCombatStateExit(BrainState state) {}

    /** Called when the mob loses its target (MobBrainGoal.stop()). */
    public void onTargetLost() {
        // Losing a target must not strand a committed attack — cancel() always releases the body — and
        // ownership has to go all the way back to NAVIGATION, or the mob would keep its combat facing
        // pinned and never turn to walk anywhere again.
        actions.cancel();
        locomotion.cancelCommitted();
        locomotion.releaseToNavigation();
        chain(null);
        entity.performing = false;
    }

    /** Called from MinibossEntity.damage() before super — records incoming damage. */
    public void onDamageTaken(float amount) {
        stimulus.notifyDamage(amount);
    }

    // ── Accessors ─────────────────────────────────────────────────────────────

    public BrainState getCurrentStance()      { return currentStance; }
    public BrainState getCurrentCombatState() { return currentCombatState; }

    // ── Shared melee helpers ──────────────────────────────────────────────────

    /**
     * Deals weapon damage to all entities within an arc around the entity's facing direction,
     * excluding the primary target (who received damage through tryAttack).
     * @param primaryTarget entity already hit by tryAttack — excluded from arc hits
     * @param halfAngleDeg  half-width of the arc in degrees (e.g. 60 = 120° total sweep)
     * @param range         reach of the arc in blocks
     */
    protected void performArcDamage(LivingEntity primaryTarget, float halfAngleDeg, float range) {
        performArcDamage(primaryTarget, halfAngleDeg, range, entity.getRotationVector(), 1.0f);
    }

    /**
     * As above, but with the arc's centre stated explicitly and a damage multiplier.
     *
     * <p>Stating the direction is what lets a swing's damage agree with the swing's <em>animation</em>
     * rather than with wherever the body happens to be pointing on the frame damage resolves. Committed
     * attacks pass the heading they captured at commit time.
     */
    protected void performArcDamage(LivingEntity primaryTarget, float halfAngleDeg, float range,
                                    Vec3d forward, float damageScale) {
        if (entity.getWorld().isClient()) return;
        float baseDamage = (float) entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * damageScale;
        double cosHalfAngle = Math.cos(Math.toRadians(halfAngleDeg));
        Vec3d centre = forward.horizontalLengthSquared() < 1.0e-6
                ? entity.getRotationVector()
                : new Vec3d(forward.x, 0.0, forward.z).normalize();
        entity.getWorld().getEntitiesByType(
            TypeFilter.instanceOf(LivingEntity.class),
            entity.getBoundingBox().expand(range),
            t -> t != entity
                && t != primaryTarget
                && entity.canHarm(t)
                && entity.distanceTo(t) <= range
                && centre.dotProduct(t.getPos().subtract(entity.getPos()).normalize()) >= cosHalfAngle
        ).forEach(arcTarget -> arcTarget.damage(entity.getDamageSources().mobAttack(entity), baseDamage));
    }

    /**
     * Resolve a volume once, immediately, outside any swept attack window.
     *
     * <p>For pulses and shockwaves — the beats of a spin, an impact ring — where the shape is momentary
     * rather than travelling. It captures a frame from the mob's current facing, resolves the whole span in
     * one go, and draws it from that same surface, so a pulse is as honest about its reach as a swing is.
     */
    protected void strikeVolume(AttackVolume volume, float damageScale, double knockback,
                                @Nullable SlashVisual slash) {
        if (!(entity.getWorld() instanceof ServerWorld world)) return;
        var frame = AttackGeometry.capture(volume, entity, false, 0.0f);
        var bounds = AttackGeometry.bounds(volume, frame, null);
        float base = (float) entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE) * damageScale;
        for (var victim : world.getEntitiesByClass(LivingEntity.class, bounds,
                entity::canHarm)) {
            if (!AttackGeometry.hits(volume, frame, 0.0f, 1.0f, victim.getBoundingBox(), null)) {
                continue;
            }
            if (victim.damage(entity.getDamageSources().mobAttack(entity), base) && knockback > 0.0) {
                var away = victim.getPos().subtract(frame.origin());
                if (away.horizontalLengthSquared() > 1.0e-6) {
                    away = new Vec3d(away.x, 0.0, away.z).normalize();
                    victim.takeKnockback(knockback, -away.x, -away.z);
                }
            }
        }
        if (slash == null || slash.perTick() <= 0) return;
        var random = entity.getRandom();
        for (int i = 0; i < slash.perTick(); i++) {
            var sample = AttackGeometry.sample(volume, frame, 0.0f, 1.0f, random,
                    slash.speed(), slash.scatter());
            world.spawnParticles(slash.particle(),
                    sample.position().x, sample.position().y, sample.position().z, 0,
                    sample.velocity().x, sample.velocity().y, sample.velocity().z, 1.0);
        }
    }

    /**
     * One melee contact: the primary target through {@code tryAttack} (so vanilla knockback, enchantments
     * and attack events all fire) plus everyone else inside the arc.
     *
     * @param forward centre of the arc — normally the heading the swing committed to
     */
    protected void meleeStrike(LivingEntity primary, float halfAngleDeg, float range, Vec3d forward) {
        if (entity.getWorld().isClient()) return;
        if (primary != null && entity.distanceTo(primary) <= range + 0.75f) {
            entity.tryAttack(primary);
        }
        performArcDamage(primary, halfAngleDeg, range, forward, 1.0f);
    }
}

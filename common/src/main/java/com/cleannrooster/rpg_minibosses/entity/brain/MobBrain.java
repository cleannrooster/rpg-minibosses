package com.cleannrooster.rpg_minibosses.entity.brain;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.math.Vec3d;

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

    protected BrainState currentStance;
    protected BrainState currentCombatState;

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

        if (--stanceEvalCountdown <= 0) {
            stanceEvalCountdown = 10;
            evaluateLayer(stanceTransitions(), true);
        }
        if (--combatStateEvalCountdown <= 0) {
            combatStateEvalCountdown = 2;
            evaluateLayer(combatStateTransitions(), false);
        }

        executeCurrentCombatState();
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
    public void onTargetLost() {}

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
        if (entity.getWorld().isClient()) return;
        float baseDamage = (float) entity.getAttributeValue(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        double cosHalfAngle = Math.cos(Math.toRadians(halfAngleDeg));
        Vec3d forward = entity.getRotationVector();
        entity.getWorld().getEntitiesByType(
            TypeFilter.instanceOf(LivingEntity.class),
            entity.getBoundingBox().expand(range),
            t -> t != entity
                && t != primaryTarget
                && entity.distanceTo(t) <= range
                && forward.dotProduct(t.getPos().subtract(entity.getPos()).normalize()) >= cosHalfAngle
        ).forEach(arcTarget -> arcTarget.damage(entity.getDamageSources().mobAttack(entity), baseDamage));
    }
}

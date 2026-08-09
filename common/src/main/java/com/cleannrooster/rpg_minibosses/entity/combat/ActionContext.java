package com.cleannrooster.rpg_minibosses.entity.combat;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Everything a {@link CombatAction} hook needs, including the parts of the world that were true at the
 * moment the action <em>committed</em> rather than right now.
 *
 * <p>The captured heading and origin are the mechanism behind "the player can dodge after commitment": a
 * leap lands where it was aimed when it launched, not where the player has since run to.
 */
public final class ActionContext {

    private final MinibossEntity entity;
    private final GroundLocomotion locomotion;
    private final @Nullable LivingEntity target;
    /** Horizontal unit heading captured when the action started. */
    private final Vec3d committedForward;
    /** Position the action was launched from. */
    private final Vec3d committedOrigin;
    /** Where the target was when the action committed — the point a leap or dash is aimed at. */
    private final @Nullable Vec3d committedTargetPos;

    /** The running swing's geometry, once the attack has committed. Null during windup and for
     * abilities with no volume. */
    private @Nullable SwingExecution swing;

    private AttackPhase phase = AttackPhase.WINDUP;
    private int phaseTick;
    /** Free slot for an action that needs one number across phases (a chosen side, a lane index). */
    private int scratch;

    ActionContext(MinibossEntity entity, GroundLocomotion locomotion, @Nullable LivingEntity target,
                  Vec3d committedForward, Vec3d committedOrigin, @Nullable Vec3d committedTargetPos) {
        this.entity = entity;
        this.locomotion = locomotion;
        this.target = target;
        this.committedForward = committedForward;
        this.committedOrigin = committedOrigin;
        this.committedTargetPos = committedTargetPos;
    }

    public MinibossEntity entity() {
        return this.entity;
    }

    public GroundLocomotion locomotion() {
        return this.locomotion;
    }

    /** The target captured at commit. May be null, and may since have died — callers must check. */
    public @Nullable LivingEntity target() {
        return this.target != null && this.target.isAlive() ? this.target : null;
    }

    /** The live target, if the entity still has one. Use for tracking; use {@link #target()} for damage. */
    public @Nullable LivingEntity liveTarget() {
        var current = this.entity.getTarget();
        return current != null && current.isAlive() ? current : target();
    }

    public Vec3d committedForward() {
        return this.committedForward;
    }

    public Vec3d committedOrigin() {
        return this.committedOrigin;
    }

    public @Nullable Vec3d committedTargetPos() {
        return this.committedTargetPos;
    }

    public AttackPhase phase() {
        return this.phase;
    }

    /** Ticks elapsed inside the current phase, starting at 1 on the first tick of that phase. */
    public int phaseTick() {
        return this.phaseTick;
    }

    public int scratch() {
        return this.scratch;
    }

    public void setScratch(int value) {
        this.scratch = value;
    }

    /** The live swing, for hooks that want to read the frame the attack committed to. */
    public @Nullable SwingExecution swing() {
        return this.swing;
    }

    void setSwing(@Nullable SwingExecution swing) {
        this.swing = swing;
    }

    void setPhase(AttackPhase phase) {
        this.phase = phase;
    }

    void setPhaseTick(int tick) {
        this.phaseTick = tick;
    }
}

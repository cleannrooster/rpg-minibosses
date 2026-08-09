package com.cleannrooster.rpg_minibosses.entity.combat;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackGeometry;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Runs one {@link CombatAction} at a time for one mob.
 *
 * <p>The single-action rule is the thing that keeps these fights readable: two behaviours can never
 * overlap their damage windows, because {@link #start} is simply refused while something is running.
 *
 * <p>The runner also owns the contract that stops attacks from stranding a mob: whenever an execution
 * ends — normally, by cancellation, or because the mob lost its target — {@link GroundLocomotion#releaseAttackMotion()}
 * is called. There is no path out of an action that skips it.
 */
public final class ActionRunner {

    private final MinibossEntity entity;
    private final GroundLocomotion locomotion;

    private @Nullable CombatAction current;
    private @Nullable ActionContext context;
    private @Nullable SwingExecution swing;
    private AttackPhase phase = AttackPhase.FINISHED;
    private int phaseTick;
    private int recoveryBudget;
    /** True while the owner is in a phase that trims recovery tails. */
    private boolean urgent;

    public ActionRunner(MinibossEntity entity, GroundLocomotion locomotion) {
        this.entity = entity;
        this.locomotion = locomotion;
    }

    // --- state ------------------------------------------------------------------------------------

    public boolean isBusy() {
        return this.current != null;
    }

    public @Nullable CombatAction current() {
        return this.current;
    }

    public @Nullable ActionContext context() {
        return this.context;
    }

    public AttackPhase phase() {
        return this.current == null ? AttackPhase.FINISHED : this.phase;
    }

    /** Turn cap to honour this tick. {@link TrackingMode#FULL} when nothing is running. */
    public TrackingMode tracking() {
        return this.current == null ? TrackingMode.FULL : this.current.tracking(this.phase);
    }

    /** True while an action owns the body — the brain must not steer. */
    public boolean ownsMovement() {
        return this.current != null && this.current.motion().ownsMovement();
    }

    public void setUrgent(boolean urgent) {
        this.urgent = urgent;
    }

    // --- driving ----------------------------------------------------------------------------------

    /** Begin an action. Refused while another runs, which is what enforces one damage window at a time. */
    public boolean start(CombatAction action, @Nullable LivingEntity target) {
        if (this.current != null) {
            return false;
        }
        var forward = this.locomotion.facing();
        if (target != null) {
            var toTarget = GroundLocomotion.horizontal(target.getPos().subtract(this.entity.getPos()));
            if (toTarget.lengthSquared() > 1.0e-4) {
                forward = toTarget.normalize();
            }
        }

        this.current = action;
        this.phase = AttackPhase.WINDUP;
        this.phaseTick = 0;
        this.recoveryBudget = Math.max(0, action.recoveryTicks()
                - (this.urgent ? action.urgentRecoveryTrim() : 0));
        this.context = new ActionContext(this.entity, this.locomotion, target, forward,
                this.entity.getPos(), target == null ? null : target.getPos());
        this.context.setPhase(AttackPhase.WINDUP);

        if (action.motion().ownsMovement()) {
            this.locomotion.claimAttackMotion();
        }
        playAnimation(action);
        action.fireStart(this.context);

        // A zero-length windup commits on the same tick it started, so a definition can state 0/n/m and
        // mean it rather than silently gaining a tick of telegraph.
        if (action.windupTicks() <= 0) {
            enterActive();
        }
        return true;
    }

    /**
     * Advance one server tick. Call once per brain tick, after {@link GroundLocomotion#beginTick()} and
     * before the brain issues its own movement intent, so an attack's motion lands in the same tick.
     */
    public void tick() {
        var action = this.current;
        var ctx = this.context;
        if (action == null || ctx == null) {
            return;
        }

        // The body is claimed every tick, not once, because anything else in the goal stack that stops the
        // path or starts one must not be able to take it back mid-attack.
        if (action.motion().ownsMovement()) {
            this.locomotion.claimAttackMotion();
        }

        this.phaseTick++;
        ctx.setPhaseTick(this.phaseTick);

        switch (this.phase) {
            case WINDUP -> {
                applyWindupMotion(action, ctx);
                if (this.phaseTick >= action.windupTicks()) {
                    enterActive();
                }
            }
            case ACTIVE -> {
                applyActiveMotion(action, ctx);
                if (this.swing != null) {
                    this.swing.tick(this.phaseTick);
                }
                action.fireActiveTick(ctx);
                if (this.phaseTick >= action.activeTicks()) {
                    enterRecovery();
                }
            }
            case RECOVERY -> {
                if (this.phaseTick >= this.recoveryBudget) {
                    finish();
                }
            }
            case FINISHED -> finish();
        }
    }

    /** Abandon whatever is running. Always releases movement authority. */
    public void cancel() {
        if (this.current == null) {
            return;
        }
        this.current = null;
        this.context = null;
        this.swing = null;
        this.phase = AttackPhase.FINISHED;
        this.phaseTick = 0;
        this.locomotion.cancelCommitted();
        this.locomotion.releaseAttackMotion();
    }

    // --- phases -----------------------------------------------------------------------------------

    private void enterActive() {
        var action = this.current;
        var ctx = this.context;
        if (action == null || ctx == null) {
            return;
        }
        this.phase = AttackPhase.ACTIVE;
        this.phaseTick = 0;
        ctx.setPhase(AttackPhase.ACTIVE);
        ctx.setPhaseTick(0);

        // The frame is captured here, at the instant the attack becomes damaging, and then frozen. That
        // is what makes the arc resolve against where the mob was aiming when it committed rather than
        // wherever it has since turned — the whole basis of dodging it.
        var volume = action.volume();
        if (volume != null) {
            var frame = AttackGeometry.capture(volume, this.entity, false, 0.0f);
            this.swing = new SwingExecution(this.entity, action, volume, frame);
            ctx.setSwing(this.swing);
        }

        if (action.motion().isCommitted()) {
            var heading = committedHeading(action, ctx);
            this.locomotion.snapFacing(heading);
            this.locomotion.beginCommittedMotion(heading, action.committedSpeed(),
                    action.committedTicks(), action.committedLift(), action.committedCorrection());
            if (action.committedCorrection() > 0.0 && ctx.committedTargetPos() != null) {
                this.locomotion.setCommittedAim(ctx.committedTargetPos());
            }
        }
        action.fireActiveStart(ctx);
    }

    private void enterRecovery() {
        var action = this.current;
        var ctx = this.context;
        if (action == null || ctx == null) {
            return;
        }
        this.phase = AttackPhase.RECOVERY;
        this.phaseTick = 0;
        ctx.setPhase(AttackPhase.RECOVERY);
        ctx.setPhaseTick(0);
        // Recovery is where the body settles, so anything still being thrown stops being thrown.
        if (action.motion().isCommitted()) {
            this.locomotion.cancelCommitted();
        }
        action.fireRecoveryStart(ctx);
        if (this.recoveryBudget <= 0) {
            finish();
        }
    }

    private void finish() {
        var action = this.current;
        var ctx = this.context;
        this.current = null;
        this.context = null;
        this.swing = null;
        this.phase = AttackPhase.FINISHED;
        this.phaseTick = 0;
        // Unconditional: this is the only place an action can end, and it always hands the body back.
        this.locomotion.releaseAttackMotion();
        if (action != null && ctx != null) {
            ctx.setPhase(AttackPhase.FINISHED);
            action.fireFinish(ctx);
        }
    }

    // --- motion -----------------------------------------------------------------------------------

    private void applyWindupMotion(CombatAction action, ActionContext ctx) {
        if (!action.motion().ownsMovement()) {
            return;
        }
        if (action.motion().arrestsOnWindup()) {
            // Violent arrest into the telegraph. The burst lands against stillness.
            this.locomotion.applyAttackMotion(Vec3d.ZERO);
            return;
        }
        if (action.windupDrift() != 0.0 && action.windupTicks() > 0) {
            var perTick = action.windupDrift() / action.windupTicks();
            this.locomotion.applyAttackMotion(ctx.committedForward().multiply(perTick));
        } else {
            this.locomotion.applyAttackMotion(Vec3d.ZERO);
        }
    }

    private void applyActiveMotion(CombatAction action, ActionContext ctx) {
        if (!action.motion().ownsMovement() || action.motion().isCommitted()) {
            return; // committed motion is driven by the locomotion system itself
        }
        if (action.advance() != 0.0 && action.activeTicks() > 0) {
            var perTick = action.advance() / action.activeTicks();
            this.locomotion.applyAttackMotion(ctx.committedForward().multiply(perTick));
        } else {
            this.locomotion.applyAttackMotion(Vec3d.ZERO);
        }
    }

    /**
     * The heading a committed motion travels. Crossing attacks aim past the target's shoulder, retreats
     * aim away, and everything else uses the heading captured at commit — never the live facing vector,
     * so a player who sidesteps between commit and release is genuinely out of the way.
     */
    private Vec3d committedHeading(CombatAction action, ActionContext ctx) {
        var target = ctx.liveTarget();
        return switch (action.motion()) {
            case CROSS_THROUGH -> target == null
                    ? ctx.committedForward()
                    : this.locomotion.crossVector(target, ctx.scratch() >= 0 ? 1 : -1);
            case RETREATING -> target == null
                    ? ctx.committedForward().multiply(-1.0)
                    : this.locomotion.retreatVector(target);
            default -> ctx.committedForward();
        };
    }

    // --- animation --------------------------------------------------------------------------------

    private void playAnimation(CombatAction action) {
        var animation = action.animation();
        if (animation == null) {
            return;
        }
        // Dispatch is server-side; AzureLib mirrors it to every tracking client, so the clip and the
        // damage window start on the same tick on both sides.
        this.entity.dispatcher.play(action.animationController(), animation,
                action.animationLoops(), action.animationSpeed());
    }
}

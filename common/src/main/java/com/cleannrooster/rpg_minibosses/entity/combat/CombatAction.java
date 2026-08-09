package com.cleannrooster.rpg_minibosses.entity.combat;

import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackVolume;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A complete description of one attack or mobility action: its timing, which phase owns the body, how much
 * it may still turn, which animation clip plays, and what happens on each phase boundary.
 *
 * <p>Definitions are immutable and shared — everything that varies per use lives in the
 * {@link ActionContext} captured when the action commits. A brain therefore decides <em>which</em> action
 * and <em>when</em>, and never re-implements phase timing, movement ownership or animation dispatch.
 *
 * <p>An attack that connects with a weapon also states its {@link AttackVolume} — the swept surface it
 * damages through. Damage and the drawn arc are both resolved from that one surface by
 * {@link SwingExecution}, so the crescent a player sees is the volume that can hit them. Abilities that
 * resolve some other way (a Spell Engine impact, a projectile) simply leave the volume unset and do their
 * work in the phase hooks.
 */
public final class CombatAction {

    private final String id;
    private final String animationController;
    private final @Nullable String animation;
    private final boolean animationLoops;
    private final float animationSpeed;

    private final int windupTicks;
    private final int activeTicks;
    private final int recoveryTicks;

    private final AttackMotion motion;
    /** Blocks the body is carried forward across the active window (attack-owned translation). */
    private final double advance;
    /** Blocks of drift during windup — negative pulls back into a wind-up step. */
    private final double windupDrift;
    /** Speed of a committed motion, blocks/tick. Only read for committed motion classes. */
    private final double committedSpeed;
    /** Ticks a committed motion runs. Defaults to the active window. */
    private final int committedTicks;
    private final double committedLift;
    private final double committedCorrection;

    private final TrackingMode windupTracking;
    private final TrackingMode activeTracking;
    private final TrackingMode recoveryTracking;

    // --- geometry ---
    /** The swept volume this attack damages through, or null for abilities that resolve their own way. */
    private final @Nullable AttackVolume volume;
    private final boolean usesWeaponDamage;
    private final float damageScale;
    private final double knockback;
    private final double knockbackVertical;
    private final boolean multiHit;
    private final int multiHitCooldown;
    private final @Nullable SlashVisual slash;
    /** How the swept surface is drawn as a ribbon. Where it is drawn comes from the volume. */
    private final @Nullable SlashProfile ribbon;

    private final int cooldownTicks;
    /** Ticks trimmed from the tail of recovery when the owner is in an urgent phase. */
    private final int urgentRecoveryTrim;

    private final @Nullable Consumer<ActionContext> onStart;
    private final @Nullable Consumer<ActionContext> onActiveStart;
    private final @Nullable Consumer<ActionContext> onActiveTick;
    private final @Nullable Consumer<ActionContext> onRecoveryStart;
    private final @Nullable Consumer<ActionContext> onFinish;

    private CombatAction(Builder builder) {
        this.id = builder.id;
        this.animationController = builder.animationController;
        this.animation = builder.animation;
        this.animationLoops = builder.animationLoops;
        this.animationSpeed = builder.animationSpeed;
        this.windupTicks = builder.windupTicks;
        this.activeTicks = builder.activeTicks;
        this.recoveryTicks = builder.recoveryTicks;
        this.motion = builder.motion;
        this.advance = builder.advance;
        this.windupDrift = builder.windupDrift;
        this.committedSpeed = builder.committedSpeed;
        this.committedTicks = builder.committedTicks > 0 ? builder.committedTicks : builder.activeTicks;
        this.committedLift = builder.committedLift;
        this.committedCorrection = builder.committedCorrection;
        this.windupTracking = builder.windupTracking;
        this.activeTracking = builder.activeTracking;
        this.recoveryTracking = builder.recoveryTracking;
        this.volume = builder.volume;
        this.usesWeaponDamage = builder.usesWeaponDamage;
        this.damageScale = builder.damageScale;
        this.knockback = builder.knockback;
        this.knockbackVertical = builder.knockbackVertical;
        this.multiHit = builder.multiHit;
        this.multiHitCooldown = builder.multiHitCooldown;
        this.slash = builder.slash;
        this.ribbon = builder.ribbon;
        this.cooldownTicks = builder.cooldownTicks;
        this.urgentRecoveryTrim = builder.urgentRecoveryTrim;
        this.onStart = builder.onStart;
        this.onActiveStart = builder.onActiveStart;
        this.onActiveTick = builder.onActiveTick;
        this.onRecoveryStart = builder.onRecoveryStart;
        this.onFinish = builder.onFinish;
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    // --- accessors --------------------------------------------------------------------------------

    public String id() {
        return this.id;
    }

    public String animationController() {
        return this.animationController;
    }

    public @Nullable String animation() {
        return this.animation;
    }

    public boolean animationLoops() {
        return this.animationLoops;
    }

    public float animationSpeed() {
        return this.animationSpeed;
    }

    public int windupTicks() {
        return this.windupTicks;
    }

    public int activeTicks() {
        return this.activeTicks;
    }

    public int recoveryTicks() {
        return this.recoveryTicks;
    }

    public int totalTicks() {
        return this.windupTicks + this.activeTicks + this.recoveryTicks;
    }

    public AttackMotion motion() {
        return this.motion;
    }

    public double advance() {
        return this.advance;
    }

    public double windupDrift() {
        return this.windupDrift;
    }

    public double committedSpeed() {
        return this.committedSpeed;
    }

    public int committedTicks() {
        return this.committedTicks;
    }

    public double committedLift() {
        return this.committedLift;
    }

    public double committedCorrection() {
        return this.committedCorrection;
    }

    /**
     * The swept volume this attack damages through.
     *
     * <p>When present, {@link ActionRunner} resolves damage and draws the arc from it — both from the same
     * surface. When null the attack resolves some other way (a Spell Engine impact, a projectile) and the
     * runner leaves it alone.
     */
    public @Nullable AttackVolume volume() {
        return this.volume;
    }

    /** True when the first contact goes through {@code tryAttack}, carrying full weapon damage. */
    public boolean usesWeaponDamage() {
        return this.usesWeaponDamage;
    }

    public float damageScale() {
        return this.damageScale;
    }

    public double knockback() {
        return this.knockback;
    }

    public double knockbackVertical() {
        return this.knockbackVertical;
    }

    public boolean multiHit() {
        return this.multiHit;
    }

    public int multiHitCooldown() {
        return this.multiHitCooldown;
    }

    public @Nullable SlashVisual slash() {
        return this.slash;
    }

    public @Nullable SlashProfile ribbon() {
        return this.ribbon;
    }

    public int cooldownTicks() {
        return this.cooldownTicks;
    }

    public int urgentRecoveryTrim() {
        return this.urgentRecoveryTrim;
    }

    public TrackingMode tracking(AttackPhase phase) {
        return switch (phase) {
            case WINDUP -> this.windupTracking;
            case ACTIVE -> this.activeTracking;
            case RECOVERY, FINISHED -> this.recoveryTracking;
        };
    }

    void fireStart(ActionContext context) {
        if (this.onStart != null) this.onStart.accept(context);
    }

    void fireActiveStart(ActionContext context) {
        if (this.onActiveStart != null) this.onActiveStart.accept(context);
    }

    void fireActiveTick(ActionContext context) {
        if (this.onActiveTick != null) this.onActiveTick.accept(context);
    }

    void fireRecoveryStart(ActionContext context) {
        if (this.onRecoveryStart != null) this.onRecoveryStart.accept(context);
    }

    void fireFinish(ActionContext context) {
        if (this.onFinish != null) this.onFinish.accept(context);
    }

    @Override
    public String toString() {
        return "CombatAction[" + this.id + " " + this.motion + " "
                + this.windupTicks + "/" + this.activeTicks + "/" + this.recoveryTicks + "]";
    }

    // --- builder ----------------------------------------------------------------------------------

    public static final class Builder {
        private final String id;
        private String animationController = "attacks";
        private @Nullable String animation;
        private boolean animationLoops;
        private float animationSpeed = 1.0f;

        private int windupTicks = 8;
        private int activeTicks = 4;
        private int recoveryTicks = 8;

        private AttackMotion motion = AttackMotion.PLANTED;
        private double advance;
        private double windupDrift;
        private double committedSpeed = 0.6;
        private int committedTicks;
        private double committedLift;
        private double committedCorrection;

        private TrackingMode windupTracking = TrackingMode.REDUCED;
        private TrackingMode activeTracking = TrackingMode.LOCKED;
        private TrackingMode recoveryTracking = TrackingMode.MINIMAL;

        private @Nullable AttackVolume volume;
        private boolean usesWeaponDamage = true;
        private float damageScale = 1.0f;
        private double knockback = 0.4;
        private double knockbackVertical;
        private boolean multiHit;
        private int multiHitCooldown = 10;
        private @Nullable SlashVisual slash;
        private @Nullable SlashProfile ribbon;

        private int cooldownTicks = 40;
        private int urgentRecoveryTrim;

        private @Nullable Consumer<ActionContext> onStart;
        private @Nullable Consumer<ActionContext> onActiveStart;
        private @Nullable Consumer<ActionContext> onActiveTick;
        private @Nullable Consumer<ActionContext> onRecoveryStart;
        private @Nullable Consumer<ActionContext> onFinish;

        private Builder(String id) {
            this.id = id;
        }

        /** Animation clip played once when the action starts, on the given controller. */
        public Builder animation(String controller, String animation) {
            this.animationController = controller;
            this.animation = animation;
            return this;
        }

        public Builder animation(String animation) {
            return animation("attacks", animation);
        }

        public Builder animationLoops(boolean loops) {
            this.animationLoops = loops;
            return this;
        }

        public Builder animationSpeed(float speed) {
            this.animationSpeed = speed;
            return this;
        }

        public Builder timing(int windup, int active, int recovery) {
            this.windupTicks = windup;
            this.activeTicks = active;
            this.recoveryTicks = recovery;
            return this;
        }

        public Builder motion(AttackMotion motion) {
            this.motion = motion;
            return this;
        }

        /** Blocks carried forward across the active window. */
        public Builder advance(double blocks) {
            this.advance = blocks;
            return this;
        }

        /** Blocks of drift across the windup; negative loads backward before a swing. */
        public Builder windupDrift(double blocks) {
            this.windupDrift = blocks;
            return this;
        }

        /**
         * Committed-motion parameters: speed in blocks/tick, duration in ticks (0 = the active window),
         * one-off vertical impulse at launch, and how much the heading may still be corrected per tick.
         */
        public Builder committed(double speed, int ticks, double lift, double correction) {
            this.committedSpeed = speed;
            this.committedTicks = ticks;
            this.committedLift = lift;
            this.committedCorrection = correction;
            return this;
        }

        public Builder tracking(TrackingMode windup, TrackingMode active) {
            this.windupTracking = windup;
            this.activeTracking = active;
            return this;
        }

        public Builder recoveryTracking(TrackingMode tracking) {
            this.recoveryTracking = tracking;
            return this;
        }

        /**
         * The swept volume this attack damages through. Stating it is what ties the damage to the swing:
         * the arc travels the way the animation does, and the drawn crescent is the same surface.
         */
        public Builder volume(AttackVolume volume) {
            this.volume = volume;
            return this;
        }

        /** Full weapon damage on the first contact, through {@code tryAttack}. The default. */
        public Builder weaponDamage() {
            this.usesWeaponDamage = true;
            this.damageScale = 1.0f;
            return this;
        }

        /**
         * A fraction of the mob's attack damage instead of a weapon swing — for pokes and pulses that
         * exist to move a player rather than to kill one.
         */
        public Builder scaledDamage(float scale) {
            this.usesWeaponDamage = false;
            this.damageScale = scale;
            return this;
        }

        public Builder knockback(double horizontal, double vertical) {
            this.knockback = horizontal;
            this.knockbackVertical = vertical;
            return this;
        }

        /** Allow the same victim to be struck repeatedly, no more often than {@code cooldown} ticks apart. */
        public Builder multiHit(int cooldown) {
            this.multiHit = true;
            this.multiHitCooldown = cooldown;
            return this;
        }

        /** Particles drawn on the swept surface. Where they go comes from the volume, not from here. */
        public Builder slash(SlashVisual slash) {
            this.slash = slash;
            return this;
        }

        /** The ribbon mesh laid over the swept surface — the visible blade. */
        public Builder ribbon(SlashProfile ribbon) {
            this.ribbon = ribbon;
            return this;
        }

        public Builder cooldown(int ticks) {
            this.cooldownTicks = ticks;
            return this;
        }

        /** Ticks clipped off the tail of recovery in an urgent phase — never off the telegraph. */
        public Builder urgentRecoveryTrim(int ticks) {
            this.urgentRecoveryTrim = ticks;
            return this;
        }

        public Builder onStart(Consumer<ActionContext> hook) {
            this.onStart = hook;
            return this;
        }

        public Builder onActiveStart(Consumer<ActionContext> hook) {
            this.onActiveStart = hook;
            return this;
        }

        public Builder onActiveTick(Consumer<ActionContext> hook) {
            this.onActiveTick = hook;
            return this;
        }

        public Builder onRecoveryStart(Consumer<ActionContext> hook) {
            this.onRecoveryStart = hook;
            return this;
        }

        public Builder onFinish(Consumer<ActionContext> hook) {
            this.onFinish = hook;
            return this;
        }

        public CombatAction build() {
            var action = new CombatAction(this);
            // Self-registering, so a client that has the mob loaded can resolve a swing packet from its id
            // alone and rebuild the arc locally. Brains are constructed on both sides.
            AttackRegistry.register(action);
            return action;
        }
    }
}

package com.cleannrooster.rpg_minibosses.client.combat;

import com.cleannrooster.rpg_minibosses.entity.combat.CombatAction;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackFrame;
import net.minecraft.util.math.MathHelper;

/**
 * One slash currently playing on the client.
 *
 * <p>It holds no geometry of its own — just the action, the frame it was swung from, and how far through
 * it is. The ribbon mesh is regenerated from
 * {@link com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackGeometry} every frame using exactly
 * the numbers the server used for hit detection, which is what keeps the drawn arc honest.
 */
public final class SlashEffect {

    private final CombatAction action;
    private AttackFrame frame;
    private final int attackerId;
    private final int lifetime;

    private int age;

    SlashEffect(CombatAction action, AttackFrame frame, int attackerId) {
        this.action = action;
        this.frame = frame;
        this.attackerId = attackerId;
        var profile = action.ribbon();
        this.lifetime = Math.max(1, action.activeTicks())
                + (profile == null ? 0 : profile.fadeOutTicks());
    }

    public CombatAction action() {
        return this.action;
    }

    public AttackFrame frame() {
        return this.frame;
    }

    void setFrame(AttackFrame frame) {
        this.frame = frame;
    }

    public int attackerId() {
        return this.attackerId;
    }

    void tick() {
        this.age++;
    }

    boolean isExpired() {
        return this.age >= this.lifetime;
    }

    /** True while the corresponding server-side damage window is open. */
    public boolean isSwinging(float partialTick) {
        return this.age + partialTick <= this.action.activeTicks();
    }

    /**
     * Leading edge of the swing, 0-1. Advancing this every frame is what animates the arc through its
     * sweep instead of popping a finished crescent into existence.
     */
    public float leadingEdge(float partialTick) {
        var active = Math.max(1, this.action.activeTicks());
        return MathHelper.clamp((this.age + partialTick) / active, 0.0f, 1.0f);
    }

    /** Trailing edge of the visible ribbon — the leading edge minus the profile's trail length. */
    public float trailingEdge(float partialTick) {
        var volume = this.action.volume();
        if (volume == null || !volume.shape().isSwept()) {
            return 0.0f; // cones and impacts show their whole span at once
        }
        var profile = this.action.ribbon();
        var trail = profile == null ? 1.0f : profile.trail();
        return Math.max(0.0f, leadingEdge(partialTick) - trail);
    }

    /** Fade applied once the damaging window has closed, so the arc dissipates rather than vanishing. */
    public float fade(float partialTick) {
        var profile = this.action.ribbon();
        var fadeTicks = profile == null ? 0 : profile.fadeOutTicks();
        var elapsed = this.age + partialTick - this.action.activeTicks();
        if (elapsed <= 0.0f || fadeTicks <= 0) {
            return 1.0f;
        }
        return MathHelper.clamp(1.0f - elapsed / fadeTicks, 0.0f, 1.0f);
    }
}

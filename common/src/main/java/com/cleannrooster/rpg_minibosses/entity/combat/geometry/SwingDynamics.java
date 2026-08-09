package com.cleannrooster.rpg_minibosses.entity.combat.geometry;

import net.minecraft.util.math.MathHelper;

/**
 * How a swing's <em>speed</em> is distributed across its active window.
 *
 * <p>Everything else here describes where a swing goes. This describes how fast it is going when it gets
 * there. A {@link SwingPath} declares its start and end angles; this decides whether the blade covers that
 * span at a constant rate or rips through the middle of it.
 *
 * <p><b>Why this is not just a visual tweak.</b> The value returned reshapes {@code s}, the swing-progress
 * parameter, and {@code s} is the input to {@link AttackGeometry#sweepAngleDegrees} — which backs the
 * damage sweep and the particle slash alike. One curve moves both together, so the drawn arc stays exactly
 * the volume that can hit you. There is no way to make the visual accelerate without the hitbox
 * accelerating with it.
 *
 * <p>Total duration is untouched: every curve maps 0 to 0 and 1 to 1, so an attack declared as N active
 * ticks still sweeps its full arc in exactly N ticks. Only the distribution changes.
 *
 * <p>This also happens to match the hand-authored clips better than a constant rate does. Bedrock
 * keyframes interpolate smoothly, so a swing already eases out of its wind-back and into its strike — the
 * weapon really is slowest at the ends and fastest in the middle. {@link #FORCEFUL} agrees with that;
 * {@link #STEADY} was the thing that disagreed.
 */
public enum SwingDynamics {
    /** Constant angular speed. Correct for anything that should read as mechanical or unhurried. */
    STEADY {
        @Override
        public float shape(float s) {
            return s;
        }
    },

    /**
     * Measured start, violent middle, controlled arrest.
     *
     * <p>Smoothstep ({@code 3s² − 2s³}) applied twice. A single smoothstep leaves about 58% of the arc in
     * the middle third; composing it reaches roughly 67%, so the outer thirds become a held anticipation
     * and a clean settle. Zero slope at both ends, symmetric, polynomial throughout.
     *
     * <p>The damaging portion of the swing genuinely <em>is</em> the apex of its momentum, because the hit
     * test reads the same curve the eye does.
     */
    FORCEFUL {
        @Override
        public float shape(float s) {
            var once = s * s * (3.0f - 2.0f * s);
            return once * once * (3.0f - 2.0f * once);
        }
    },

    /**
     * Pure acceleration, no settle: slowest at the root, fastest at full extension.
     *
     * <p>For thrusts and lanes, where the point should arrive at its furthest reach still gaining speed.
     * A weapon that eases into its own maximum extension reads as <em>placing</em> the tip; one still
     * accelerating when it gets there reads as launched.
     */
    LUNGE {
        @Override
        public float shape(float s) {
            return s * s * (2.0f - s * 0.35f) / 1.65f;
        }
    };

    /**
     * Remap swing progress. Implementations must satisfy {@code shape(0) == 0}, {@code shape(1) == 1} and
     * be monotonically non-decreasing — a curve that ran backwards would drag the damage sweep back over
     * ground it had already covered.
     */
    public abstract float shape(float s);

    /**
     * Clamped wrapper. Geometry calls this rather than {@link #shape} directly, so a caller that overshoots
     * the window cannot push the blade past its declared end angle.
     */
    public final float apply(float s) {
        return shape(MathHelper.clamp(s, 0.0f, 1.0f));
    }
}

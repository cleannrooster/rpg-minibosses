package com.cleannrooster.rpg_minibosses.entity.combat;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

/**
 * The rendered blade of an attack: a textured ribbon laid directly onto the attack's own swept surface,
 * so what the player sees is the volume that hurts them.
 *
 * <p>Nothing here says where the ribbon goes — that comes from
 * {@link com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackGeometry}, the same function the
 * server queries when deciding whether you were hit. A horizontal sweep therefore renders as a horizontal
 * crescent of exactly its real reach and arc, a rising cut as a rising arc, a charge as a lane the length
 * of its actual trail. This record only decides how that surface is painted.
 *
 * <p>Ported from the Divine Encounters combat system.
 *
 * @param texture       ribbon texture; U runs along the swing, V across the blade
 * @param red           tint, 0-1
 * @param green         tint, 0-1
 * @param blue          tint, 0-1
 * @param alpha         peak opacity
 * @param sweepSegments subdivisions along the swing direction; more = smoother crescent
 * @param bladeSegments subdivisions from root to tip
 * @param trail         how much of the already-swept arc stays visible behind the leading edge, as a
 *                      fraction of the full arc
 * @param innerFraction where the drawn ribbon starts along the blade — a slash reads better with the
 *                      innermost part near the shoulder left empty
 * @param thickness     visual half-thickness perpendicular to the swing plane, in blocks
 * @param fadeOutTicks  how long the ribbon lingers and fades after the active window ends
 * @param emissive      draw fullbright, so the arc reads at night and indoors
 * @param overreach     how far a dim outer bloom projects past the real reach, as a fraction of the range.
 *                      0 draws nothing extra. See {@link #MAX_OVERREACH} — this is the one part of the
 *                      ribbon that is deliberately not the damage volume, and it is drawn differently on
 *                      purpose.
 */
public record SlashProfile(
        Identifier texture,
        float red,
        float green,
        float blue,
        float alpha,
        int sweepSegments,
        int bladeSegments,
        float trail,
        float innerFraction,
        float thickness,
        int fadeOutTicks,
        boolean emissive,
        float overreach
) {
    /** Clamped at construction, so no profile can quietly exceed the fairness ceiling. */
    public SlashProfile {
        overreach = MathHelper.clamp(overreach, 0.0f, MAX_OVERREACH);
    }

    /** Profiles that predate the bloom keep working unchanged. */
    public SlashProfile(Identifier texture, float red, float green, float blue, float alpha,
                        int sweepSegments, int bladeSegments, float trail, float innerFraction,
                        float thickness, int fadeOutTicks, boolean emissive) {
        this(texture, red, green, blue, alpha, sweepSegments, bladeSegments, trail, innerFraction,
                thickness, fadeOutTicks, emissive, 0.0f);
    }

    /**
     * Hard ceiling on the bloom, as a fraction of the attack's range.
     *
     * <p>The bloom exists so a strike reads as <em>projecting</em> force rather than stopping dead at the
     * blade — but every block it extends is a block where the player sees an effect and takes no damage,
     * and past a certain point that stops reading as pressure and starts reading as a lie. A third of the
     * range is about where it turns.
     */
    public static final float MAX_OVERREACH = 0.34f;

    /**
     * Opacity of the bloom relative to the arc's own alpha.
     *
     * <p>This number is the fairness contract, expressed as a constant. The bloom must stay faint enough
     * that the bright core of the ribbon — which <em>is</em> the damage volume, exactly — remains the
     * obvious shape. If the two are ever comparable in brightness, the arc stops telling the truth about
     * its reach.
     */
    public static final float OVERREACH_ALPHA = 0.3f;

    public static final Identifier DEFAULT_TEXTURE =
            Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/effect/slash.png");
    /** Narrower, harder-edged texture for thrusts and charge trails. */
    public static final Identifier THRUST_TEXTURE =
            Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/effect/thrust.png");

    /** Draws nothing — for attacks whose motion is the visual. */
    public static final SlashProfile NONE =
            new SlashProfile(DEFAULT_TEXTURE, 1, 1, 1, 0.0f, 2, 2, 0.0f, 0.0f, 0.0f, 0, false);

    public boolean isVisible() {
        return this.alpha > 0.01f;
    }

    public boolean hasOverreach() {
        return this.overreach > 0.001f && isVisible();
    }

    /** A broad, slow crescent with real thickness — heavy two-handed weapons. */
    public static SlashProfile heavyCrescent(float red, float green, float blue) {
        return new SlashProfile(DEFAULT_TEXTURE, red, green, blue, 0.85f, 26, 5, 0.9f, 0.34f, 0.14f, 5,
                true, 0.24f);
    }

    /** A thin, quick crescent — blades and daggers. */
    public static SlashProfile crescent(float red, float green, float blue) {
        return new SlashProfile(DEFAULT_TEXTURE, red, green, blue, 0.78f, 22, 4, 0.8f, 0.3f, 0.07f, 4,
                true, 0.16f);
    }

    /**
     * An elongated streak for lanes and charge trails.
     *
     * <p>No bloom: a lane's drawn reach is already its whole visual, and the renderer has no swept arc to
     * project along.
     */
    public static SlashProfile streak(float red, float green, float blue) {
        return new SlashProfile(THRUST_TEXTURE, red, green, blue, 0.7f, 8, 10, 1.0f, 0.0f, 0.05f, 4, true);
    }

    public SlashProfile withAlpha(float alpha) {
        return new SlashProfile(this.texture, this.red, this.green, this.blue, alpha, this.sweepSegments,
                this.bladeSegments, this.trail, this.innerFraction, this.thickness, this.fadeOutTicks,
                this.emissive, this.overreach);
    }

    public SlashProfile withOverreach(float overreach) {
        return new SlashProfile(this.texture, this.red, this.green, this.blue, this.alpha,
                this.sweepSegments, this.bladeSegments, this.trail, this.innerFraction, this.thickness,
                this.fadeOutTicks, this.emissive, overreach);
    }
}

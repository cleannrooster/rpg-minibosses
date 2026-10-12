package com.cleannrooster.rpg_minibosses.magic;

import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * One boss ability: what school it is, how it reaches its targets and what it does to each. Immutable and
 * shared, like {@link com.cleannrooster.rpg_minibosses.entity.combat.CombatAction}; attach one to an
 * action with {@code CombatAction.Builder#cast}, or fire it directly with {@link AbilityExecutor}.
 *
 * <p>Replaces a boss spell JSON. How it looks and sounds is its {@link Presentation}; left at the
 * default, an ability gets its school's sounds, a telegraph for anything with an area, and shapes chosen
 * from its delivery ({@link AbilityPresentation}).</p>
 */
public record AbilitySpec(Identifier id, MagicSchool school, Intent intent, Delivery delivery, List<Impact> impacts,
                          Presentation presentation) {

    /**
     * Overrides for how an ability presents itself. Every null means "the default for this school and
     * delivery".
     *
     * @param telegraph whether an action casting this warns of its area during the wind-up
     * @param look      the school whose colours and default sounds it uses, if not its own (a physical
     *                  bomb that should look and sound like fire)
     */
    public record Presentation(@Nullable SoundEvent windupSound, @Nullable SoundEvent releaseSound,
                               @Nullable SoundEvent impactSound, @Nullable Identifier releaseCue,
                               @Nullable Identifier impactCue, boolean telegraph, @Nullable MagicSchool look,
                               int telegraphColour) {
        /** {@link #telegraphColour} when the telegraph takes the usual look: red to hurt, green to help. */
        public static final int USUAL_TELEGRAPH = 0;
        public static final Presentation DEFAULT = new Presentation(null, null, null, null, null, true, null, USUAL_TELEGRAPH);
    }

    /** The school it looks and sounds like. */
    public MagicSchool lookSchool() {
        return this.presentation.look() != null ? this.presentation.look() : this.school;
    }

    public enum Intent {
        /** Reaches only what the caster may harm ({@link Relations#canHarm}). */
        HARMFUL,
        /** Reaches only the caster's allies ({@link Relations#canHelp}). */
        HELPFUL
    }

    public AbilitySpec {
        impacts = List.copyOf(impacts);
    }

    public static Builder builder(Identifier id, MagicSchool school) {
        return new Builder(id, school);
    }

    public static final class Builder {
        private final Identifier id;
        private final MagicSchool school;
        private Intent intent = Intent.HARMFUL;
        private Delivery delivery = new Delivery.Direct();
        private final java.util.ArrayList<Impact> impacts = new java.util.ArrayList<>();
        private @Nullable SoundEvent windupSound;
        private @Nullable SoundEvent releaseSound;
        private @Nullable SoundEvent impactSound;
        private @Nullable Identifier releaseCue;
        private @Nullable Identifier impactCue;
        private boolean telegraph = true;
        private @Nullable MagicSchool look;
        private int telegraphColour = Presentation.USUAL_TELEGRAPH;

        private Builder(Identifier id, MagicSchool school) {
            this.id = id;
            this.school = school;
        }

        public Builder helpful() {
            this.intent = Intent.HELPFUL;
            return this;
        }

        public Builder delivery(Delivery delivery) {
            this.delivery = delivery;
            return this;
        }

        public Builder impact(Impact impact) {
            this.impacts.add(impact);
            return this;
        }

        /** Shorthand for the commonest impact: {@code coefficient} × power as damage, default knockback. */
        public Builder damage(float coefficient) {
            return impact(Impact.Damage.of(coefficient));
        }

        /** Replace the school's sounds; null keeps the default for that slot. */
        public Builder sounds(@Nullable SoundEvent windup, @Nullable SoundEvent release, @Nullable SoundEvent impact) {
            this.windupSound = windup;
            this.releaseSound = release;
            this.impactSound = impact;
            return this;
        }

        /** Replace the shape drawn as it goes off (a {@code FxCues} id, or one of a boss's own). */
        public Builder releaseCue(Identifier cue) {
            this.releaseCue = cue;
            return this;
        }

        /** Replace the shape drawn where it lands. */
        public Builder impactCue(Identifier cue) {
            this.impactCue = cue;
            return this;
        }

        /** Look and sound like {@code school} while still dealing this ability's own school of damage. */
        /** Draws its telegraph in {@code rgb} instead of the usual red (or green, for a helpful ability). */
        public Builder telegraphColour(int rgb) {
            this.telegraphColour = rgb & 0xFFFFFF;
            return this;
        }

        public Builder looksLike(MagicSchool school) {
            this.look = school;
            return this;
        }

        /** No wind-up warning, for areas that should land unannounced. */
        public Builder noTelegraph() {
            this.telegraph = false;
            return this;
        }

        public AbilitySpec build() {
            var presentation = new Presentation(this.windupSound, this.releaseSound, this.impactSound,
                    this.releaseCue, this.impactCue, this.telegraph, this.look, this.telegraphColour);
            var spec = new AbilitySpec(this.id, this.school, this.intent, this.delivery, this.impacts, presentation);
            AbilityRegistry.register(spec);
            return spec;
        }
    }
}

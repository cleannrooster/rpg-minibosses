package com.cleannrooster.rpg_minibosses.magic;

import net.minecraft.util.Identifier;

import java.util.List;

/**
 * One boss ability: what school it is, how it reaches its targets and what it does to each. Immutable and
 * shared, like {@link com.cleannrooster.rpg_minibosses.entity.combat.CombatAction}; attach one to an
 * action with {@code CombatAction.Builder#cast}, or fire it directly with {@link AbilityExecutor}.
 *
 * <p>Replaces a boss spell JSON. Effect and sound cues join it in later phases.</p>
 */
public record AbilitySpec(Identifier id, MagicSchool school, Intent intent, Delivery delivery, List<Impact> impacts) {

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

        public AbilitySpec build() {
            var spec = new AbilitySpec(this.id, this.school, this.intent, this.delivery, this.impacts);
            AbilityRegistry.register(spec);
            return spec;
        }
    }
}

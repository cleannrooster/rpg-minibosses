package com.cleannrooster.rpg_minibosses.magic;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.entry.RegistryEntry;

/**
 * One thing that happens to one target when an ability lands. An ability lists several; they run in
 * order against each target its delivery reaches.
 */
public sealed interface Impact permits Impact.Damage, Impact.Ignite, Impact.ApplyEffect, Impact.Knockback, Impact.Heal {

    void apply(ImpactContext context, LivingEntity target);

    /**
     * Rolled power × {@code coefficient} as the school's damage. Same shape as Spell Engine's DAMAGE impact.
     *
     * @param knockback      multiplier on the 0.4 base push Spell Engine uses; 0 for none
     * @param bypassIframes  hit even inside the target's invulnerability window (multi-hit pulses)
     */
    record Damage(float coefficient, float knockback, boolean bypassIframes) implements Impact {
        public static Damage of(float coefficient) {
            return new Damage(coefficient, 1.0f, false);
        }

        @Override
        public void apply(ImpactContext context, LivingEntity target) {
            var backend = MagicBackend.active();
            double amount = backend.roll(context.caster(), context.school(), context.basePower(), target)
                    * this.coefficient * context.scale();
            if (amount <= 0.0) {
                return;
            }
            if (this.bypassIframes) {
                target.timeUntilRegen = 0;
            }
            var source = backend.damageSource(context.caster(), context.school(), context.direct());
            if (target.damage(source, (float) amount) && this.knockback > 0.0f) {
                Knockback.push(context, target, 0.4 * this.knockback, 0.0);
            }
        }
    }

    /** Set on fire for {@code seconds}. */
    record Ignite(int seconds) implements Impact {
        @Override
        public void apply(ImpactContext context, LivingEntity target) {
            target.setOnFireFor(this.seconds);
        }
    }

    record ApplyEffect(RegistryEntry<StatusEffect> effect, int durationTicks, int amplifier) implements Impact {
        @Override
        public void apply(ImpactContext context, LivingEntity target) {
            target.addStatusEffect(new StatusEffectInstance(this.effect, this.durationTicks, this.amplifier),
                    context.caster());
        }
    }

    /** A push away from the impact origin, independent of damage. */
    record Knockback(double horizontal, double vertical) implements Impact {
        @Override
        public void apply(ImpactContext context, LivingEntity target) {
            push(context, target, this.horizontal, this.vertical);
        }

        static void push(ImpactContext context, LivingEntity target, double horizontal, double vertical) {
            var away = target.getPos().subtract(context.origin());
            double dx = away.x;
            double dz = away.z;
            if (dx * dx + dz * dz < 1.0e-6) {
                // Standing on the origin: push along the caster's facing instead of nowhere.
                var facing = context.caster().getRotationVector();
                dx = facing.x;
                dz = facing.z;
            }
            // takeKnockback pushes *against* the vector it is given.
            target.takeKnockback(horizontal, -dx, -dz);
            if (vertical != 0.0) {
                target.addVelocity(0.0, vertical, 0.0);
                target.velocityModified = true;
            }
        }
    }

    /** Rolled power × {@code coefficient} as healing. */
    record Heal(float coefficient) implements Impact {
        @Override
        public void apply(ImpactContext context, LivingEntity target) {
            double amount = MagicBackend.active().roll(context.caster(), context.school(), context.basePower(), null)
                    * this.coefficient * context.scale();
            if (amount > 0.0) {
                target.heal((float) amount);
            }
        }
    }
}

package com.cleannrooster.anarchy_rpg.magic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.jetbrains.annotations.Nullable;

/**
 * How power becomes a number and damage becomes a {@link DamageSource}. Two implementations:
 * {@link VanillaMagicBackend} (no Spell Power: flat power, this mod's damage types) and the Spell Power
 * backend in {@code compat.spellengine} (crits, vulnerability, Spell Power's damage types, so resistances
 * and other mods' defenses apply). Abilities never branch on which one is active.
 */
public interface MagicBackend {
    /**
     * The amount one impact is scaled from: the caster's power in {@code school}, rolled for a crit and
     * adjusted for the target's vulnerability where the backend supports that.
     *
     * @param base the caster's unrolled power, already resolved by {@link MagicBackends#basePower}
     */
    double roll(LivingEntity caster, MagicSchool school, double base, @Nullable LivingEntity target);

    /** Power for a caster that is not a {@link MagicCaster}. */
    double fallbackPower(LivingEntity caster, MagicSchool school);

    /** @param direct the projectile or cloud that carried the hit, or null for a direct cast */
    DamageSource damageSource(LivingEntity caster, MagicSchool school, @Nullable Entity direct);

    /** Spell Power's backend when Spell Engine is installed, otherwise the vanilla one. */
    static MagicBackend active() {
        return MagicBackends.active();
    }
}

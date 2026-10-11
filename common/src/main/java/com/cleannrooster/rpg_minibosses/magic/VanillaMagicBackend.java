package com.cleannrooster.rpg_minibosses.magic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.registry.RegistryKeys;
import org.jetbrains.annotations.Nullable;

/**
 * The backend used without Spell Power: power is the caster's stated value with no crits, magical hits
 * are this mod's own damage types, physical hits are plain mob attacks.
 */
public final class VanillaMagicBackend implements MagicBackend {
    public static final VanillaMagicBackend INSTANCE = new VanillaMagicBackend();

    private VanillaMagicBackend() {
    }

    @Override
    public double roll(LivingEntity caster, MagicSchool school, double base, @Nullable LivingEntity target) {
        return base;
    }

    @Override
    public double fallbackPower(LivingEntity caster, MagicSchool school) {
        // Without Spell Power nothing else grants spell power, so a caster that does not state its own
        // hits with its weapon strength: the same source Spell Engine uses for the physical schools.
        return attackDamage(caster);
    }

    @Override
    public DamageSource damageSource(LivingEntity caster, MagicSchool school, @Nullable Entity direct) {
        var sources = caster.getDamageSources();
        if (!school.isMagical()) {
            return direct != null ? sources.mobProjectile(direct, caster) : sources.mobAttack(caster);
        }
        var type = caster.getWorld().getRegistryManager().get(RegistryKeys.DAMAGE_TYPE).entryOf(school.damageType());
        return new DamageSource(type, direct != null ? direct : caster, caster);
    }

    static double attackDamage(LivingEntity caster) {
        var attribute = caster.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_DAMAGE);
        return attribute == null ? 0.0 : attribute.getValue();
    }
}

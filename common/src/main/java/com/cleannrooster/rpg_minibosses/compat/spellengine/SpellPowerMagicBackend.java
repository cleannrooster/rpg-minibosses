package com.cleannrooster.rpg_minibosses.compat.spellengine;

import com.cleannrooster.rpg_minibosses.magic.MagicBackend;
import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import com.cleannrooster.rpg_minibosses.magic.VanillaMagicBackend;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.spell_power.api.SpellDamageSource;
import net.spell_power.api.SpellPower;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.jetbrains.annotations.Nullable;

/**
 * Boss damage through Spell Power, the way Spell Engine's DAMAGE impact does it: the caster's power is
 * rolled for a crit against the target's vulnerability, and the hit is Spell Power's damage type for the
 * school, so resistances and other mods' spell defenses apply.
 *
 * <p>The base power still comes from the boss ({@code MagicCaster}); only crit chance, crit damage and
 * vulnerability come from Spell Power. A school Spell Power does not know falls back to the core backend.</p>
 */
final class SpellPowerMagicBackend implements MagicBackend {
    static final SpellPowerMagicBackend INSTANCE = new SpellPowerMagicBackend();

    private SpellPowerMagicBackend() {
    }

    private static @Nullable SpellSchool school(MagicSchool school) {
        return SpellSchools.getSchool(school.spellPowerId().toString());
    }

    @Override
    public double roll(LivingEntity caster, MagicSchool school, double base, @Nullable LivingEntity target) {
        var spellSchool = school(school);
        if (spellSchool == null) {
            return VanillaMagicBackend.INSTANCE.roll(caster, school, base, target);
        }
        var stats = stats(spellSchool, caster);
        if (stats == null) {
            return VanillaMagicBackend.INSTANCE.roll(caster, school, base, target);
        }
        var result = new SpellPower.Result(spellSchool, base, stats.criticalChance(), stats.criticalDamage());
        return target != null
                ? result.random(SpellPower.getVulnerability(target, spellSchool)).amount()
                : result.randomValue();
    }

    @Override
    public double fallbackPower(LivingEntity caster, MagicSchool school) {
        var spellSchool = school(school);
        var stats = spellSchool == null ? null : stats(spellSchool, caster);
        return stats != null ? stats.baseValue() : VanillaMagicBackend.INSTANCE.fallbackPower(caster, school);
    }

    /**
     * Spell Power's view of {@code caster}, or null if it cannot give one: bosses state their own power and
     * do not carry Spell Power's attributes, and Spell Power reads them unguarded.
     */
    private static @Nullable SpellPower.Result stats(SpellSchool school, LivingEntity caster) {
        try {
            return SpellPower.getSpellPower(school, caster);
        } catch (IllegalArgumentException missingAttribute) {
            return null;
        }
    }

    @Override
    public DamageSource damageSource(LivingEntity caster, MagicSchool school, @Nullable Entity direct) {
        var spellSchool = school(school);
        if (spellSchool == null || !school.isMagical()) {
            return VanillaMagicBackend.INSTANCE.damageSource(caster, school, direct);
        }
        return SpellDamageSource.create(spellSchool, caster);
    }
}

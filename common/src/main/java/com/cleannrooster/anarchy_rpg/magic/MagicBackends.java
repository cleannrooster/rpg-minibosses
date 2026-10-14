package com.cleannrooster.anarchy_rpg.magic;

import com.cleannrooster.anarchy_rpg.compat.SpellEngineCompat;
import net.minecraft.entity.LivingEntity;

/** Picks the active {@link MagicBackend} once, and resolves a caster's base power through it. */
public final class MagicBackends {
    private static MagicBackend active;

    private MagicBackends() {
    }

    static MagicBackend active() {
        if (active == null) {
            var fromCompat = SpellEngineCompat.hooks().magicBackend();
            active = fromCompat != null ? fromCompat : VanillaMagicBackend.INSTANCE;
        }
        return active;
    }

    /**
     * A caster's unrolled power in {@code school}. Physical schools always use attack damage, as Spell
     * Engine does; magical schools use the caster's own stated value when it has one.
     */
    public static double basePower(LivingEntity caster, MagicSchool school) {
        if (!school.isMagical()) {
            return VanillaMagicBackend.attackDamage(caster);
        }
        if (caster instanceof MagicCaster magicCaster) {
            return magicCaster.magicPower(school);
        }
        return active().fallbackPower(caster, school);
    }
}

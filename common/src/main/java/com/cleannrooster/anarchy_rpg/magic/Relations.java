package com.cleannrooster.anarchy_rpg.magic;

import com.cleannrooster.anarchy_rpg.entity.GeminiEntity;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Ownable;
import net.minecraft.entity.player.PlayerEntity;

import java.util.Objects;

/**
 * Who an ability may hurt or help. The single predicate every delivery filters through, replacing Spell
 * Engine's {@code EntityRelations} + {@code TargetHelper} for boss abilities.
 *
 * <p>Minibosses keep their own {@link MinibossEntity#canHarm} (tamed bosses, downed bosses, betrayal);
 * until the bosses are ported that still ends in Spell Engine's check, so behaviour is unchanged while
 * Spell Engine is required. Everyone else gets {@link #baseCanHarm}.</p>
 */
public final class Relations {
    private Relations() {
    }

    public static boolean canHarm(LivingEntity caster, Entity target) {
        if (caster instanceof MinibossEntity miniboss) {
            return miniboss.canHarm(target);
        }
        if (caster instanceof GeminiEntity gemini && isPartner(gemini, target)) {
            return false;
        }
        return baseCanHarm(caster, target);
    }

    /** Allies: self, teammates, a shared owner, or a Gemini's partner. */
    public static boolean canHelp(LivingEntity caster, Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive()) {
            return false;
        }
        if (target == caster || caster.isTeammate(target)) {
            return true;
        }
        if (caster instanceof GeminiEntity gemini && isPartner(gemini, target)) {
            return true;
        }
        return sharesOwner(caster, target);
    }

    /**
     * The rules every caster obeys: never itself, nothing dead, spectating or in creative, never a
     * teammate, never its owner or something sharing its owner.
     */
    public static boolean baseCanHarm(LivingEntity caster, Entity target) {
        if (target == caster || !(target instanceof LivingEntity living) || !living.isAlive() || target.isSpectator()) {
            return false;
        }
        if (target instanceof PlayerEntity player && player.isCreative()) {
            return false;
        }
        if (caster.isTeammate(target)) {
            return false;
        }
        if (caster instanceof Ownable ownable && ownable.getOwner() == target) {
            return false;
        }
        if (target instanceof Ownable ownable && ownable.getOwner() == caster) {
            return false;
        }
        return !sharesOwner(caster, target);
    }

    private static boolean sharesOwner(Entity a, Entity b) {
        return a instanceof Ownable first && b instanceof Ownable second
                && first.getOwner() != null && Objects.equals(first.getOwner(), second.getOwner());
    }

    private static boolean isPartner(GeminiEntity gemini, Entity target) {
        var partner = gemini.getPartner();
        return partner != null && partner.equals(target);
    }
}

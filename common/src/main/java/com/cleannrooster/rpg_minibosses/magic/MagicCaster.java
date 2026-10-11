package com.cleannrooster.rpg_minibosses.magic;

/**
 * An entity that states its own spell power. Bosses implement this from their config values (fire mage
 * fire power, templar healing power, ...), so their damage does not depend on Spell Power's attributes
 * existing. Casters that do not implement it fall back to the backend's own lookup.
 */
public interface MagicCaster {
    /** Power for a magical school, before crits and vulnerability. 0 if this caster has none in it. */
    double magicPower(MagicSchool school);
}

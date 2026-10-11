package com.cleannrooster.rpg_minibosses.magic;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.entity.damage.DamageType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;

/**
 * The kinds of damage a boss ability can deal. Mirrors Spell Power's schools one to one, so the Spell
 * Power backend can resolve each by id and the core backend can stand in for it.
 *
 * <p>Magical schools take their power from the caster ({@link MagicCaster}) and deal one of this mod's
 * own damage types ({@code rpg-minibosses:<name>}), tagged the way Spell Power tags its own: magic,
 * bypasses armor and shields. Physical schools take the attacker's attack damage and deal mob-attack
 * damage, as Spell Engine's physical schools do.</p>
 */
public enum MagicSchool {
    GENERIC("generic", 0xffffff, true),
    ARCANE("arcane", 0xff66ff, true),
    FIRE("fire", 0xff3300, true),
    FROST("frost", 0xccffff, true),
    HEALING("healing", 0x66ff66, true),
    LIGHTNING("lightning", 0xffff66, true),
    SOUL("soul", 0x2dd4da, true),
    PHYSICAL_MELEE("physical_melee", 0xb3b3b3, false),
    PHYSICAL_RANGED("physical_ranged", 0x805e4d, false);

    private final String name;
    private final int color;
    private final boolean magical;

    MagicSchool(String name, int color, boolean magical) {
        this.name = name;
        this.color = color;
        this.magical = magical;
    }

    public String schoolName() {
        return this.name;
    }

    /** RGB, for effects that tint by school. */
    public int color() {
        return this.color;
    }

    public boolean isMagical() {
        return this.magical;
    }

    /** The matching Spell Power school id, e.g. {@code spell_power:fire}. */
    public Identifier spellPowerId() {
        return Identifier.of("spell_power", this.name);
    }

    /** This mod's damage type for a magical school. Physical schools have none (they use mob attack damage). */
    public RegistryKey<DamageType> damageType() {
        if (!this.magical) {
            throw new IllegalStateException(this + " is physical and has no magic damage type");
        }
        return RegistryKey.of(RegistryKeys.DAMAGE_TYPE, RPGMinibosses.id(this.name));
    }
}

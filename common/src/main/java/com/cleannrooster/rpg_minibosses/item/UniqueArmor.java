package com.cleannrooster.rpg_minibosses.item;

import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.registry.entry.RegistryEntry;
import net.spell_engine.rpg_series.item.Armor;

public class UniqueArmor extends Armor

        .CustomItem {
    public UniqueArmor(RegistryEntry<ArmorMaterial> material, Type slot, Settings settings) {
        super(material, slot, settings);
    }

}

package com.cleannrooster.rpg_minibosses.item;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.spell_engine.rpg_series.item.Armor;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.List;

public class AbberrathArmor extends Armor.CustomItem {
    public SpellSchool school = SpellSchools.FIRE;
    public AbberrathArmor(RegistryEntry<ArmorMaterial> material, ArmorItem.Type type, Item.Settings settings, SpellSchool school) {
        super(material, type, settings);
    }

    public SpellSchool getMagicSchool() {
        return school;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("text.rpg-minibosses.abberraths_hooves").formatted(Formatting.DARK_RED));

        super.appendTooltip(stack, context, tooltip, type);
    }

}

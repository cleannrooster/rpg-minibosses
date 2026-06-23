package com.cleannrooster.rpg_minibosses.item;

import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.spell_engine.api.item.weapon.StaffItem;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.List;

public class WhisperingIceStaff extends StaffItem {
    public WhisperingIceStaff(ToolMaterial material, Settings settings) {
        super(material, settings);
    }
    public SpellSchool school = SpellSchools.FROST;

    public SpellSchool getMagicSchool() {
        return school;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("text.rpg-minibosses.whispering_ice").formatted(Formatting.DARK_RED));
        super.appendTooltip(stack, context, tooltip, type);

    }

}

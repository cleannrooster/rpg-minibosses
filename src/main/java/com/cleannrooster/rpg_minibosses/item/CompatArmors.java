package com.cleannrooster.rpg_minibosses.item;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.extraspellattributes.ReabsorptionInit;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.spell_engine.api.config.ArmorSetConfig;
import net.spell_engine.api.config.AttributeModifier;
import net.spell_engine.api.item.armor.Armor;

import java.util.List;

public class CompatArmors {
    public static RegistryEntry<ArmorMaterial> juggernaut = Armors.material(
            "juggernaut_armor",
            3, 8, 6, 3,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, () -> Ingredient.ofItems(Items.NETHERITE_SCRAP));

    public static RegistryEntry<ArmorMaterial> trickster = Armors.material(
            "trickster_armor",
            2, 6, 4, 2,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, () -> Ingredient.ofItems(Items.LEATHER,Items.IRON_INGOT));
    public static final Armor.Set tricksterArmor = Armors.create(
            trickster,
            Identifier.of(RPGMinibosses.MOD_ID, "trickster"),
            30,
            ThiefArmor::new,
            ArmorSetConfig.with(
                    new ArmorSetConfig.Piece(2)
                            .addAll(List.of(
                                    AttributeModifier.multiply(Identifier.tryParse(ReabsorptionInit.SPELLSUPPRESS.getIdAsString()), 0.15F),
                                    AttributeModifier.multiply(Identifier.tryParse(ReabsorptionInit.GLANCINGBLOW.getIdAsString()), 0.15F)


                            )),
                    new ArmorSetConfig.Piece(6)
                            .addAll(List.of(
                                    AttributeModifier.multiply(Identifier.tryParse(ReabsorptionInit.SPELLSUPPRESS.getIdAsString()), 0.15F),
                                    AttributeModifier.multiply(Identifier.tryParse(ReabsorptionInit.GLANCINGBLOW.getIdAsString()), 0.15F)

                            )),
                    new ArmorSetConfig.Piece(4)
                            .addAll(List.of(
                                    AttributeModifier.multiply(Identifier.tryParse(ReabsorptionInit.SPELLSUPPRESS.getIdAsString()), 0.15F),
                                    AttributeModifier.multiply(Identifier.tryParse(ReabsorptionInit.GLANCINGBLOW.getIdAsString()), 0.15F)

                            )),
                    new ArmorSetConfig.Piece(2)
                            .addAll(List.of(
                                    AttributeModifier.multiply(Identifier.tryParse(ReabsorptionInit.SPELLSUPPRESS.getIdAsString()), 0.15F),
                                    AttributeModifier.multiply(Identifier.tryParse(ReabsorptionInit.GLANCINGBLOW.getIdAsString()), 0.15F)
                            ))
            ),2)
            .armorSet();
    public static final Armor.Set juggernautArmor = Armors.create(
            juggernaut,
            Identifier.of(RPGMinibosses.MOD_ID, "juggernaut"),
            30,
            JuggernautArmor::new,
            ArmorSetConfig.with(
                    new ArmorSetConfig.Piece(3)
                            .addAll(List.of(
                                    AttributeModifier.bonus(Identifier.tryParse(ReabsorptionInit.DEFIANCE.getIdAsString()), 1F)


                            )),
                    new ArmorSetConfig.Piece(8)
                            .addAll(List.of(
                                    AttributeModifier.bonus(Identifier.tryParse(ReabsorptionInit.DEFIANCE.getIdAsString()), 1F)

                            )),
                    new ArmorSetConfig.Piece(6)
                            .addAll(List.of(
                                    AttributeModifier.bonus(Identifier.tryParse(ReabsorptionInit.DEFIANCE.getIdAsString()), 1F)

                            )),
                    new ArmorSetConfig.Piece(3)
                            .addAll(List.of(
                                    AttributeModifier.bonus(Identifier.tryParse(ReabsorptionInit.DEFIANCE.getIdAsString()), 1F)
                            ))
            ),2)
            .armorSet();
    public static void register(){

    }
}

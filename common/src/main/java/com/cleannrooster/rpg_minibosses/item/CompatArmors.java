package com.cleannrooster.rpg_minibosses.item;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Items;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.spell_engine.api.config.ArmorSetConfig;
import net.spell_engine.api.config.AttributeModifier;
import net.spell_engine.rpg_series.item.Armor;
import net.spell_engine.rpg_series.item.Armor.*;

import java.util.List;

import static com.cleannrooster.rpg_minibosses.RPGMinibosses.CONTENT_NAMESPACE;
import static com.cleannrooster.rpg_minibosses.item.Armors.armorentries;

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
            Identifier.of(CONTENT_NAMESPACE, "trickster"),
            30,
            ThiefArmor::new,
            ArmorSetConfig.with(
                    new ArmorSetConfig.Piece(2)
                            .addAll(List.of(
                                    AttributeModifier.multiply(Identifier.of(CONTENT_NAMESPACE, "spellsuppression"), 0.15F),
                                    AttributeModifier.multiply(Identifier.of(CONTENT_NAMESPACE, "glancingblow"), 0.15F)


                            )),
                    new ArmorSetConfig.Piece(6)
                            .addAll(List.of(
                                    AttributeModifier.multiply(Identifier.of(CONTENT_NAMESPACE, "spellsuppression"), 0.15F),
                                    AttributeModifier.multiply(Identifier.of(CONTENT_NAMESPACE, "glancingblow"), 0.15F)

                            )),
                    new ArmorSetConfig.Piece(4)
                            .addAll(List.of(
                                    AttributeModifier.multiply(Identifier.of(CONTENT_NAMESPACE, "spellsuppression"), 0.15F),
                                    AttributeModifier.multiply(Identifier.of(CONTENT_NAMESPACE, "glancingblow"), 0.15F)

                            )),
                    new ArmorSetConfig.Piece(2)
                            .addAll(List.of(
                                    AttributeModifier.multiply(Identifier.of(CONTENT_NAMESPACE, "spellsuppression"), 0.15F),
                                    AttributeModifier.multiply(Identifier.of(CONTENT_NAMESPACE, "glancingblow"), 0.15F)
                            ))
            ),2)
            .armorSet();
    public static final Set juggernautArmor = Armors.create(
            juggernaut,
            Identifier.of(CONTENT_NAMESPACE, "juggernaut"),
            30,
            JuggernautArmor::new,
            ArmorSetConfig.with(
                    new ArmorSetConfig.Piece(3)
                            .addAll(List.of(
                                    AttributeModifier.bonus(Identifier.of(CONTENT_NAMESPACE,"defiance"), 1F)


                            )),
                    new ArmorSetConfig.Piece(8)
                            .addAll(List.of(
                                    AttributeModifier.bonus(Identifier.of(CONTENT_NAMESPACE,"defiance"), 1F)

                            )),
                    new ArmorSetConfig.Piece(6)
                            .addAll(List.of(
                                    AttributeModifier.bonus(Identifier.of(CONTENT_NAMESPACE,"defiance"), 1F)

                            )),
                    new ArmorSetConfig.Piece(3)
                            .addAll(List.of(
                                    AttributeModifier.bonus(Identifier.of(CONTENT_NAMESPACE,"defiance"), 1F)
                            ))
            ),2)
            .armorSet();
    public static void register(){
    }
}

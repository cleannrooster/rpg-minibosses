package com.cleannrooster.rpg_minibosses.item;

import com.cleannrooster.artificers.Artificers;
import com.cleannrooster.artificers.armors.PeasantArmor;
import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.ThiefArmorRenderer;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.extraspellattributes.ReabsorptionInit;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.*;
import net.minecraft.recipe.Ingredient;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.spell_engine.api.config.ArmorSetConfig;
import net.spell_engine.api.config.AttributeModifier;
import net.spell_engine.api.config.WeaponConfig;
import net.spell_engine.api.item.Equipment;
import net.spell_engine.api.item.armor.Armor;
import net.spell_engine.api.item.weapon.Weapon;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class Armors {

    public static Item ABBERRATH ;
    public static Item TABULA ;

    public static RegistryEntry<ArmorMaterial> material(String name,

                                                        int protectionHead, int protectionChest, int protectionLegs, int protectionFeet,
                                                        int enchantability, RegistryEntry<SoundEvent> equipSound, Supplier<Ingredient> repairIngredient) {
        var material = new ArmorMaterial(
                Map.of(
                        ArmorItem.Type.HELMET, protectionHead,
                        ArmorItem.Type.CHESTPLATE, protectionChest,
                        ArmorItem.Type.LEGGINGS, protectionLegs,
                        ArmorItem.Type.BOOTS, protectionFeet),
                enchantability, equipSound, repairIngredient,
                List.of(new ArmorMaterial.Layer(Identifier.of(RPGMinibosses.MOD_ID, name))),
                0,0
        );
        return Registry.registerReference(Registries.ARMOR_MATERIAL, Identifier.of(RPGMinibosses.MOD_ID, name), material);
    }
    public static RegistryKey<ItemGroup> KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(),Identifier.of(RPGMinibosses.MOD_ID,"armor"));

    public static RegistryEntry<ArmorMaterial> abberrath = material(
            "abberrath",
            3, 8, 6, 3,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, () -> Ingredient.ofItems(Items.BLAZE_ROD));

    public static RegistryEntry<ArmorMaterial> juggernaut = material(
            "juggernaut",
            3, 8, 6, 3,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE, () -> Ingredient.ofItems(Items.NETHERITE_SCRAP));

    public static RegistryEntry<ArmorMaterial> trickster = material(
            "trickster",
            2, 6, 4, 2,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_LEATHER, () -> Ingredient.ofItems(Items.LEATHER,Items.IRON_INGOT));

    private static Armor.Entry create(RegistryEntry<ArmorMaterial> material, Identifier id, int durability, Armor.Set.ItemFactory factory, ArmorSetConfig defaults, int tier) {
        var entry = Armor.Entry.create(
                material,
                id,
                durability,
                factory,
                defaults,
                Equipment.LootProperties.of(tier));
        armorentries.add(entry);

        return entry;
    }
    public static ItemGroup RPGARMOR;
    public static final ArrayList<Weapon.Entry> entries = new ArrayList<>();
    public static final ArrayList<Armor.Entry> armorentries = new ArrayList<>();

    private static Weapon.Entry entry(String name, Weapon.CustomMaterial material, Weapon.Factory item, WeaponConfig defaults) {
        return entry(null, name, material, item, defaults);
    }

    private static Weapon.Entry entry(String requiredMod, String name, Weapon.CustomMaterial material, Weapon.Factory item, WeaponConfig defaults) {
        var entry = new Weapon.Entry(RPGMinibosses.MOD_ID, name, material, item, defaults, Equipment.WeaponType.DAMAGE_STAFF);
        if (entry.isRequiredModInstalled()) {
            entries.add(entry);
        }
        return entry;
    }

    public static final Weapon.Entry whispering_ice = whispering_ice(null,"whispering_ice",
            Weapon.CustomMaterial.matching(ToolMaterials.DIAMOND, () -> Ingredient.ofItems(Items.PRISMARINE_CRYSTALS)), 4F, SpellSchools.FROST)
            .attribute(AttributeModifier.bonus((SpellSchools.FROST).id, 7))
            .loot(Equipment.LootProperties.of(6));

    private static Weapon.Entry whispering_ice(String requiredMod, String name, Weapon.CustomMaterial material, float damage, SpellSchool school) {
        var settings = new Item.Settings();
        return entry(requiredMod, name, material, WhisperingIceStaff::new, new WeaponConfig(damage, -3F))
                .loot(Equipment.LootProperties.of(6));
    }
    public static final Armor.Set tricksterArmor = create(
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
    public static final Armor.Set juggernautArmor = create(
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
    public static void register(Map<String, WeaponConfig> configs){
        if ( RPGARMOR == null){
            RPGARMOR = FabricItemGroup.builder()
                    .icon(() -> new ItemStack(ABBERRATH))
                    .displayName(Text.translatable("itemGroup.rpg-minibosses.armors"))
                    .build();
            Registry.register(Registries.ITEM_GROUP, KEY, RPGARMOR);
        }

        ABBERRATH = new AbberrathArmor(abberrath, ArmorItem.Type.BOOTS,new Item.Settings().maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(30)).attributeModifiers(new AttributeModifiersComponent(List.of(
                new AttributeModifiersComponent.Entry(SpellSchools.FIRE.getAttributeEntry(),
                new EntityAttributeModifier(Identifier.of(RPGMinibosses.MOD_ID,"abberraths_hooves"),2, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.FEET),
                new AttributeModifiersComponent.Entry(EntityAttributes.GENERIC_ARMOR,
                        new EntityAttributeModifier(Identifier.of(RPGMinibosses.MOD_ID,"armor_boots"),2, EntityAttributeModifier.Operation.ADD_VALUE), AttributeModifierSlot.FEET)),true)),SpellSchools.FIRE);
         TABULA = Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.MOD_ID,"tabula_rasa"),new Item(new Item.Settings().maxCount(1)));
        Weapon.register(configs, entries, RPGMinibossesEntities.KEY);
        Registry.register(Registries.ITEM,Identifier.of(RPGMinibosses.MOD_ID,"abberraths_hooves"),ABBERRATH);
        ItemGroupEvents.modifyEntriesEvent(RPGMinibossesEntities.KEY).register((content) -> {
            content.add(ABBERRATH);
            content.add(TABULA);

        });
    }
    public static void registerArmors(Map<String, ArmorSetConfig> configs) {
        Armor.register(configs, armorentries, KEY);
    }
}

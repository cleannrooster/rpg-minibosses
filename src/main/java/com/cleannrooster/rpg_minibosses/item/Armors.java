package com.cleannrooster.rpg_minibosses.item;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.ThiefArmorRenderer;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import dev.emi.trinkets.api.SlotType;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.impl.FabricLoaderImpl;
import net.fabricmc.loader.impl.launch.FabricLauncher;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
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
import net.minecraft.util.Rarity;
import net.spell_engine.SpellEngineMod;
import net.spell_engine.api.config.ArmorSetConfig;
import net.spell_engine.api.config.AttributeModifier;
import net.spell_engine.api.config.WeaponConfig;
import net.spell_engine.api.entity.SpellEngineAttributes;
import net.spell_engine.api.item.Equipment;
import net.spell_engine.api.item.armor.Armor;
import net.spell_engine.api.item.armor.ConfigurableAttributes;
import net.spell_engine.api.item.weapon.Weapon;
import net.spell_engine.api.spell.SpellDataComponents;
import net.spell_engine.fabric.FabricMod;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.spell_engine.api.item.set.EquipmentSet.attributesFrom;

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
    public static RegistryEntry<ArmorMaterial> despot = material(
            "despot",
            3, 8, 6, 3,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, () -> Ingredient.ofItems(Items.GOLD_BLOCK));
    public static RegistryEntry<ArmorMaterial> kintsugi = material(
            "kintsugi",
            2, 6, 4, 2,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, () -> Ingredient.ofItems(Items.NETHERITE_SCRAP));
    public static RegistryEntry<ArmorMaterial> foxshade = material(
            "foxshade",
            2, 4, 2, 2,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, () -> Ingredient.ofItems(Items.LEATHER));
    public static RegistryEntry<ArmorMaterial> sanguine_fire = material(
            "sanguine_fire",
            1, 4, 2, 1,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, () -> Ingredient.ofItems(Items.BLAZE_ROD));
    public static RegistryEntry<ArmorMaterial> sanguine_frost = material(
            "sanguine_frost",
            1, 4, 2, 1,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, () -> Ingredient.ofItems(Items.PRISMARINE_CRYSTALS));
    public static RegistryEntry<ArmorMaterial> sanguine_arcane = material(
            "sanguine_arcane",
            1, 4, 2, 1,
            30,
            SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND, () -> Ingredient.ofItems(Items.ENDER_PEARL));


    public static Armor.Entry create(RegistryEntry<ArmorMaterial> material, Identifier id, int durability, Armor.Set.ItemFactory factory, ArmorSetConfig defaults, int tier) {
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
    public static Armor.Entry createUniqueSet(RegistryEntry<ArmorMaterial> material, Identifier id, int durability, Armor.Set.ItemFactory factory, ArmorSetConfig defaults, Equipment.LootProperties lootProperties, @Nullable Armor.ItemSettingsTweaker settingsTweaker) {
        Item.Settings helmetSettings = (new Item.Settings()).maxDamage(ArmorItem.Type.HELMET.getMaxDamage(durability));
        Item.Settings chestplateSettings = (new Item.Settings()).maxDamage(ArmorItem.Type.CHESTPLATE.getMaxDamage(durability));
        Item.Settings leggingsSettings = (new Item.Settings()).maxDamage(ArmorItem.Type.LEGGINGS.getMaxDamage(durability));
        Item.Settings bootsSettings = (new Item.Settings()).maxDamage(ArmorItem.Type.BOOTS.getMaxDamage(durability));
        if (settingsTweaker != null) {
            settingsTweaker.chestplate().accept(chestplateSettings);
            settingsTweaker.leggings().accept(leggingsSettings);
        }
        int tier = lootProperties.tier();
        if (tier >= 3) {
            helmetSettings.fireproof();
            chestplateSettings.fireproof();
            leggingsSettings.fireproof();
            bootsSettings.fireproof();
        }

        Armor.Set set = new Armor.Set(id.getNamespace(), id.getPath(), null, factory.create(material, ArmorItem.Type.CHESTPLATE, chestplateSettings), factory.create(material, ArmorItem.Type.LEGGINGS, leggingsSettings), null);
        return new Armor.Entry(material, set, defaults, lootProperties);
    }
    private static Armor.ItemSettingsTweaker commonSettings(Identifier equipmentSetId) {
        return Armor.ItemSettingsTweaker.standard(itemSettings -> {
            itemSettings
                    .component(SpellDataComponents.EQUIPMENT_SET, equipmentSetId)
                    .component(DataComponentTypes.RARITY, Rarity.RARE);
        });
    }

    public static Armor.Entry createUniqueSet(RegistryEntry<ArmorMaterial> material, Identifier id, int durability, Armor.Set.ItemFactory factory, ArmorSetConfig defaults) {
        return createUniqueSet(material, id, durability, factory, defaults, Equipment.LootProperties.EMPTY);
    }

    public static Armor.Entry createUniqueSet(RegistryEntry<ArmorMaterial> material, Identifier id, int durability, Armor.Set.ItemFactory factory, ArmorSetConfig defaults, Equipment.LootProperties lootProperties) {
        return createUniqueSet(material, id, durability, factory, defaults, lootProperties, (Armor.ItemSettingsTweaker)null);
    }

    public static Armor.Entry createUniqueSet(RegistryEntry<ArmorMaterial> material, Identifier id, int durability, Armor.Set.ItemFactory factory, ArmorSetConfig defaults, Armor.ItemSettingsTweaker tweaker) {
        var entry = Armors.createUniqueSet(
                material,
                id,
                durability,
                factory,
                defaults,
                Equipment.LootProperties.of(5),
                tweaker);
        armorentries.add(entry);

        return entry;
    }
    private static Weapon.Entry entry(String requiredMod, String name, Weapon.CustomMaterial material, Weapon.Factory item, WeaponConfig defaults) {
        var entry = new Weapon.Entry(RPGMinibosses.MOD_ID, name, material, item, defaults, Equipment.WeaponType.DAMAGE_STAFF);
        if (entry.isRequiredModInstalled()) {
            entries.add(entry);
        }
        return entry;
    }
    public static final Armor.Set despotArmor = Armors.createUniqueSet(
                    despot,
                    Identifier.of(RPGMinibosses.MOD_ID, "despot"),
                    30,
                    UniqueArmor::new,
                    ArmorSetConfig.with(
                            null    ,
                            new ArmorSetConfig.Piece(8)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(Identifier.tryParse(EntityAttributes.GENERIC_ATTACK_DAMAGE.getIdAsString()), 0.25F)

                                    )),
                            new ArmorSetConfig.Piece(6)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(Identifier.tryParse(EntityAttributes.GENERIC_ATTACK_DAMAGE.getIdAsString()), 0.25F)

                                    )),
                            null
                    ),commonSettings(SetBonuses.despot.id()))
            .armorSet();
    public static final Armor.Set foxArmor = Armors.createUniqueSet(
                    foxshade,
                    Identifier.of(RPGMinibosses.MOD_ID, "foxshade"),
                    30,
                    UniqueArmor::new,
                    ArmorSetConfig.with(
                            null    ,
                            new ArmorSetConfig.Piece(6)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(SpellEngineAttributes.EVASION_CHANCE.id, 0.1F)

                                    )),
                            new ArmorSetConfig.Piece(4)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(SpellEngineAttributes.EVASION_CHANCE.id, 0.1F)
                                    )),
                            null
                    ),commonSettings(SetBonuses.foxshade.id()))
            .armorSet();
    public static final Armor.Set kintsugiArmor = Armors.createUniqueSet(
                    kintsugi,
                    Identifier.of(RPGMinibosses.MOD_ID, "kintsugi"),
                    30,
                    UniqueArmor::new,
                    ArmorSetConfig.with(
                            null    ,
                            new ArmorSetConfig.Piece(7)
                                    .addAll(List.of(

                                    )),
                            new ArmorSetConfig.Piece(5)
                                    .addAll(List.of(
                                    )),
                            null
                    ), commonSettings(SetBonuses.kintsugi.id()))
            .armorSet();
    public static final Armor.Set sanguine_red = Armors.createUniqueSet(
                    sanguine_fire,
                    Identifier.of(RPGMinibosses.MOD_ID, "sanguine_fire"),
                    30,
                    UniqueArmor::new,
                    ArmorSetConfig.with(
                            null    ,
                            new ArmorSetConfig.Piece(5)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(SpellSchools.FIRE.id, 0.25F)
                                    )),
                            new ArmorSetConfig.Piece(3)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(SpellSchools.FIRE.id, 0.25F)
                                    )),
                            null
                    ),commonSettings(SetBonuses.savant_red.id()))
            .armorSet();
    public static final Armor.Set sanguine_blue = Armors.createUniqueSet(
                    sanguine_frost,
                    Identifier.of(RPGMinibosses.MOD_ID, "sanguine_frost"),
                    30,
                    UniqueArmor::new,
                    ArmorSetConfig.with(
                            null    ,
                            new ArmorSetConfig.Piece(5)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(SpellSchools.FROST.id, 0.25F)
                                    )),
                            new ArmorSetConfig.Piece(3)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(SpellSchools.FROST.id, 0.25F)
                                    )),
                            null
                    ),commonSettings(SetBonuses.savant_blue.id()))
            .armorSet();

    public static final Armor.Set sanguine_purple = Armors.createUniqueSet(
                    sanguine_arcane,
                    Identifier.of(RPGMinibosses.MOD_ID, "sanguine_arcane"),
                    30,
                    UniqueArmor::new,
                    ArmorSetConfig.with(
                            null    ,
                            new ArmorSetConfig.Piece(5)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(SpellSchools.ARCANE.id, 0.25F)
                                    )),
                            new ArmorSetConfig.Piece(3)
                                    .addAll(List.of(
                                            AttributeModifier.multiply(SpellSchools.ARCANE.id, 0.25F)
                                    )),
                            null
                    ), commonSettings(SetBonuses.savant_purple.id()))
            .armorSet();
    public static final Weapon.Entry whispering_ice = whispering_ice(null,"whispering_ice",
            Weapon.CustomMaterial.matching(ToolMaterials.DIAMOND, () -> Ingredient.ofItems(Items.PRISMARINE_CRYSTALS)), 4F, SpellSchools.FROST)
            .attribute(AttributeModifier.bonus((SpellSchools.FROST).id, 7))
            .loot(Equipment.LootProperties.of(6));

    private static Weapon.Entry whispering_ice(String requiredMod, String name, Weapon.CustomMaterial material, float damage, SpellSchool school) {
        var settings = new Item.Settings();
        return entry(requiredMod, name, material, WhisperingIceStaff::new, new WeaponConfig(damage, -3F))
                .loot(Equipment.LootProperties.of(6));
    }

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
        ItemGroupEvents.modifyEntriesEvent(KEY).register((content) -> {
            content.add(ABBERRATH);
            content.add(TABULA);

        });
    }
    public static void registerArmors(Map<String, ArmorSetConfig> configs) {
        if(FabricLoader.getInstance().isModLoaded("extraspellattributes")){
            CompatArmors.register();
        }
        Armors.register(configs, armorentries, KEY);
    }
    public static void register(Map<String, ArmorSetConfig> configs, List<Armor.Entry> entries, RegistryKey<ItemGroup> itemGroupKey) {
        for(Armor.Entry entry : entries) {
            ArmorSetConfig config = (ArmorSetConfig)configs.get(entry.name());
            if (config == null) {
                config = entry.defaults();
                configs.put(entry.name(), config);
            }

            for(Object piece : entry.armorSet().pieces()) {
                if(piece != null && !(piece instanceof UniqueArmor uniqueArmor && (uniqueArmor.getSlotType().equals(EquipmentSlot.HEAD) || uniqueArmor.getSlotType().equals(EquipmentSlot.FEET)) )) {
                    EquipmentSlot slot = ((ArmorItem) piece).getSlotType();
                    ((ConfigurableAttributes) piece).setAttributes(attributesFrom(config, ((ArmorItem) piece).getType()));
                }
            }
                register(entry.armorSet(), itemGroupKey);

        }

    }
    private static AttributeModifiersComponent attributesFrom(ArmorSetConfig config, ArmorItem.Type slot) {
        ArmorSetConfig.Piece piece = null;
        Identifier modifierId = Identifier.ofVanilla("armor." + slot.getName());
        switch (slot) {
            case BOOTS -> piece = config.feet;
            case LEGGINGS -> piece = config.legs;
            case CHESTPLATE -> piece = config.chest;
            case HELMET -> piece = config.head;
        }

        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
        AttributeModifierSlot attributeModifierSlot = AttributeModifierSlot.forEquipmentSlot(slot.getEquipmentSlot());
        if (config.armor_toughness != 0.0F) {
            builder.add(EntityAttributes.GENERIC_ARMOR_TOUGHNESS, new EntityAttributeModifier(modifierId, (double)config.armor_toughness, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        }

        if (config.knockback_resistance != 0.0F) {
            builder.add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE, new EntityAttributeModifier(modifierId, (double)config.knockback_resistance, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        }

        if (piece != null && piece.armor != 0) {
            builder.add(EntityAttributes.GENERIC_ARMOR, new EntityAttributeModifier(modifierId, (double)piece.armor, EntityAttributeModifier.Operation.ADD_VALUE), attributeModifierSlot);
        }
        if(piece != null) {
            for (AttributeModifier attribute : piece.attributes) {
                try {
                    RegistryEntry.Reference<EntityAttribute> entityAttribute = (RegistryEntry.Reference) Registries.ATTRIBUTE.getEntry(Identifier.of(attribute.attribute)).get();
                    builder.add(entityAttribute, new EntityAttributeModifier(modifierId, (double) attribute.value, attribute.operation), attributeModifierSlot);
                } catch (Exception e) {
                    System.err.println("Failed to add item attribute modifier: " + e.getMessage());
                }
            }

        }

        return builder.build();
    }
    public static void register(Armor.Set set, RegistryKey<ItemGroup> itemGroupKey) {
        for(Object piece : set.pieces()) {
            if(piece instanceof ArmorItem armorItem) {

                    Registry.register(Registries.ITEM, set.idOf(armorItem), armorItem);

            }
        }

        ItemGroupEvents.modifyEntriesEvent(itemGroupKey).register((ItemGroupEvents.ModifyEntries)(content) -> {
            for(Object piece : set.pieces()) {
                if(piece instanceof ArmorItem armorItem) {
                    if ( !(piece instanceof UniqueArmor uniqueArmor && (uniqueArmor.getSlotType().equals(EquipmentSlot.HEAD) || uniqueArmor.getSlotType().equals(EquipmentSlot.FEET)))) {

                        content.add(armorItem);
                    }
                }
            }

        });
    }

}

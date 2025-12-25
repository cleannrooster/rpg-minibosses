package com.cleannrooster.rpg_minibosses.data;

import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagProvider;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.spell_engine.api.item.Equipment;
import net.spell_engine.api.item.armor.Armor;
import net.spell_engine.api.item.weapon.Weapon;
import net.spell_engine.api.tags.SpellEngineItemTags;
import net.spell_engine.rpg_series.datagen.RPGSeriesDataGen;
import net.spell_engine.rpg_series.tags.RPGSeriesItemTags;
import net.spell_power.api.SpellPowerTags;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RPGMINIDatagen {
    @SafeVarargs
    public static <E> List<E> combine(List<E>... smallLists) {
        ArrayList<E> bigList = new ArrayList();

        for(List<E> list : smallLists) {
            bigList.addAll(list);
        }

        return bigList;
    }

    public static class BaselineTagGenerator extends FabricTagProvider<Item> {
        public BaselineTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, RegistryKeys.ITEM, registriesFuture);
        }

        protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {
            for(Map.Entry<Equipment.WeaponType, TagKey<Item>> entry : RPGSeriesItemTags.WeaponType.ALL.entrySet()) {
                this.getOrCreateTagBuilder((TagKey)entry.getValue());
            }

            for(RPGSeriesItemTags.RoleArchetype archetype : RPGSeriesItemTags.RoleArchetype.values()) {
                FabricTagProvider<Item>.FabricTagBuilder tag = this.getOrCreateTagBuilder(RPGSeriesItemTags.Archetype.tag(archetype));

                for(Map.Entry<Equipment.WeaponType, TagKey<Item>> entry : RPGSeriesItemTags.WeaponType.ALL.entrySet()) {
                    if (RPGSeriesItemTags.Archetype.classify((Equipment.WeaponType)entry.getKey()) == archetype) {
                        tag.addTag((TagKey)entry.getValue());
                    }
                }
            }

            for(Map.Entry<RPGSeriesItemTags.LootTheme, TagKey<Item>> entry : RPGSeriesItemTags.LootThemes.ALL.entrySet()) {
                this.getOrCreateTagBuilder((TagKey)entry.getValue());
            }

            for(int i = 0; i < 10; ++i) {
                for(RPGSeriesItemTags.LootCategory category : RPGSeriesItemTags.LootCategory.values()) {
                    this.getOrCreateTagBuilder(RPGSeriesItemTags.LootTiers.get(i, category));
                }
            }

            for(Map.Entry<RPGSeriesItemTags.ArmorMetaType, TagKey<Item>> entry : RPGSeriesItemTags.ArmorType.ALL.entrySet()) {
                this.getOrCreateTagBuilder((TagKey)entry.getValue());
            }

            List<Equipment.WeaponType> fullSpellWeaponTypes = List.of(net.spell_engine.api.item.Equipment.WeaponType.DAMAGE_STAFF, net.spell_engine.api.item.Equipment.WeaponType.DAMAGE_WAND, net.spell_engine.api.item.Equipment.WeaponType.HEALING_STAFF, net.spell_engine.api.item.Equipment.WeaponType.HEALING_WAND, net.spell_engine.api.item.Equipment.WeaponType.SPELL_BLADE, net.spell_engine.api.item.Equipment.WeaponType.SPELL_SCYTHE);
            List<Equipment.WeaponType> meleeSpellWeaponTypes = List.of(net.spell_engine.api.item.Equipment.WeaponType.SWORD, net.spell_engine.api.item.Equipment.WeaponType.CLAYMORE, net.spell_engine.api.item.Equipment.WeaponType.MACE, net.spell_engine.api.item.Equipment.WeaponType.HAMMER, net.spell_engine.api.item.Equipment.WeaponType.GLAIVE);
            List<Equipment.WeaponType> spellInfinityTypes = RPGSeriesDataGen.<Equipment.WeaponType>combine(fullSpellWeaponTypes, meleeSpellWeaponTypes);
            FabricTagProvider<Item>.FabricTagBuilder spellInfinityTag = this.getOrCreateTagBuilder(SpellEngineItemTags.ENCHANTABLE_SPELL_INFINITY);

            for(Equipment.WeaponType type : spellInfinityTypes) {
                spellInfinityTag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

            FabricTagProvider<Item>.FabricTagBuilder spellHasteTag = this.getOrCreateTagBuilder(SpellPowerTags.Items.Enchantable.HASTE);

            for(Equipment.WeaponType type : fullSpellWeaponTypes) {
                spellHasteTag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

            FabricTagProvider<Item>.FabricTagBuilder criticalDamageTag = this.getOrCreateTagBuilder(SpellPowerTags.Items.Enchantable.CRITICAL_DAMAGE);

            for(Equipment.WeaponType type : fullSpellWeaponTypes) {
                criticalDamageTag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

            List<Equipment.WeaponType> spellPowerTypes = RPGSeriesDataGen.<Equipment.WeaponType>combine(fullSpellWeaponTypes, meleeSpellWeaponTypes);
            FabricTagProvider<Item>.FabricTagBuilder spellPowerTag = this.getOrCreateTagBuilder(SpellPowerTags.Items.Enchantable.SPELL_POWER_GENERIC);

            for(Equipment.WeaponType type : spellPowerTypes) {
                spellPowerTag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

            FabricTagProvider<Item>.FabricTagBuilder spellVolatilityTag = this.getOrCreateTagBuilder(SpellPowerTags.Items.Enchantable.CRITICAL_CHANCE);
            spellVolatilityTag.addTag(RPGSeriesItemTags.ArmorType.get(RPGSeriesItemTags.ArmorMetaType.MAGIC));
            Equipment.WeaponType[] unbreakingTypes = net.spell_engine.api.item.Equipment.WeaponType.values();
            FabricTagProvider<Item>.FabricTagBuilder unbreakingTag = this.getOrCreateTagBuilder(ItemTags.DURABILITY_ENCHANTABLE);

            for(Equipment.WeaponType type : unbreakingTypes) {
                unbreakingTag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

            List<Equipment.WeaponType> sharpWeaponTypes = List.of(net.spell_engine.api.item.Equipment.WeaponType.SWORD, net.spell_engine.api.item.Equipment.WeaponType.SPEAR, net.spell_engine.api.item.Equipment.WeaponType.CLAYMORE, net.spell_engine.api.item.Equipment.WeaponType.MACE, net.spell_engine.api.item.Equipment.WeaponType.HAMMER, net.spell_engine.api.item.Equipment.WeaponType.DAGGER, net.spell_engine.api.item.Equipment.WeaponType.SICKLE, net.spell_engine.api.item.Equipment.WeaponType.DOUBLE_AXE, net.spell_engine.api.item.Equipment.WeaponType.GLAIVE, net.spell_engine.api.item.Equipment.WeaponType.SPELL_BLADE, net.spell_engine.api.item.Equipment.WeaponType.SPELL_SCYTHE);
            FabricTagProvider<Item>.FabricTagBuilder sharpTag = this.getOrCreateTagBuilder(ItemTags.SHARP_WEAPON_ENCHANTABLE);

            for(Equipment.WeaponType type : sharpWeaponTypes) {
                sharpTag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

            List<Equipment.WeaponType> meleeWeaponTypes = List.of(net.spell_engine.api.item.Equipment.WeaponType.SWORD, net.spell_engine.api.item.Equipment.WeaponType.CLAYMORE, net.spell_engine.api.item.Equipment.WeaponType.MACE, net.spell_engine.api.item.Equipment.WeaponType.HAMMER, net.spell_engine.api.item.Equipment.WeaponType.SPEAR, net.spell_engine.api.item.Equipment.WeaponType.DAGGER, net.spell_engine.api.item.Equipment.WeaponType.SICKLE, net.spell_engine.api.item.Equipment.WeaponType.DOUBLE_AXE, net.spell_engine.api.item.Equipment.WeaponType.GLAIVE);
            FabricTagProvider<Item>.FabricTagBuilder meleeTag = this.getOrCreateTagBuilder(ItemTags.SWORDS);

            for(Equipment.WeaponType type : meleeWeaponTypes) {
                meleeTag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

            for(Equipment.WeaponType type : List.of(net.spell_engine.api.item.Equipment.WeaponType.SHORT_BOW, net.spell_engine.api.item.Equipment.WeaponType.LONG_BOW)) {
                FabricTagProvider<Item>.FabricTagBuilder tag = this.getOrCreateTagBuilder(ItemTags.BOW_ENCHANTABLE);
                tag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

            for(Equipment.WeaponType type : List.of(net.spell_engine.api.item.Equipment.WeaponType.RAPID_CROSSBOW, net.spell_engine.api.item.Equipment.WeaponType.HEAVY_CROSSBOW)) {
                FabricTagProvider<Item>.FabricTagBuilder tag = this.getOrCreateTagBuilder(ItemTags.CROSSBOW_ENCHANTABLE);
                tag.addTag(RPGSeriesItemTags.WeaponType.get(type));
            }

        }
    }

    public static record ShieldEntry(Identifier id, Equipment.LootProperties lootProperties) {
    }

    public static record BowEntry(Identifier id, Equipment.WeaponType weaponType, Equipment.LootProperties lootProperties) {
    }

    public abstract static class ItemTagGenerator extends FabricTagProvider<Item> {
        public ItemTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, RegistryKeys.ITEM, registriesFuture);
        }

        public void generateWeaponTags(List<Weapon.Entry> weapons) {
            for(Weapon.Entry weapon : weapons) {
                TagKey<Item> weaponType = RPGSeriesItemTags.WeaponType.get(weapon.category());
                FabricTagProvider<Item>.FabricTagBuilder weaponTag = this.getOrCreateTagBuilder(weaponType);
                weaponTag.addOptional(weapon.id());
                int tier = weapon.lootProperties().tier();
                if (tier >= 0) {
                    FabricTagProvider<Item>.FabricTagBuilder tierTag = this.getOrCreateTagBuilder(RPGSeriesItemTags.LootTiers.get(tier, RPGSeriesItemTags.LootCategory.WEAPONS));
                    tierTag.addOptional(weapon.id());
                }

                String lootTheme = weapon.lootProperties().theme();
                if (lootTheme != null && !lootTheme.isEmpty()) {
                    FabricTagProvider<Item>.FabricTagBuilder themeTag = this.getOrCreateTagBuilder(RPGSeriesItemTags.LootThemes.get(lootTheme));
                    themeTag.addOptional(weapon.id());
                }
            }

        }

        public void generateArmorTags(List<Armor.Entry> armors) {
            this.generateArmorTags(armors, EnumSet.noneOf(RPGSeriesItemTags.ArmorMetaType.class));
        }

        public void generateArmorTags(List<Armor.Entry> armors, RPGSeriesDataGen.ItemTagGenerator.ArmorOptions options) {
            this.generateArmorTags(armors, EnumSet.noneOf(RPGSeriesItemTags.ArmorMetaType.class), options);
        }

        public void generateArmorTags(List<Armor.Entry> armors, RPGSeriesItemTags.ArmorMetaType metaType) {
            this.generateArmorTags(armors, EnumSet.of(metaType));
        }

        public void generateArmorTags(List<Armor.Entry> armors, RPGSeriesItemTags.ArmorMetaType metaType, RPGSeriesDataGen.ItemTagGenerator.ArmorOptions options) {
            this.generateArmorTags(armors, EnumSet.of(metaType), options);
        }

        public void generateArmorTags(List<Armor.Entry> armors, EnumSet<RPGSeriesItemTags.ArmorMetaType> metaTypes) {
            this.generateArmorTags(armors, metaTypes, RPGSeriesDataGen.ItemTagGenerator.ArmorOptions.DEFAULT);
        }

        public void generateArmorTags(List<Armor.Entry> armors, EnumSet<RPGSeriesItemTags.ArmorMetaType> metaTypes, RPGSeriesDataGen.ItemTagGenerator.ArmorOptions options) {
            for(Armor.Entry armor : armors) {
                Armor.Set set = armor.armorSet();

                FabricTagProvider<Item>.FabricTagBuilder chestTag = this.getOrCreateTagBuilder(ItemTags.CHEST_ARMOR);
                chestTag.add(set.chest);
                FabricTagProvider<Item>.FabricTagBuilder legsTag = this.getOrCreateTagBuilder(ItemTags.LEG_ARMOR);
                legsTag.add(set.legs);

                int tier = armor.lootProperties().tier();
                if (options.allowLootTierTags() && tier >= 0) {
                    FabricTagProvider<Item>.FabricTagBuilder tierTag = this.getOrCreateTagBuilder(RPGSeriesItemTags.LootTiers.get(tier, RPGSeriesItemTags.LootCategory.ARMORS));

                    for(Object id : armor.armorSet().pieceIds()) {
                        tierTag.addOptional((Identifier)id);
                    }
                }

                String lootTheme = armor.lootProperties().theme();
                if (options.allowLootThemeTags() && lootTheme != null && !lootTheme.isEmpty()) {
                    FabricTagProvider<Item>.FabricTagBuilder themeTag = this.getOrCreateTagBuilder(RPGSeriesItemTags.LootThemes.get(lootTheme));

                    for(Object id : armor.armorSet().pieceIds()) {
                        themeTag.addOptional((Identifier)id);
                    }
                }

                for(RPGSeriesItemTags.ArmorMetaType metaType : metaTypes) {
                    FabricTagProvider<Item>.FabricTagBuilder metaTag = this.getOrCreateTagBuilder(RPGSeriesItemTags.ArmorType.get(metaType));

                    for(Object id : armor.armorSet().pieceIds()) {
                        metaTag.addOptional((Identifier)id);
                    }
                }
            }

        }

        public void generateBowTags(List<RPGSeriesDataGen.BowEntry> bows) {
            for(RPGSeriesDataGen.BowEntry entry : bows) {
                Identifier id = entry.id();
                TagKey<Item> weaponType = RPGSeriesItemTags.WeaponType.get(entry.weaponType());
                FabricTagProvider<Item>.FabricTagBuilder weaponTag = this.getOrCreateTagBuilder(weaponType);
                weaponTag.addOptional(id);
            }

            this.generateLootTags((Map)bows.stream().collect(Collectors.toMap(RPGSeriesDataGen.BowEntry::id, RPGSeriesDataGen.BowEntry::lootProperties)), RPGSeriesItemTags.LootCategory.WEAPONS);
        }

        public void generateShieldTags(List<RPGSeriesDataGen.ShieldEntry> shields) {
            for(RPGSeriesDataGen.ShieldEntry entry : shields) {
                Identifier id = entry.id();
                TagKey<Item> weaponType = RPGSeriesItemTags.WeaponType.get(net.spell_engine.api.item.Equipment.WeaponType.SHIELD);
                FabricTagProvider<Item>.FabricTagBuilder weaponTag = this.getOrCreateTagBuilder(weaponType);
                weaponTag.addOptional(id);
            }

            this.generateLootTags((Map)shields.stream().collect(Collectors.toMap(RPGSeriesDataGen.ShieldEntry::id, RPGSeriesDataGen.ShieldEntry::lootProperties)), RPGSeriesItemTags.LootCategory.WEAPONS);
        }

        public void generateAccessoryTags(Map<Identifier, Equipment.LootProperties> accessories) {
            this.generateLootTags(accessories, RPGSeriesItemTags.LootCategory.ACCESSORIES);
        }

        public void generateRelicTags(Map<Identifier, Equipment.LootProperties> relics) {
            this.generateLootTags(relics, RPGSeriesItemTags.LootCategory.RELICS);
        }

        public void generateLootTags(Map<Identifier, Equipment.LootProperties> items, RPGSeriesItemTags.LootCategory category) {
            for(Map.Entry<Identifier, Equipment.LootProperties> entry : items.entrySet()) {
                Identifier id = (Identifier)entry.getKey();
                Equipment.LootProperties lootProperties = (Equipment.LootProperties)entry.getValue();
                int tier = lootProperties.tier();
                if (tier >= 0) {
                    FabricTagProvider<Item>.FabricTagBuilder tierTag = this.getOrCreateTagBuilder(RPGSeriesItemTags.LootTiers.get(tier, category));
                    tierTag.addOptional(id);
                }

                String lootTheme = lootProperties.theme();
                if (lootTheme != null && !lootTheme.isEmpty()) {
                    FabricTagProvider<Item>.FabricTagBuilder themeTag = this.getOrCreateTagBuilder(RPGSeriesItemTags.LootThemes.get(lootTheme));
                    themeTag.addOptional(id);
                }
            }

        }

        public static record ArmorOptions(boolean allowLootTierTags, boolean allowLootThemeTags) {
            public static final RPGSeriesDataGen.ItemTagGenerator.ArmorOptions DEFAULT = new RPGSeriesDataGen.ItemTagGenerator.ArmorOptions(true, true);
        }
    }
}

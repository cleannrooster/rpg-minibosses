package com.cleannrooster.rpg_minibosses.data;

import com.cleannrooster.rpg_minibosses.item.Armors;
import com.cleannrooster.rpg_minibosses.item.SetBonuses;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.item.Item;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import net.spell_engine.api.item.set.EquipmentSet;
import net.spell_engine.api.item.set.EquipmentSetRegistry;
import net.spell_engine.rpg_series.datagen.RPGSeriesDataGen;
import net.spell_engine.rpg_series.tags.RPGSeriesItemTags;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
public class DataGenRPG implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();
        pack.addProvider(ItemTagGenerator::new);

        pack.addProvider(EquipmentSetGenerator::new);


    }
    private static List<Item> allArmorPieces() {
        var items = new ArrayList<Item>();
        for (var entry: Armors.armorentries) {
            entry.armorSet().pieces().forEach(item -> {
                items.add((Item)item);
            });
        }
        return items;
    }
    public static class ItemTagGenerator extends RPGMINIDatagen.ItemTagGenerator {
        public ItemTagGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup wrapperLookup) {

            generateArmorTags(
                    Armors.armorentries.stream().filter(entry -> entry.material().getIdAsString().contains("armor")).toList(),
                    RPGSeriesItemTags.ArmorMetaType.MELEE
            );
            generateArmorTags(
                    Armors.armorentries.stream().filter(entry -> entry.material().getIdAsString().contains("robe")).toList(),
                    RPGSeriesItemTags.ArmorMetaType.MAGIC
            );


        }
    }


    public static class EquipmentSetGenerator extends FabricDynamicRegistryProvider {

        public EquipmentSetGenerator(FabricDataOutput output, CompletableFuture<RegistryWrapper.WrapperLookup> registriesFuture) {
            super(output, registriesFuture);
        }

        @Override
        protected void configure(RegistryWrapper.WrapperLookup registries, Entries entries) {
            var itemLookup = registries.createRegistryLookup().getOrThrow(RegistryKeys.ITEM);
            for (var set : SetBonuses.all) {
                var items = RegistryEntryList.of(
                        set.itemSupplier().get().stream()
                                .map(id -> itemLookup.getOrThrow(RegistryKey.of(RegistryKeys.ITEM, id)))
                                .toList()
                );
                entries.add(
                        RegistryKey.of(EquipmentSetRegistry.KEY, set.id()),
                        new EquipmentSet.Definition(
                                set.id().getPath(),
                                items,
                                set.bonuses()
                        )
                );
            }
        }

        @Override
        public String getName() {
            return "Equipment Set Generator";
        }
    }
}
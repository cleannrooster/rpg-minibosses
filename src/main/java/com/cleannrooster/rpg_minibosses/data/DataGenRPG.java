package com.cleannrooster.rpg_minibosses.data;

import com.cleannrooster.rpg_minibosses.item.SetBonuses;
import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricDynamicRegistryProvider;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntryList;
import net.spell_engine.api.item.set.EquipmentSet;
import net.spell_engine.api.item.set.EquipmentSetRegistry;

import java.util.concurrent.CompletableFuture;
public class DataGenRPG implements DataGeneratorEntrypoint {
    @Override
    public void onInitializeDataGenerator(FabricDataGenerator fabricDataGenerator) {
        FabricDataGenerator.Pack pack = fabricDataGenerator.createPack();

        pack.addProvider(EquipmentSetGenerator::new);

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
package com.cleannrooster.rpg_minibosses.config;

import com.cleannrooster.rpg_minibosses.item.Armors;
import net.fabric_extras.structure_pool.api.StructurePoolConfig;
import net.spell_engine.api.config.ConfigFile;
import net.spell_engine.api.item.armor.Armor;
import net.spell_engine.api.item.weapon.Weapon;

import java.util.*;
import java.util.stream.Collectors;

public class Default {
    public Default() {
    }

    public static final ConfigFile.Equipment itemConfig = new ConfigFile.Equipment();

    public final static StructurePoolConfig villageConfig;
    static {

        villageConfig = new StructurePoolConfig();
        var limit = 1;
        villageConfig.entries.addAll(List.of(
                new StructurePoolConfig.Entry("minecraft:village/desert/houses", new ArrayList<>(Arrays.asList(new StructurePoolConfig.Entry.Structure("rpg-minibosses:village/generic/wanted", 10, limit))
                )),
                new StructurePoolConfig.Entry("minecraft:village/savanna/houses", "rpg-rpg-minibosses:village/generic/wanted", 10, limit),

                new StructurePoolConfig.Entry("minecraft:village/plains/houses", "rpg-minibosses:village/generic/wanted", 10, limit),

                new StructurePoolConfig.Entry("minecraft:village/taiga/houses", "rpg-minibosses:village/generic/wanted", 10, limit),

                new StructurePoolConfig.Entry("minecraft:village/snowy/houses", new ArrayList<>(Arrays.asList(
                        new StructurePoolConfig.Entry.Structure("rpg-minibosses:village/generic/wanted", 10, limit))
                ))
        ));
        Iterator var0 = Armors.entries.iterator();

        while(var0.hasNext()) {
            Weapon.Entry weapon = (Weapon.Entry)var0.next();
            itemConfig.weapons.put(weapon.name(), weapon.defaults());
        }




        String var3 = "weapons";
    }

    @SafeVarargs
    private static <T> List<T> joinLists(List<T>... lists) {
        return Arrays.stream(lists).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
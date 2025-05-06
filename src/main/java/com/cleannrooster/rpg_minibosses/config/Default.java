package com.cleannrooster.rpg_minibosses.config;

import net.fabric_extras.structure_pool.api.StructurePoolConfig;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Default {
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
    }

    @SafeVarargs
    private static <T> List<T> joinLists(List<T>... lists) {
        return Arrays.stream(lists).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
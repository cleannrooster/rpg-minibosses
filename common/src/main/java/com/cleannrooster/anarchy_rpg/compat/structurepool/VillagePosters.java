package com.cleannrooster.anarchy_rpg.compat.structurepool;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.compat.StructurePoolCompat;
import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabric_extras.structure_pool.api.StructurePoolConfig;
import net.tiny_config.ConfigManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The wanted-poster house in villages, through Structure Pool API. Loaded reflectively by
 * {@link StructurePoolCompat} only when that mod is installed.
 */
public final class VillagePosters implements StructurePoolCompat.Hooks {
    static final StructurePoolConfig DEFAULTS;
    static {

        DEFAULTS = new StructurePoolConfig();
        var limit = 1;
        DEFAULTS.entries.addAll(List.of(
                new StructurePoolConfig.Entry("minecraft:village/desert/houses", new ArrayList<>(Arrays.asList(new StructurePoolConfig.Entry.Structure("anarchy_rpg:village/generic/wanted", 10, limit))
                )),
                new StructurePoolConfig.Entry("minecraft:village/savanna/houses", "anarchy_rpg:village/generic/wanted", 10, limit),

                new StructurePoolConfig.Entry("minecraft:village/plains/houses", "anarchy_rpg:village/generic/wanted", 10, limit),

                new StructurePoolConfig.Entry("minecraft:village/taiga/houses", "anarchy_rpg:village/generic/wanted", 10, limit),

                new StructurePoolConfig.Entry("minecraft:village/snowy/houses", new ArrayList<>(Arrays.asList(
                        new StructurePoolConfig.Entry.Structure("anarchy_rpg:village/generic/wanted", 10, limit))
                ))
        ));

    }


    private final ConfigManager<StructurePoolConfig> config = new ConfigManager<>("villages", DEFAULTS)
            .builder()
            .setDirectory(RPGMinibosses.CONTENT_NAMESPACE)
            .sanitize(true)
            .build();

    public VillagePosters() {
    }

    @Override
    public void initConfig() {
        this.config.refresh();
    }

    @Override
    public void injectVillages() {
        StructurePoolAPI.injectAll(this.config.value);
    }
}

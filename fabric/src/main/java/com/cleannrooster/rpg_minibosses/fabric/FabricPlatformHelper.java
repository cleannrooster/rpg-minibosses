package com.cleannrooster.rpg_minibosses.fabric;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.platform.PlatformHelper;
import me.emafire003.dev.structureplacerapi.StructurePlacerAPI;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/** Fabric {@link PlatformHelper}: native mod-loaded check + StructurePlacerAPI guild-hall placement. */
public final class FabricPlatformHelper implements PlatformHelper {
    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean placeGuildHall(ServerWorld world, BlockPos origin) {
        StructurePlacerAPI api = new StructurePlacerAPI(world, RPGMinibosses.id("guild_hall"), origin);
        api.loadStructure();
        return true;
    }
}

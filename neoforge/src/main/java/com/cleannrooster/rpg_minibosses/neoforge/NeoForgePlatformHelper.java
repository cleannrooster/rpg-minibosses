package com.cleannrooster.rpg_minibosses.neoforge;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.platform.PlatformHelper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.neoforged.fml.ModList;

/**
 * NeoForge {@link PlatformHelper}: native mod-loaded check.
 *
 * <p>StructurePlacerAPI has no NeoForge build, so {@link #placeGuildHall} does not have a drop-in
 * equivalent. Per the migration decision it logs a clear, actionable warning rather than silently
 * doing nothing. (A vanilla {@code StructureTemplateManager} fallback is the planned follow-up.)</p>
 */
public final class NeoForgePlatformHelper implements PlatformHelper {
    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean placeGuildHall(ServerWorld world, BlockPos origin) {
        RPGMinibosses.LOGGER.warn(
                "[rpg-minibosses] Guild-hall structure placement is not yet supported on NeoForge "
                        + "(StructurePlacerAPI is Fabric-only). Skipping placement at {}. "
                        + "The guild-hall dimension will load without its pre-built structure.",
                origin);
        return false;
    }
}

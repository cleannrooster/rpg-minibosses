package com.cleannrooster.rpg_minibosses.neoforge;







import java.util.Set;import java.util.List;import net.minecraft.world.biome.SpawnSettings;import net.minecraft.entity.SpawnGroup;import net.minecraft.entity.EntityType;import dev.architectury.registry.level.biome.BiomeModifications;import com.cleannrooster.rpg_minibosses.RPGMinibosses;
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

    @Override
    public void addMonsterSpawnAlongside(EntityType<?> type, int weight, Set<EntityType<?>> ifSpawnsOneOf) {
        // Architectury's read-only biome view returns no spawners, but inside a modification the mutable
        // view is backed by NeoForge's spawn-settings builder, which does: test there.
        BiomeModifications.addProperties((context, properties) -> {
            var spawners = properties.getSpawnProperties().getSpawners();
            if (spawners != null && spawners.values().stream().flatMap(List::stream)
                    .anyMatch(spawn -> ifSpawnsOneOf.contains(spawn.type))) {
                properties.getSpawnProperties().addSpawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(type, weight, 1, 1));
            }
        });
    }
}

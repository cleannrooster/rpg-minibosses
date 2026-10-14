package com.cleannrooster.anarchy_rpg.platform;



import java.util.Set;import net.minecraft.entity.EntityType;import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ServiceLoader;

/** Static access point to the loader-provided {@link PlatformHelper}. */
public final class Platform {
    private static final PlatformHelper INSTANCE = ServiceLoader.load(PlatformHelper.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                    "No PlatformHelper service found for anarchy_rpg on this loader"));

    private Platform() {
    }

    public static boolean isModLoaded(String modId) {
        return INSTANCE.isModLoaded(modId);
    }

    public static boolean placeGuildHall(ServerWorld world, BlockPos origin) {
        return INSTANCE.placeGuildHall(world, origin);
    }

    public static void addMonsterSpawnAlongside(EntityType<?> type, int weight, Set<EntityType<?>> ifSpawnsOneOf) {
        INSTANCE.addMonsterSpawnAlongside(type, weight, ifSpawnsOneOf);
    }
}

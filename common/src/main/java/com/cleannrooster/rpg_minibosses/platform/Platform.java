package com.cleannrooster.rpg_minibosses.platform;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ServiceLoader;

/** Static access point to the loader-provided {@link PlatformHelper}. */
public final class Platform {
    private static final PlatformHelper INSTANCE = ServiceLoader.load(PlatformHelper.class)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                    "No PlatformHelper service found for rpg-minibosses on this loader"));

    private Platform() {
    }

    public static boolean isModLoaded(String modId) {
        return INSTANCE.isModLoaded(modId);
    }

    public static boolean placeGuildHall(ServerWorld world, BlockPos origin) {
        return INSTANCE.placeGuildHall(world, origin);
    }
}

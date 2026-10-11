package com.cleannrooster.rpg_minibosses.entity;

import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Players who can see an entity — the vanilla stand-in for Fabric API's {@code PlayerLookup.tracking}.
 * Uses the chunk-watcher list, narrowed to the entity type's tracking range, which is what the entity
 * tracker itself checks.
 */
public final class EntityTracking {
    private EntityTracking() {
    }

    public static List<ServerPlayerEntity> playersTracking(Entity entity) {
        if (!(entity.getWorld() instanceof ServerWorld world)) {
            return List.of();
        }
        double range = entity.getType().getMaxTrackDistance() * 16.0;
        double rangeSq = range * range;
        return world.getChunkManager().chunkLoadingManager.getPlayersWatchingChunk(entity.getChunkPos(), false).stream()
                .filter(player -> player != entity && player.squaredDistanceTo(entity) <= rangeSq)
                .toList();
    }
}

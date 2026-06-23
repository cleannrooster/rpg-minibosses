package com.cleannrooster.rpg_minibosses.worldgen;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Defers miniboss encounter spawns out of worldgen.
 *
 * <p>{@link MinibossEncounterFeature} runs on a generation worker thread; spawning an entity there
 * deadlocks because the mob's construction/navigation touches neighbouring chunks that are still
 * mid-generation. Spawning inside a {@code CHUNK_LOAD} callback is just as unsafe for mobs with a
 * custom navigator (the Rogue's {@code TricksterNavigation}/{@code RogueNodeMaker}): adding the
 * entity re-enters chunk access while the chunk is still loading and hangs.</p>
 *
 * <p>So the feature only records the marker here, and the entity is spawned on the main thread during
 * the normal world tick — the same window vanilla uses for natural spawns — once the target chunk is
 * fully loaded. Each marker is keyed per chunk and consumed exactly once, so a chunk never
 * double-spawns; markers whose chunk is not loaded are simply retried on a later tick.</p>
 */
public final class MinibossEncounterSpawnManager {

    private record Pending(RegistryKey<World> world, BlockPos pos, EncounterType type, float yaw) {}

    private static final Map<Long, Pending> PENDING = new ConcurrentHashMap<>();

    private MinibossEncounterSpawnManager() {
    }

    /** Called from the feature (gen thread). Records the rotated spawn marker for deferred spawning. */
    public static void markPending(ServerWorld world, BlockPos marker, EncounterType type, float yaw) {
        PENDING.put(new ChunkPos(marker).toLong(), new Pending(world.getRegistryKey(), marker, type, yaw));
    }

    /** Wire the per-world tick drain once (loader-agnostic via FFAPI). */
    public static void register() {
        ServerTickEvents.END_WORLD_TICK.register(MinibossEncounterSpawnManager::drain);
    }

    private static void drain(ServerWorld world) {
        if (PENDING.isEmpty()) {
            return;
        }
        for (Iterator<Map.Entry<Long, Pending>> it = PENDING.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<Long, Pending> entry = it.next();
            Pending pending = entry.getValue();
            if (!pending.world().equals(world.getRegistryKey())) {
                continue;
            }
            ChunkPos chunkPos = new ChunkPos(entry.getKey());
            // Spawn only once the chunk is fully loaded; otherwise leave it queued for a later tick.
            if (!world.getChunkManager().isChunkLoaded(chunkPos.x, chunkPos.z)) {
                continue;
            }
            it.remove();
            spawn(world, pending);
        }
    }

    private static void spawn(ServerWorld world, Pending pending) {
        // Config is read at spawn time: structures still generate, only the mob is gated.
        if (!RPGMinibossesEntities.config.structureEncounterSpawns) {
            return;
        }
        BlockPos marker = pending.pos();
        // Require a solid floor and two blocks of clearance at the marker.
        if (!world.getBlockState(marker.down()).isSolidBlock(world, marker.down())
                || world.getBlockState(marker).isSolidBlock(world, marker)
                || world.getBlockState(marker.up()).isSolidBlock(world, marker.up())) {
            return;
        }

        EntityType<? extends MobEntity> entityType = pending.type().entityType();
        Entity entity = entityType.create(world);
        if (!(entity instanceof MobEntity mob)) {
            return;
        }
        mob.refreshPositionAndAngles(marker.getX() + 0.5, marker.getY(), marker.getZ() + 0.5, pending.yaw(), 0.0F);
        mob.initialize(world, world.getLocalDifficulty(marker), SpawnReason.STRUCTURE, null);
        // realmwalker-style NBT-backed persistence so the boss survives chunk reload.
        if (mob instanceof MinibossEntity miniboss) {
            miniboss.markFromStructure();
        } else {
            mob.setPersistent();
        }
        if (!world.spawnEntity(mob)) {
            mob.discard();
        }
    }
}

package com.cleannrooster.rpg_minibosses.platform;



import java.util.Set;import net.minecraft.entity.EntityType;import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Small explicit platform interface for the handful of operations that are genuinely
 * loader-specific (not covered by vanilla or Architectury API).
 *
 * <p>Implementations live in each loader module and are discovered via {@link java.util.ServiceLoader}
 * ({@code META-INF/services/com.cleannrooster.rpg_minibosses.platform.PlatformHelper}).</p>
 */
public interface PlatformHelper {
    /** Whether another mod is loaded. Fabric: FabricLoader; NeoForge: ModList. */
    boolean isModLoaded(String modId);

    /**
     * Place the guild-hall NBT structure at {@code origin} in {@code world}.
     *
     * <p>Fabric uses StructurePlacerAPI (its original behavior). NeoForge has no equivalent library,
     * so its implementation falls back to the vanilla structure-template manager and, if that is not
     * possible, logs a clear warning rather than silently doing nothing. Returns {@code true} if the
     * structure was placed.</p>
     */
    boolean placeGuildHall(ServerWorld world, BlockPos origin);

    /**
     * Add {@code type} as a monster spawn to every biome that already spawns one of {@code ifSpawnsOneOf}
     * (Fabric's {@code BiomeSelectors.spawnsOneOf}). Loader-specific because Architectury's read-only biome
     * view does not expose existing spawns. Call during init.
     */
    void addMonsterSpawnAlongside(EntityType<?> type, int weight, Set<EntityType<?>> ifSpawnsOneOf);
}

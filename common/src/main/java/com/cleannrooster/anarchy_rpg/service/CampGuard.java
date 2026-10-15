package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.hub.HubAnchor;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.CreeperEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;

import java.util.Comparator;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A camp Juggernaut on watch (Guard the camp): instead of wandering it walks a ring of {@link #WAYPOINTS} points
 * around its hub's bounds, {@link #INSET} blocks in from the edge, and fights monsters inside them or within
 * {@link #GUARD_MARGIN} blocks, dropping any chase that strays {@link #LEASH_MARGIN} blocks past them. Creepers
 * are left alone, as by a companion on patrol: it would only bring the explosion into the camp.
 *
 * <p>The watch is the adventurer's {@code guardUntil} world time, saved with it, so it outlives the chunk
 * unloading; while unloaded it simply does not happen.
 */
public final class CampGuard {
    private static final int WAYPOINTS = 8;
    private static final int INSET = 2;
    private static final double GUARD_MARGIN = 8.0;
    private static final double LEASH_MARGIN = 16.0;

    /** Which ring point each guard is walking to. Not saved: a reload starts the ring over. */
    private static final Map<MinibossEntity, Integer> WAYPOINT = new WeakHashMap<>();

    private CampGuard() {
    }

    /** Whether this adventurer is keeping watch over its camp right now. */
    public static boolean onWatch(MinibossEntity npc) {
        return npc.getOwnerUuid() == null && npc.getGuardUntil() > npc.getWorld().getTime();
    }

    /** Once a second from the adventurer's server tick. */
    public static void tick(MinibossEntity npc) {
        if (npc.age % 20 != 0 || !onWatch(npc) || !(npc.getWorld() instanceof ServerWorld world) || npc.isIncapacitated()) {
            return;
        }
        HubAnchor hub = CampServices.hubOf(npc);
        if (hub == null) {
            npc.setGuardUntil(0); // No longer lives there (turned over, recruited): the watch is off.
            return;
        }
        BlockBox bounds = hub.bounds();

        LivingEntity target = npc.getTarget();
        if (target != null) {
            if (!target.isAlive() || outside(bounds, target.getBlockPos(), LEASH_MARGIN)) {
                npc.setTarget(null);
                npc.getNavigation().stop();
            } else {
                return; // Its own combat AI has the fight.
            }
        }
        Box reach = new Box(bounds.getMinX(), bounds.getMinY(), bounds.getMinZ(), bounds.getMaxX() + 1, bounds.getMaxY() + 1, bounds.getMaxZ() + 1)
                .expand(GUARD_MARGIN, 8.0, GUARD_MARGIN);
        LivingEntity threat = world.getEntitiesByClass(LivingEntity.class, reach,
                        entity -> entity.isAlive() && entity instanceof Monster && !(entity instanceof CreeperEntity)
                                && !(entity instanceof MinibossEntity))
                .stream().min(Comparator.comparingDouble(npc::squaredDistanceTo)).orElse(null);
        if (threat != null) {
            npc.setTarget(threat);
            return;
        }

        if (npc.getNavigation().isIdle()) {
            int index = (WAYPOINT.getOrDefault(npc, -1) + 1) % WAYPOINTS;
            WAYPOINT.put(npc, index);
            BlockPos ring = ringPoint(bounds, index, npc.getBlockY());
            if (world.isChunkLoaded(ring)) {
                BlockPos ground = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, ring);
                npc.getNavigation().startMovingTo(ground.getX() + 0.5, ground.getY(), ground.getZ() + 0.5, 0.9);
            }
        }
    }

    /** Corner, side, corner, side... clockwise around the bounds, {@link #INSET} blocks in. */
    private static BlockPos ringPoint(BlockBox bounds, int index, int y) {
        int minX = Math.min(bounds.getMinX() + INSET, bounds.getCenter().getX());
        int maxX = Math.max(bounds.getMaxX() - INSET, bounds.getCenter().getX());
        int minZ = Math.min(bounds.getMinZ() + INSET, bounds.getCenter().getZ());
        int maxZ = Math.max(bounds.getMaxZ() - INSET, bounds.getCenter().getZ());
        int midX = (minX + maxX) / 2;
        int midZ = (minZ + maxZ) / 2;
        return switch (index) {
            case 0 -> new BlockPos(minX, y, minZ);
            case 1 -> new BlockPos(midX, y, minZ);
            case 2 -> new BlockPos(maxX, y, minZ);
            case 3 -> new BlockPos(maxX, y, midZ);
            case 4 -> new BlockPos(maxX, y, maxZ);
            case 5 -> new BlockPos(midX, y, maxZ);
            case 6 -> new BlockPos(minX, y, maxZ);
            default -> new BlockPos(minX, y, midZ);
        };
    }

    private static boolean outside(BlockBox bounds, BlockPos pos, double margin) {
        return pos.getX() < bounds.getMinX() - margin || pos.getX() > bounds.getMaxX() + margin
                || pos.getZ() < bounds.getMinZ() - margin || pos.getZ() > bounds.getMaxZ() + margin;
    }
}

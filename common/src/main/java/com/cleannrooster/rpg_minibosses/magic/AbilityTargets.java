package com.cleannrooster.rpg_minibosses.magic;

import dev.eann.atlasvfx.telegraph.TelegraphArea;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.function.Predicate;

/**
 * Who an area contains. The area is AtlasVFX's {@link TelegraphArea} — the same object a telegraph draws —
 * so the warning a player sees and the volume that hits them cannot disagree.
 */
public final class AbilityTargets {
    private AbilityTargets() {
    }

    /**
     * Living entities standing in {@code area}: any corner of their footprint, or its centre, over the
     * shape and within {@code height} above it. Testing the footprint rather than the feet point is what
     * lets a wide mob be caught by the edge of a ring.
     */
    public static List<LivingEntity> within(ServerWorld world, TelegraphArea area, double height,
                                            Predicate<LivingEntity> filter) {
        var search = area.bounds(height).expand(2.0, 0.0, 2.0);
        return world.getEntitiesByClass(LivingEntity.class, search, entity -> filter.test(entity) && touches(area, entity, height));
    }

    /** Living entities in {@code area} that {@code caster} may harm. */
    public static List<LivingEntity> harmable(ServerWorld world, LivingEntity caster, TelegraphArea area, double height) {
        return within(world, area, height, entity -> Relations.canHarm(caster, entity));
    }

    static boolean touches(TelegraphArea area, LivingEntity entity, double height) {
        var box = entity.getBoundingBox();
        double y = box.minY;
        if (area.contains(new Vec3d(entity.getX(), y, entity.getZ()), height)) {
            return true;
        }
        return area.contains(new Vec3d(box.minX, y, box.minZ), height)
                || area.contains(new Vec3d(box.minX, y, box.maxZ), height)
                || area.contains(new Vec3d(box.maxX, y, box.minZ), height)
                || area.contains(new Vec3d(box.maxX, y, box.maxZ), height);
    }
}

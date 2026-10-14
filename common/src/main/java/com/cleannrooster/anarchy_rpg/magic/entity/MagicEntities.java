package com.cleannrooster.anarchy_rpg.magic.entity;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

/** Entity types for the ability carriers. Registered in the ENTITY_TYPE phase. */
public final class MagicEntities {
    public static EntityType<BossProjectile> PROJECTILE;
    public static EntityType<BossCloud> CLOUD;

    private MagicEntities() {
    }

    public static void register() {
        PROJECTILE = Registry.register(Registries.ENTITY_TYPE, RPGMinibosses.id("boss_projectile"),
                EntityType.Builder.<BossProjectile>create(BossProjectile::new, SpawnGroup.MISC)
                        .dimensions(0.5f, 0.5f)
                        .maxTrackingRange(8)
                        .trackingTickInterval(1)
                        .build("boss_projectile"));
        CLOUD = Registry.register(Registries.ENTITY_TYPE, RPGMinibosses.id("boss_cloud"),
                EntityType.Builder.<BossCloud>create(BossCloud::new, SpawnGroup.MISC)
                        .dimensions(0.5f, 0.5f)
                        .maxTrackingRange(8)
                        .trackingTickInterval(10)
                        .build("boss_cloud"));
    }
}

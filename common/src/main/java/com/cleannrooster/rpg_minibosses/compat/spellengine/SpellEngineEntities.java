package com.cleannrooster.rpg_minibosses.compat.spellengine;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.compat.spellengine.magus.MagusDominionOrbEntity;
import com.cleannrooster.rpg_minibosses.compat.spellengine.magus.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.compat.spellengine.magus.OrbEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.registry.Registry;
import net.spell_power.api.SpellSchools;

import static com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities.config;
import static com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities.normalMovementSpeed;
import static net.minecraft.registry.Registries.ENTITY_TYPE;

/** Forsaken Magus and the entities only its kit spawns. */
public final class SpellEngineEntities {
    private SpellEngineEntities() {
    }

    /** Joins {@link RPGMinibossesEntities#entries} on construction, so the core loop registers it, its spawn egg and its attributes. */
    public static final RPGMinibossesEntities.Entry<MagusPrimeEntity> MAGUS_PRIME = new RPGMinibossesEntities.Entry<MagusPrimeEntity>("magus",
            MagusPrimeEntity::new, HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,config.magusArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.magusMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.magusMaxHealth)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,config.magusKnockbackResistance)
            .add(SpellSchools.FIRE.attributeEntry,config.magusFirePower)
            .add(SpellSchools.ARCANE.attributeEntry,config.magusArcanePower)
            .add(SpellSchools.FROST.attributeEntry,config.magusFrostPower)
            .add(SpellSchools.LIGHTNING.attributeEntry,config.magusLightningPower)
            .add(SpellSchools.SOUL.attributeEntry,config.magusSoulPower),

            0x09356B,
            0xebcb6a,false);

    /** The dark-matter orb. Registered as {@code rpg-minibosses:dark_matter}. */
    public static EntityType<OrbEntity> ORB;
    public static EntityType<MagusDominionOrbEntity> MAGUS_DOMINION_ORB;

    /** Forces class initialization, which is what adds {@link #MAGUS_PRIME} to the core entry list. */
    static void declare() {
    }

    static void register() {
        ORB = Registry.register(
                ENTITY_TYPE,
                RPGMinibosses.id("dark_matter"),
                EntityType.Builder.<OrbEntity>create(OrbEntity::new, SpawnGroup.MONSTER)
                        .dimensions(4, 4)
                        .maxTrackingRange(8)
                        .trackingTickInterval(1)
                        .build("dark_matter")
        );
        MAGUS_DOMINION_ORB = Registry.register(
                ENTITY_TYPE,
                RPGMinibosses.id("magus_dominion_orb"),
                EntityType.Builder.<MagusDominionOrbEntity>create(MagusDominionOrbEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5F, 0.5F)
                        .maxTrackingRange(4)
                        .trackingTickInterval(5)
                        .build("magus_dominion_orb")
        );
    }
}

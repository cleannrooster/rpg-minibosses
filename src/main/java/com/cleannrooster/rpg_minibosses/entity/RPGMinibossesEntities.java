package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.RPGMinibosses.*;

import com.cleannrooster.rpg_minibosses.config.ServerConfig;
import com.cleannrooster.rpg_minibosses.config.ServerConfigWrapper;
import com.extraspellattributes.ReabsorptionInit;
import com.google.common.base.Predicates;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.block.entity.Spawner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.DolphinEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.structure.OceanMonumentGenerator;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureSpawns;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.structure.BasicTempleStructure;
import net.minecraft.world.gen.structure.OceanMonumentStructure;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraft.world.spawner.PatrolSpawner;
import net.minecraft.world.spawner.SpecialSpawner;
import net.spell_engine.fx.ParticleHelper;
import net.spell_engine.fx.SpellEngineParticles;
import net.spell_engine.rpg_series.config.Defaults;
import net.spell_power.api.SpellSchools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;


import static com.cleannrooster.rpg_minibosses.RPGMinibosses.MOD_ID;
import static net.minecraft.registry.Registries.ENTITY_TYPE;

public class RPGMinibossesEntities {
    public static final ArrayList<Entry> entries = new ArrayList<>();
    public static final ArrayList<Entry> minibosses = new ArrayList<>();

    public static ServerConfig config;
    static{
        AutoConfig.register(ServerConfigWrapper.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));
        config = AutoConfig.getConfigHolder(ServerConfigWrapper.class).getConfig().server;

    }


    public static class Entry<T extends PatrolEntity> {
        public final Identifier id;
        public EntityType<T> entityType;
        public EntityType.EntityFactory<T> entityFactory;
        public DefaultAttributeContainer.Builder attributes;
        public int primarycolor;
        public int secondarycolor;
        public int maxSize;


        public boolean shouldSpawn;

        public Entry(String name, EntityType.EntityFactory<T> factory, DefaultAttributeContainer.Builder attributes, int primarycolor, int secondarycolor, boolean shouldSpawn) {
            this.id = Identifier.of(MOD_ID, name);
            this.entityFactory = factory;
            this.attributes = attributes;
            this.primarycolor = primarycolor;
            this.secondarycolor = secondarycolor;
            this.shouldSpawn = shouldSpawn;
            this.maxSize = 3;
            entries.add(this);
        }
        public Entry(String name, EntityType.EntityFactory<T> factory, DefaultAttributeContainer.Builder attributes, int primarycolor, int secondarycolor, boolean shouldSpawn, int maxSize) {
            this.id = Identifier.of(MOD_ID, name);
            this.entityFactory = factory;
            this.attributes = attributes;
            this.primarycolor = primarycolor;
            this.secondarycolor = secondarycolor;
            this.shouldSpawn = shouldSpawn;
            this.maxSize = maxSize;
            entries.add(this);
        }

        public EntityType<T> entityType() {
            return entityType;
        }

        public Identifier id() {
            return id;
        }

        public EntityType.EntityFactory<T> entityFactory() {
            return entityFactory;
        }
    }

    public static double normalMovementSpeed = 0.23000000417232513;
    public static final Entry<JuggernautEntity> JUGGERNAUT_ENTITY_ENTRY = new Entry<JuggernautEntity>("juggernaut",
            (entityType,world) -> new JuggernautEntity(entityType,world,false,config.juggernautGreater), HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.greaterScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(ReabsorptionInit.DEFIANCE,config.juggernautGreaterDefiance)
            .add(EntityAttributes.GENERIC_ARMOR,config.juggernautGreaterArmor)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.juggernautGreaterAttackDamage)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.juggernautGreaterMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.juggernautGreaterMaxHealth)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,config.juggernautGreaterKnockbackResistance),
            0x09356B,
            0xebcb6a,false);
    public static final Entry<ArtilleristEntity> ARTILLERIST_ENTITY_ENTRY = new Entry<ArtilleristEntity>("mercenary",
            (entityType,world) -> new ArtilleristEntity(entityType,world,false,config.mercenaryGreater), HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.greaterScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ARMOR,config.mercenaryGreaterArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.mercenaryGreaterMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.mercenaryGreaterMaxHealth)
            .add(ReabsorptionInit.DEFIANCE,config.mercenaryGreaterDefiance),

            0x09356B,
            0xebcb6a,false);

    public static final Entry<TricksterEntity> TRICKSTER_ENTITY_ENTRY = new Entry<TricksterEntity>("trickster",
            (entityType,world) -> new TricksterEntity(entityType,world,false,config.rogueGreater),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.greaterScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,6)
            .add(EntityAttributes.GENERIC_ARMOR,config.rogueGreaterArmor)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.rogueGreaterAttackDamage)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.rogueGreaterMovementSpeed)
            .add(ReabsorptionInit.SPELLSUPPRESS,100+config.rogueGreaterSuppress)
            .add(ReabsorptionInit.GLANCINGBLOW,100+config.rogueGreaterEvasion)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.rogueGreaterMaxHealth),
            0x09356B,
            0xebcb6a,false);
    public static final Entry<JuggernautEntity> M_JUGGERNAUT_ENTITY_ENTRY = new Entry<JuggernautEntity>("minor_juggernaut",
            (entityType,world) -> new JuggernautEntity(entityType,world,true,config.juggernautLesser), HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(ReabsorptionInit.DEFIANCE,config.juggernautLesserDefiance)
            .add(EntityAttributes.GENERIC_ARMOR,config.juggernautLesserArmor)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.juggernautLesserAttackDamage)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.juggernautLesserMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.juggernautLesserMaxHealth)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,config.juggernautLesserKnockbackResistance),
            0x09356B,
            0xebcb6a,true);
    public static final Entry<ArtilleristEntity> M_ARTILLERIST_ENTITY_ENTRY = new Entry<ArtilleristEntity>("minor_mercenary",
            (entityType,world) -> new ArtilleristEntity(entityType,world,true,config.mercenaryLesser), HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,config.mercenaryLesserMaxHealth)
            .add(EntityAttributes.GENERIC_ARMOR,config.mercenaryLesserArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed* config.mercenaryLesserMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.mercenaryLesserMaxHealth)
            .add(ReabsorptionInit.DEFIANCE,config.mercenaryLesserDefiance),

            0x09356B,
            0xebcb6a,true);

    public static final Entry<TricksterEntity> M_TRICKSTER_ENTITY_ENTRY = new Entry<TricksterEntity>("minor_trickster",
            (entityType,world) -> new TricksterEntity(entityType,world,true,config.rogueLesser),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.rogueLesserAttackDamage)
            .add(EntityAttributes.GENERIC_ARMOR,config.rogueLesserArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.rogueLesserMovementSpeed)
            .add(ReabsorptionInit.SPELLSUPPRESS,100F+config.rogueLesserSuppress)
            .add(ReabsorptionInit.GLANCINGBLOW,100+config.rogueLesserEvasion)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.rogueLesserMaxHealth),
            0x09356B,
            0xebcb6a,true);

    public static final Entry<MagusPrimeEntity> MAGuS_PRIME = new Entry<MagusPrimeEntity>("magus",
            MagusPrimeEntity::new,HostileEntity.createHostileAttributes()
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


    public static final Entry<ArchmageFireEntity> ARCHMAGE_FIRE_ENTITY_ENTRY = new Entry<ArchmageFireEntity>("archmage_fire",
            (entityType,world) -> new ArchmageFireEntity(entityType,world,false,config.fireMageGreater),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.greaterScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,config.fireMageGreaterArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.fireMageMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.fireMageGreaterMaxHealth)
            .add(SpellSchools.FIRE.attributeEntry,config.fireMageFirePower),
            0x09356B,
            0xebcb6a,false);
    public static final Entry<TemplarEntity> TEMPLAR_ENTITY_ENTRY = new Entry<TemplarEntity>("templar",
            (entityType,world) -> new TemplarEntity(entityType,world,false,config.templarGreater),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.greaterScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.templarGreaterAttackDamage)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,1)
            .add(EntityAttributes.GENERIC_ARMOR,config.templarGreaterArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.templarGreaterMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,100F)
            .add(SpellSchools.HEALING.attributeEntry,config.templarGreaterHealingPower),
            0x09356B,
            0xebcb6a,false);
    public static final Entry<ArchmageFireEntity> M_ARCHMAGE_FIRE_ENTITY_ENTRY = new Entry<ArchmageFireEntity>("minor_archmage_fire",
            (entityType,world) -> new ArchmageFireEntity(entityType,world,true,config.fireMageLesser),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,config.fireMageLesserArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.fireMageLesserMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.fireMageLesserMaxHealth)
            .add(SpellSchools.FIRE.attributeEntry,config.fireMageLesserFirePower),
            0x09356B,
            0xebcb6a,true);
    public static final Entry<TemplarEntity> M_TEMPLAR_ENTITY_ENTRY = new Entry<TemplarEntity>("minor_templar",
            (entityType,world) -> new TemplarEntity(entityType,world,true,config.templarLesser),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.templarLesserAttackDamage)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,1)
            .add(EntityAttributes.GENERIC_ARMOR,config.templarLesserArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.templarLesserMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.templarLesserMaxHealth)
            .add(SpellSchools.HEALING.attributeEntry,config.templarLesserHealingPower),
            0x09356B,
            0xebcb6a,true);
    public static RegistryKey<ItemGroup> KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(),Identifier.of(RPGMinibosses.MOD_ID,"generic"));



    public static ItemGroup RPGENTITIES;
    public static void register() {
        for (var entry: entries) {
            entry.entityType  = Registry.register(
                    ENTITY_TYPE,
                    Identifier.of(MOD_ID, entry.id().getPath()),
                    FabricEntityTypeBuilder.<MinibossEntity>create(SpawnGroup.MONSTER, entry.entityFactory())
                            .dimensions(EntityDimensions.changing(0.6F, 1.8F).withEyeHeight(1.62F)) // dimensions in Minecraft units of the render
                            .trackRangeBlocks(128)
                            .trackedUpdateRate(1)
                            .build()
            );
            Item  EGG = new SpawnEggItem(entry.entityType, entry.primarycolor, entry.secondarycolor, new Item.Settings());
            Registry.register(Registries.ITEM, Identifier.of(MOD_ID, "forsaken_"+entry.id().getPath()+"_spawn_egg"), EGG);

            if ( RPGENTITIES == null){
                RPGENTITIES = FabricItemGroup.builder()
                        .icon(() -> new ItemStack(EGG))
                        .displayName(Text.translatable("itemGroup.rpg-minibosses.eggs"))
                        .build();
                Registry.register(Registries.ITEM_GROUP, KEY, RPGENTITIES);
            }
            ItemGroupEvents.modifyEntriesEvent(KEY).register((content) -> {
                content.add(EGG);
            });
            if(entry.shouldSpawn) {
                BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityType.WITCH), SpawnGroup.MONSTER, entry.entityType, config.mult, 1, 1);
                minibosses.add(entry);

            }

            SpawnRestriction.register((EntityType<? extends PatrolEntity>)entry.entityType, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, PatrolEntity::canSpawn);

            FabricDefaultAttributeRegistry.register(entry.entityType, entry.attributes);

        }
    }

}

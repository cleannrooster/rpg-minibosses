package com.cleannrooster.rpg_minibosses.entity;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.RPGMinibosses.*;

import com.cleannrooster.rpg_minibosses.config.ServerConfig;
import com.cleannrooster.rpg_minibosses.magic.MagicCaster;
import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import com.cleannrooster.rpg_minibosses.platform.Platform;
import com.cleannrooster.rpg_minibosses.config.ServerConfigWrapper;
import com.google.common.base.Predicates;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.level.biome.BiomeModifications;
import dev.architectury.registry.level.entity.EntityAttributeRegistry;
import dev.architectury.registry.level.entity.SpawnPlacementsRegistry;
import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import me.shedaniel.autoconfig.serializer.PartitioningSerializer;
import net.minecraft.block.entity.Spawner;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.AboveGroundTargeting;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
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
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BiomeTags;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.structure.OceanMonumentGenerator;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.village.raid.Raid;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureSpawns;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.structure.BasicTempleStructure;
import net.minecraft.world.gen.structure.OceanMonumentStructure;
import net.minecraft.world.gen.structure.StructureType;
import net.minecraft.world.spawner.PatrolSpawner;
import net.minecraft.world.spawner.SpecialSpawner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;


import static com.cleannrooster.rpg_minibosses.RPGMinibosses.CONTENT_NAMESPACE;
import static net.minecraft.registry.Registries.ENTITY_TYPE;

public class RPGMinibossesEntities {
    public static final ArrayList<Entry> entries = new ArrayList<>();
    public static final ArrayList<Entry> minibosses = new ArrayList<>();

    public static ServerConfig config;
    static{
        AutoConfig.register(ServerConfigWrapper.class, PartitioningSerializer.wrap(JanksonConfigSerializer::new));
        config = AutoConfig.getConfigHolder(ServerConfigWrapper.class).getConfig().server;

    }


    public static class Entry<T extends PathAwareEntity> {
        public final Identifier id;
        public EntityType<T> entityType;
        public EntityType.EntityFactory<T> entityFactory;
        public DefaultAttributeContainer.Builder attributes;
        public int primarycolor;
        public int secondarycolor;
        public int maxSize;


        public boolean shouldSpawn;

        public Entry(String name, EntityType.EntityFactory<T> factory, DefaultAttributeContainer.Builder attributes, int primarycolor, int secondarycolor, boolean shouldSpawn) {
            this.id = Identifier.of(CONTENT_NAMESPACE, name);
            this.entityFactory = factory;
            this.attributes = attributes;
            this.primarycolor = primarycolor;
            this.secondarycolor = secondarycolor;
            this.shouldSpawn = shouldSpawn;
            this.maxSize = 3;
            entries.add(this);
        }
        public Entry(String name, EntityType.EntityFactory<T> factory, DefaultAttributeContainer.Builder attributes, int primarycolor, int secondarycolor, boolean shouldSpawn, int maxSize) {
            this.id = Identifier.of(CONTENT_NAMESPACE, name);
            this.entityFactory = factory;
            this.attributes = attributes;
            this.primarycolor = primarycolor;
            this.secondarycolor = secondarycolor;
            this.shouldSpawn = shouldSpawn;
            this.maxSize = maxSize;
            entries.add(this);
        }


        /** This boss's power in a magical school, from config; read back through {@link MagicCaster}. */
        public final java.util.EnumMap<MagicSchool, Double> magicPower = new java.util.EnumMap<>(MagicSchool.class);

        public Entry<T> magic(MagicSchool school, double power) {
            this.magicPower.put(school, power);
            return this;
        }

        public DefaultAttributeContainer.Builder add( RegistryEntry<EntityAttribute> attribute, double baseValue) {
              attributes.add(attribute, baseValue);
            return attributes;
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

    public static double normalMovementSpeed = 0.22;
    public static final Entry<JuggernautEntity> JUGGERNAUT_ENTITY_ENTRY = new Entry<JuggernautEntity>("juggernaut",
            (entityType,world) -> new JuggernautEntity(entityType,world,false,config.juggernautGreater), HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.greaterScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)/*
            .add(ReabsorptionInit.DEFIANCE,config.juggernautGreaterDefiance)*/
            .add(EntityAttributes.GENERIC_ARMOR,config.juggernautGreaterArmor)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.juggernautGreaterAttackDamage)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.juggernautGreaterMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.juggernautGreaterMaxHealth)
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F)

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
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F),
          //  .add(ReabsorptionInit.DEFIANCE,config.mercenaryGreaterDefiance),

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
         /*   .add(ReabsorptionInit.SPELLSUPPRESS,100+config.rogueGreaterSuppress)
            .add(ReabsorptionInit.GLANCINGBLOW,100+config.rogueGreaterEvasion)*/
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F)

            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.rogueGreaterMaxHealth),
            0x09356B,
            0xebcb6a,false);
    public static final Entry<JuggernautEntity> M_JUGGERNAUT_ENTITY_ENTRY = new Entry<JuggernautEntity>("minor_juggernaut",
            (entityType,world) -> new JuggernautEntity(entityType,world,true,config.juggernautLesser), HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)/*
            .add(ReabsorptionInit.DEFIANCE,config.juggernautLesserDefiance)*/
            .add(EntityAttributes.GENERIC_ARMOR,config.juggernautLesserArmor)

            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.juggernautLesserAttackDamage)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.juggernautLesserMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.juggernautLesserMaxHealth)
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,config.juggernautLesserKnockbackResistance),
            0x09356B,
            0xebcb6a,true);
    public static final Entry<ArtilleristEntity> M_ARTILLERIST_ENTITY_ENTRY = new Entry<ArtilleristEntity>("minor_mercenary",
            (entityType,world) -> new ArtilleristEntity(entityType,world,true,config.mercenaryLesser), HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ARMOR,config.mercenaryLesserArmor)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed* config.mercenaryLesserMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.mercenaryLesserMaxHealth)
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F),/*
            .add(ReabsorptionInit.DEFIANCE,config.mercenaryLesserDefiance),
*/
            0x09356B,
            0xebcb6a,true);

    public static final Entry<TricksterEntity> M_TRICKSTER_ENTITY_ENTRY = new Entry<TricksterEntity>("minor_trickster",
            (entityType,world) -> new TricksterEntity(entityType,world,true,config.rogueLesser),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.rogueLesserAttackDamage)
            .add(EntityAttributes.GENERIC_ARMOR,config.rogueLesserArmor)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.rogueLesserMovementSpeed)/*
            .add(ReabsorptionInit.SPELLSUPPRESS,100F+config.rogueLesserSuppress)
            .add(ReabsorptionInit.GLANCINGBLOW,100+config.rogueLesserEvasion)*/
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.rogueLesserMaxHealth),
            0x09356B,
            0xebcb6a,true);

    public static final Entry<ArchmageFireEntity> ARCHMAGE_FIRE_ENTITY_ENTRY = new Entry<ArchmageFireEntity>("archmage_fire",
            (entityType,world) -> new ArchmageFireEntity(entityType,world,false,config.fireMageGreater),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.greaterScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,24)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)

            .add(EntityAttributes.GENERIC_ARMOR,config.fireMageGreaterArmor)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.fireMageMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.fireMageGreaterMaxHealth)
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F),
            0x09356B,
            0xebcb6a,false)
            .magic(MagicSchool.FIRE, config.fireMageFirePower);
    public static final Entry<TemplarEntity> TEMPLAR_ENTITY_ENTRY = new Entry<TemplarEntity>("templar",
            (entityType,world) -> new TemplarEntity(entityType,world,false,config.templarGreater),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.greaterScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.templarGreaterAttackDamage)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,1)
            .add(EntityAttributes.GENERIC_ARMOR,config.templarGreaterArmor)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.templarGreaterMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,100F)
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F),
            0x09356B,
            0xebcb6a,false)
            .magic(MagicSchool.HEALING, config.templarGreaterHealingPower);
    public static final Entry<ArchmageFireEntity> M_ARCHMAGE_FIRE_ENTITY_ENTRY = new Entry<ArchmageFireEntity>("minor_archmage_fire",
            (entityType,world) -> new ArchmageFireEntity(entityType,world,true,config.fireMageLesser),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,24)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,config.fireMageLesserArmor)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.fireMageLesserMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.fireMageLesserMaxHealth)
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F),
            0x09356B,
            0xebcb6a,true)
            .magic(MagicSchool.FIRE, config.fireMageLesserFirePower);

    public static final Entry<TemplarEntity> M_TEMPLAR_ENTITY_ENTRY = new Entry<TemplarEntity>("minor_templar",
            (entityType,world) -> new TemplarEntity(entityType,world,true,config.templarLesser),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_SCALE,config.lesserScale)

            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.templarLesserAttackDamage)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,1)
            .add(EntityAttributes.GENERIC_ARMOR,config.templarLesserArmor)

            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*config.templarLesserMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.templarLesserMaxHealth)
            .add(EntityAttributes.GENERIC_STEP_HEIGHT,1.2F),
            0x09356B,
            0xebcb6a,true)
            .magic(MagicSchool.HEALING, config.templarLesserHealingPower);

    public static final Entry<GeminiEntity> GEMINI_ALPHA = new Entry<GeminiEntity>("gemini_alpha",
            ((type, world) -> new GeminiEntity(type,world,MagicSchool.FIRE)),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,config.alphaArmor)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.alphaHealth)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,100)
            .add(EntityAttributes.GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE,100)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.betaAttackDamage),

            0x09356B,
            0xebcb6a,false)
            .magic(MagicSchool.ARCANE, config.alphaFirePower)
            .magic(MagicSchool.FIRE, config.alphaFirePower)
            .magic(MagicSchool.LIGHTNING, config.alphaLightningPower);

    public static final Entry<GeminiEntity> GEMINI_BETA = new Entry<GeminiEntity>("gemini_beta",
            ((type, world) -> new GeminiEntity(type,world,MagicSchool.FROST)),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,config.betaArmor)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.betaHealth)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,100)
            .add(EntityAttributes.GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE,100)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.betaAttackDamage),

            0x09356B,
            0xebcb6a,false)
            .magic(MagicSchool.FROST, config.betaFrostPower)
            .magic(MagicSchool.LIGHTNING, config.betaLightningPower);

    public static final Entry<GeminiEntity> GEMINI_ALPHA_UBER = new Entry<GeminiEntity>("gemini_alpha_uber",
            ((type, world) -> new GeminiEntity(type,world,MagicSchool.FIRE, true)),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,config.alphaUberArmor)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.alphaUberHealth)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,100)
            .add(EntityAttributes.GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE,100)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.alphaUberAttackDamage),
            0x09356B,
            0xebcb6a,false)
            .magic(MagicSchool.ARCANE, config.alphaUberArcanePower)
            .magic(MagicSchool.FIRE, config.alphaUberFirePower)
            .magic(MagicSchool.LIGHTNING, config.alphaUberLightningPower);

    public static final Entry<GeminiEntity> GEMINI_BETA_UBER = new Entry<GeminiEntity>("gemini_beta_uber",
            ((type, world) -> new GeminiEntity(type,world,MagicSchool.FROST, true)),HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,config.betaUberArmor)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,config.betaUberHealth)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,100)
            .add(EntityAttributes.GENERIC_EXPLOSION_KNOCKBACK_RESISTANCE,100)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,config.betaUberAttackDamage),
            0x09356B,
            0xebcb6a,false)
            .magic(MagicSchool.FROST, config.betaUberFrostPower)
            .magic(MagicSchool.LIGHTNING, config.betaUberLightningPower);

    public static RegistryKey<ItemGroup> KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(),Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"generic"));

    public static EntityType<TrapCleann> TRAP;
    public static EntityType<StormAnchorEntity> STORM_ANCHOR;



    public static ItemGroup RPGENTITIES;
    private static Item iconEgg;

    /**
     * Convenience for Fabric: registries are open throughout {@code onInitialize}, so run every
     * phase in order. NeoForge instead invokes the individual phase methods from the matching
     * {@code RegisterEvent}.
     */
    public static void register() {
        registerEntityTypes();
        registerSpawnEggs();
        registerItemGroup();
    }

    /** ENTITY_TYPE registry phase. Spawn rules / attributes / biome spawns go through Architectury. */
    public static void registerEntityTypes() {
        for (var entry: entries) {
            entry.entityType  = Registry.register(
                    ENTITY_TYPE,
                    Identifier.of(CONTENT_NAMESPACE, entry.id().getPath()),
                    EntityType.Builder.create(entry.entityFactory(), SpawnGroup.MONSTER)
                            .dimensions(0.6F, 1.8F).eyeHeight(1.62F) // dimensions in Minecraft units of the render
                            .maxTrackingRange(8) // chunks: 128 blocks
                            .trackingTickInterval(1)
                            .build(entry.id().toString())
            );
            EntityType<?> type = entry.entityType;
            if(entry.shouldSpawn) {
                Platform.addMonsterSpawnAlongside(type, config.mult, Set.of(EntityType.WITCH));
                TagKey<Biome> guildHall = TagKey.of(RegistryKeys.BIOME,Identifier.of(CONTENT_NAMESPACE,"has_structure/guild_hall"));
                addSpawn(context -> context.hasTag(guildHall), type, 25);

                if(config.hell){
                    Platform.addMonsterSpawnAlongside(type, config.mult, Set.of(EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN));

                }
                minibosses.add(entry);

            }

            SpawnPlacementsRegistry.register(() -> (EntityType<PathAwareEntity>) entry.entityType, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, MinibossEntity::canSpawn);

            DefaultAttributeContainer.Builder attributes = entry.attributes;
            EntityAttributeRegistry.register(() -> (EntityType<? extends LivingEntity>) type, () -> attributes);

        }
        TRAP = Registry.register(
                ENTITY_TYPE,
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "trap"),
                EntityType.Builder.<TrapCleann>create(TrapCleann::new, SpawnGroup.MISC)
                        .dimensions(1F, 1F) // dimensions in Minecraft units of the render
                        .maxTrackingRange(8)
                        .trackingTickInterval(1)
                        .build("trap")
        );
        STORM_ANCHOR = Registry.register(
                ENTITY_TYPE,
                Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "storm_anchor"),
                EntityType.Builder.<StormAnchorEntity>create(StormAnchorEntity::new, SpawnGroup.MISC)
                        .dimensions(0.5F, 0.5F)
                        .maxTrackingRange(8)
                        .trackingTickInterval(5)
                        .build("storm_anchor")
        );
    }

    private static void addSpawn(java.util.function.Predicate<BiomeModifications.BiomeContext> where, EntityType<?> type, int weight) {
        BiomeModifications.addProperties(where, (context, properties) ->
                properties.getSpawnProperties().addSpawn(SpawnGroup.MONSTER, new SpawnSettings.SpawnEntry(type, weight, 1, 1)));
    }

    /** The power the entry for {@code type} states in {@code school}; 0 if it states none. */
    public static double magicPower(EntityType<?> type, MagicSchool school) {
        for (Entry<?> entry : entries) {
            if (entry.entityType == type) {
                return entry.magicPower.getOrDefault(school, 0.0);
            }
        }
        return 0.0;
    }

    /** ITEM registry phase: spawn eggs. Requires {@link #registerEntityTypes()} to have run. */
    public static void registerSpawnEggs() {
        for (var entry: entries) {
            Item  EGG = new SpawnEggItem(entry.entityType, entry.primarycolor, entry.secondarycolor, new Item.Settings());
            if(entry.id().toString().contains("gemini")){
                Registry.register(Registries.ITEM, Identifier.of(CONTENT_NAMESPACE, entry.id().getPath() + "_spawn_egg"), EGG);
            }
            else {
                Registry.register(Registries.ITEM, Identifier.of(CONTENT_NAMESPACE, "forsaken_" + entry.id().getPath() + "_spawn_egg"), EGG);
            }
            if (iconEgg == null) {
                iconEgg = EGG;
            }
            CreativeTabRegistry.append(CreativeTabRegistry.defer(KEY), EGG);
        }
    }

    /** ITEM_GROUP registry phase. Icon requires a spawn egg from {@link #registerSpawnEggs()}. */
    public static void registerItemGroup() {
        RPGENTITIES = CreativeTabRegistry.create(Text.translatable("itemGroup.rpg-minibosses.eggs"),
                () -> new ItemStack(iconEgg));
        Registry.register(Registries.ITEM_GROUP, KEY, RPGENTITIES);
    }

}

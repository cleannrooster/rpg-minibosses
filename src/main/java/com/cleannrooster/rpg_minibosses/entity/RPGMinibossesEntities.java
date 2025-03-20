package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.extraspellattributes.ReabsorptionInit;
import com.google.common.base.Predicates;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectionContext;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.*;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.entity.mob.GuardianEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.SkeletonEntity;
import net.minecraft.entity.passive.AnimalEntity;
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
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureSpawns;
import net.minecraft.world.biome.SpawnSettings;
import net.minecraft.world.gen.structure.BasicTempleStructure;
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

    public static class Entry<T extends MinibossEntity> {
        public final Identifier id;
        public EntityType<T> entityType;
        public EntityType.EntityFactory<T> entityFactory;
        public DefaultAttributeContainer.Builder attributes;
        public int primarycolor;
        public int secondarycolor;

        public Entry(String name, EntityType.EntityFactory<T> factory, DefaultAttributeContainer.Builder attributes, int primarycolor, int secondarycolor) {
            this.id = Identifier.of(MOD_ID, name);
            this.entityFactory = factory;
            this.attributes = attributes;
            this.primarycolor = primarycolor;
            this.secondarycolor = secondarycolor;
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
            JuggernautEntity::new, HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(ReabsorptionInit.DEFIANCE,4F)
            .add(EntityAttributes.GENERIC_ARMOR,20)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*1)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,100)
            .add(EntityAttributes.GENERIC_KNOCKBACK_RESISTANCE,0.75),
            0x09356B,
            0xebcb6a);
    public static final Entry<ArtilleristEntity> ARTILLERIST_ENTITY_ENTRY = new Entry<ArtilleristEntity>("mercenary",
            ArtilleristEntity::new, HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ARMOR,14)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,100)
            .add(ReabsorptionInit.DEFIANCE,2F),

            0x09356B,
            0xebcb6a);
    public static final Entry<TricksterEntity> TRICKSTER_ENTITY_ENTRY = new Entry<TricksterEntity>("trickster",
            TricksterEntity::new,HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,6)
            .add(EntityAttributes.GENERIC_ARMOR,8)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*1.8F)
            .add(ReabsorptionInit.SPELLSUPPRESS,160F)
            .add(ReabsorptionInit.GLANCINGBLOW,160F)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,100),
            0x09356B,
            0xebcb6a);

    public static final Entry<ArchmageFireEntity> ARCHMAGE_FIRE_ENTITY_ENTRY = new Entry<ArchmageFireEntity>("archmage_fire",
            ArchmageFireEntity::new,HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,4)
            .add(EntityAttributes.GENERIC_ARMOR,4)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*0.9F)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,100)
            .add(SpellSchools.FIRE.attributeEntry,6),
            0x09356B,
            0xebcb6a);
    public static final Entry<TemplarEntity> TEMPLAR_ENTITY_ENTRY = new Entry<TemplarEntity>("templar",
            TemplarEntity::new,HostileEntity.createHostileAttributes()
            .add(EntityAttributes.GENERIC_FOLLOW_RANGE,32)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE,8)
            .add(EntityAttributes.GENERIC_ATTACK_SPEED,1)
            .add(EntityAttributes.GENERIC_ARMOR,16)
            .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, normalMovementSpeed*1.4F)
            .add(EntityAttributes.GENERIC_MAX_HEALTH,100)
            .add(SpellSchools.HEALING.attributeEntry,6),
            0x09356B,
            0xebcb6a);
    public static RegistryKey<ItemGroup> KEY = RegistryKey.of(Registries.ITEM_GROUP.getKey(),Identifier.of(RPGMinibosses.MOD_ID,"generic"));

    public static ItemGroup RPGENTITIES;
    public static void register() {

        for (var entry: entries) {
            entry.entityType  = Registry.register(
                    ENTITY_TYPE,
                    Identifier.of(MOD_ID, entry.id().getPath()),
                    FabricEntityTypeBuilder.<MinibossEntity>create(SpawnGroup.MONSTER, entry.entityFactory())
                            .dimensions(EntityDimensions.fixed(0.6F, 1.8F)) // dimensions in Minecraft units of the render
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


            BiomeModifications.addSpawn(BiomeSelectors.spawnsOneOf(EntityType.WITCH), SpawnGroup.MONSTER, entry.entityType,1,1,3);

            SpawnRestriction.register(entry.entityType, SpawnLocationTypes.ON_GROUND, Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, HostileEntity::canSpawnInDark);
            FabricDefaultAttributeRegistry.register(entry.entityType,entry.attributes);

        }
    }

}

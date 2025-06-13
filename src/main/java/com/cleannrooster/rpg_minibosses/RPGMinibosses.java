package com.cleannrooster.rpg_minibosses;

import com.cleannrooster.rpg_minibosses.block.RPGMinibossesBlocks;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.config.Default;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.OrbEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.item.Armors;
import com.cleannrooster.rpg_minibosses.item.SummonHorn;
import com.cleannrooster.rpg_minibosses.patrols.Patrol;
import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabric_extras.structure_pool.api.StructurePoolConfig;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.tag.InstrumentTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.spell_engine.api.config.ConfigFile;
import net.spell_engine.api.render.CustomModels;
import net.tinyconfig.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.minecraft.registry.Registries.ENTITY_TYPE;

public class RPGMinibosses implements ModInitializer {
	public static final String MOD_ID = "rpg-minibosses";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	public static Item LAVOSHORN;
	public static final Identifier EXPLOSION = Identifier.of("rpg-minibosses:explosion");
	public static final Identifier ANTICIPATION = Identifier.of("rpg-minibosses:boom");

	public static SoundEvent EXPLOSION_SOUND = SoundEvent.of(EXPLOSION);
	public static SoundEvent ANTICIPATION_SOUND = SoundEvent.of(ANTICIPATION);

	public static EntityType<OrbEntity> ORBENTITY;
	public static final Identifier INFAMY = Identifier.of(MOD_ID, "infamy");
	public static final Identifier BENEVOLENCE = Identifier.of(MOD_ID, "benevolence");

	public static ConfigManager<StructurePoolConfig> villageConfig = new ConfigManager<>
			("villages", Default.villageConfig)
			.builder()
			.setDirectory(MOD_ID)
			.sanitize(true)
			.build();
	public static ConfigManager<ConfigFile.Equipment> itemConfig = new ConfigManager<ConfigFile.Equipment>
			("items", Default.itemConfig)
			.builder()
			.setDirectory(MOD_ID)
			.sanitize(true)
			.build();
	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		Registry.register(Registries.CUSTOM_STAT, Identifier.of(MOD_ID,"infamy"), INFAMY);
		Registry.register(Registries.CUSTOM_STAT, Identifier.of(MOD_ID,"benevolence"), BENEVOLENCE);
		itemConfig.refresh();

		RPGMinibossesEntities.register();
		villageConfig.refresh();
		LAVOSHORN = new SummonHorn<MagusPrimeEntity>(new Item.Settings().maxCount(1).maxDamage(1),RPGMinibossesEntities.MAGuS_PRIME.entityType, InstrumentTags.GOAT_HORNS);
		Registry.register(Registries.ITEM,Identifier.of(MOD_ID,"lavos_horn"),LAVOSHORN);
		ItemGroupEvents.modifyEntriesEvent(RPGMinibossesEntities.KEY).register((content) -> {
			content.add(LAVOSHORN);
		});
        if (!FabricLoader.getInstance().isModLoaded("lithostitched")) {
            // Only inject the village if the Lithostitched is not present
            StructurePoolAPI.injectAll(villageConfig.value);
        }
		Registry.register(Registries.SOUND_EVENT, EXPLOSION, EXPLOSION_SOUND);
		Registry.register(Registries.SOUND_EVENT, ANTICIPATION, ANTICIPATION_SOUND);

		Effects.register();
		CustomModels.registerModelIds(List.of(
				Identifier.of(MOD_ID, "projectile/feather")
		));
		CustomModels.registerModelIds(List.of(
				Identifier.of(MOD_ID, "projectile/flamewaveprojectile")
		));
		ORBENTITY  = Registry.register(
				ENTITY_TYPE,
				Identifier.of(MOD_ID, "dark_matter"),
				FabricEntityTypeBuilder.<OrbEntity>create(SpawnGroup.MONSTER, OrbEntity::new)
						.dimensions(EntityDimensions.fixed(4, 4)) // dimensions in Minecraft units of the render
						.trackRangeBlocks(128)
						.trackedUpdateRate(1)
						.build()
		);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("spawnAnarchyPatrol").requires(serverCommandSource -> serverCommandSource.hasPermissionLevel(2))
				.executes((ctx) -> {
					// For versions below 1.19, replace "Text.literal" with "new LiteralText".
					// For versions below 1.20, remode "() ->" directly.

					PlayerEntity player = (((ServerCommandSource)ctx.getSource()).getPlayer());
					if(!player.getWorld().isClient()) {
						Patrol.forceSpawn((ServerWorld)player.getWorld(),true,true,player);

					}


					return 1;
				})));
		Patrol.patrolList.add(new Patrol());
		RPGMinibossesBlocks.register();
		Armors.register(itemConfig.value.weapons);
		LOGGER.info("Hello Fabric world!");
	}
}
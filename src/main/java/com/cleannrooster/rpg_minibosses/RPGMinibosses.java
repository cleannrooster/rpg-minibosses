package com.cleannrooster.rpg_minibosses;

import com.cleannrooster.rpg_minibosses.block.RPGMinibossesBlocks;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.config.Default;
import com.cleannrooster.rpg_minibosses.entity.GeminiEntity;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.OrbEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.item.Armors;
import com.cleannrooster.rpg_minibosses.item.SummonHorn;
import com.cleannrooster.rpg_minibosses.item.SummonItem;
import com.cleannrooster.rpg_minibosses.patrols.Patrol;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import me.emafire003.dev.structureplacerapi.StructurePlacerAPI;
import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabric_extras.structure_pool.api.StructurePoolConfig;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.player.*;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.NetherPortalBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.EmptyMapItem;
import net.minecraft.item.FilledMapItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.InstrumentTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.LocateCommand;
import net.minecraft.server.command.PlaceCommand;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.state.property.Properties;
import net.minecraft.test.StructureTestUtil;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockLocating;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.minecraft.world.dimension.NetherPortal;
import net.minecraft.world.gen.feature.EndPlatformFeature;
import net.minecraft.world.gen.feature.NetherPlacedFeatures;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.world.gen.structure.StructureKeys;
import net.minecraft.world.gen.structure.Structures;
import net.spell_engine.api.config.ConfigFile;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.mixin.entity.PlayerEntityEvents;
import net.tinyconfig.ConfigManager;
import org.jetbrains.annotations.Nullable;
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
	public static Item GEMINI;

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
		GEMINI = new SummonItem<>(new Item.Settings().maxCount(1).maxDamage(1),List.of(RPGMinibossesEntities.GEMINI_ALPHA.entityType,RPGMinibossesEntities.GEMINI_BETA.entityType), InstrumentTags.GOAT_HORNS);

		Registry.register(Registries.ITEM,Identifier.of(MOD_ID,"lavos_horn"),LAVOSHORN);
		Registry.register(Registries.ITEM,Identifier.of(MOD_ID,"gemini_fragment"),GEMINI);

		ItemGroupEvents.modifyEntriesEvent(RPGMinibossesEntities.KEY).register((content) -> {
			content.add(LAVOSHORN);
			content.add(GEMINI);
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
		PlayerBlockBreakEvents.BEFORE.register(((world, player, pos, state, blockEntity) -> {
            return !player.getWorld().getRegistryKey().equals(DIMENSIONKEY) || player.isCreative();
		}));
		UseItemCallback.EVENT.register(((player, world, hand) -> {
			if(world.getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative()) {
				return TypedActionResult.fail(player.getStackInHand(hand));
			}
			else{
				return TypedActionResult.pass(player.getStackInHand(hand));
			}
		}));
		UseBlockCallback.EVENT.register(((player,world,hand,blockHitResult) -> {
			if(world.getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative()) {
				return ActionResult.FAIL;
			}
			else{
				return ActionResult.PASS;
			}
		}));
		UseItemCallback.EVENT.register(((player, world, hand) -> {
			if(world.getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative()) {
				return TypedActionResult.fail(player.getStackInHand(hand));
			}
			else{
				return TypedActionResult.pass(player.getStackInHand(hand));
			}
		}));
		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player.getWorld().getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative()) {
                return ActionResult.FAIL;
            } else {
                return ActionResult.PASS;
            }
        });
		AttackBlockCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (player.getWorld().getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative()) {
				return ActionResult.FAIL;
			} else {
				return ActionResult.PASS;
			}
		});
		ServerEntityEvents.ENTITY_LOAD.register(((entity, world) -> {
			if(world.getRegistryKey().equals(DIMENSIONKEY) && entity instanceof PlayerEntity player){
				BlockPos pos = BlockPos.ofFloored(new Vec3d(74.5,74,19.5)).down();
				if(world.getBlockState(pos).isAir()){
					StructurePlacerAPI api =  new StructurePlacerAPI(world,Identifier.of("rpg-minibosses","guild_hall"),BlockPos.ofFloored(0,64,0));
					api.loadStructure();

				}
				entity.teleportTo(new TeleportTarget(world,new Vec3d(74.5,74,19.5), Vec3d.ZERO,entity.getYaw(),entity.getPitch(),TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
			}
		}));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("guildHideout")
				.executes((ctx) -> {
					// For versions below 1.19, replace "Text.literal" with "new LiteralText".
					// For versions below 1.20, remode "() ->" directly.
					if(RPGMinibossesEntities.config.guild && (ctx.getSource().getPlayer().getLastAttacker() == null || ( ctx.getSource().getPlayer().age - ctx.getSource().getPlayer().getLastAttackedTime() == 0) || (ctx.getSource().getPlayer().age - ctx.getSource().getPlayer().getLastAttackedTime() == 0)) && !ctx.getSource().getPlayer().getWorld().getRegistryKey().equals(DIMENSIONKEY)) {
						ctx.getSource().getPlayer().teleportTo(new TeleportTarget(ctx.getSource().getServer().getWorld(DIMENSIONKEY), new Vec3d(74.5, 74, 19.5), Vec3d.ZERO, ctx.getSource().getPlayer().getYaw(), ctx.getSource().getPlayer().getPitch(), TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));

					}
					return 1;
				})));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(CommandManager.literal("leaveGuildHideout")
				.executes((ctx) -> {
					// For versions below 1.19, replace "Text.literal" with "new LiteralText".
					// For versions below 1.20, remode "() ->" directly.
					if(RPGMinibossesEntities.config.guild && (ctx.getSource().getPlayer().getLastAttacker() == null || ( ctx.getSource().getPlayer().age - ctx.getSource().getPlayer().getLastAttackedTime() ==0) || (ctx.getSource().getPlayer().age - ctx.getSource().getPlayer().getLastAttackedTime() == 0))  && ctx.getSource().getPlayer().getWorld().getRegistryKey().equals(DIMENSIONKEY)) {

						ctx.getSource().getPlayer().teleportTo(ctx.getSource().getPlayer().getRespawnTarget(true, TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));

					}
					return 1;
				})));

		Patrol.patrolList.add(new Patrol());
		RPGMinibossesBlocks.register();
		Armors.register(itemConfig.value.weapons);
		Armors.registerArmors(itemConfig.value.armor_sets);
		itemConfig.save();

		LOGGER.info("Hello Fabric world!");
	}
	private static final DynamicCommandExceptionType STRUCTURE_INVALID_EXCEPTION = new DynamicCommandExceptionType((id) -> {
		return Text.stringifiedTranslatable("commands.locate.structure.invalid", new Object[]{id});
	});
	public static final RegistryKey<World> DIMENSIONKEY = RegistryKey.of(RegistryKeys.WORLD,Identifier.of("rpg-minibosses","guild_hall"));
	public TeleportTarget createTeleportTarget(ServerWorld world, Entity entity, BlockPos pos) {
		RegistryKey<World> registryKey = world.getRegistryKey() == World.OVERWORLD ? DIMENSIONKEY : World.OVERWORLD;
		ServerWorld serverWorld = world.getServer().getWorld(registryKey);

		if (serverWorld == null) {
			return null;
		} else {
			boolean bl = registryKey == DIMENSIONKEY;
            BlockLocating.Rectangle rectangle;

			if(bl){
				BlockState blockState = world.getBlockState(pos);

				rectangle = BlockLocating.getLargestRectangle(pos, Direction.Axis.X, 21, Direction.Axis.Y, 21, (posx) -> {
					return entity.getWorld().getBlockState(posx) == Blocks.LODESTONE.getDefaultState();
				});
				return getExitPortalTarget(entity, pos, rectangle, serverWorld);

			}
			return null;

		}
	}
	private static TeleportTarget getExitPortalTarget(Entity entity, BlockPos pos, BlockLocating.Rectangle exitPortalRectangle, ServerWorld world) {
		BlockState blockState = entity.getWorld().getBlockState(pos);
		Direction.Axis axis;
		Vec3d vec3d;
		BlockLocating.Rectangle rectangle = null;
		if (blockState.contains(Properties.HORIZONTAL_AXIS)) {
			axis = Direction.Axis.X;
			 rectangle = BlockLocating.getLargestRectangle(pos, axis, 21, Direction.Axis.Y, 21, (posx) -> {
				return world.getBlockState(posx) == Blocks.LODESTONE.getDefaultState();
			});
			vec3d = new Vec3d(0.5, 0.0, 0.0);
		} else {
			axis = Direction.Axis.X;
			vec3d = new Vec3d(0.5, 0.0, 0.0);
		}
		TeleportTarget.PostDimensionTransition postDimensionTransition = TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET;
		if(rectangle != null) {
			return getExitPortalTarget(world, rectangle, axis, vec3d, entity, entity.getVelocity(), entity.getYaw(), entity.getPitch(), postDimensionTransition);
		}
		else{
			return null;
		}
	}
	private static TeleportTarget getExitPortalTarget(ServerWorld world, BlockLocating.Rectangle exitPortalRectangle, Direction.Axis axis, Vec3d positionInPortal, Entity entity, Vec3d velocity, float yaw, float pitch, TeleportTarget.PostDimensionTransition postDimensionTransition) {
		BlockPos blockPos = exitPortalRectangle.lowerLeft;
		BlockState blockState = world.getBlockState(blockPos);
		Direction.Axis axis2 = (Direction.Axis)blockState.getOrEmpty(Properties.HORIZONTAL_AXIS).orElse(Direction.Axis.X);
		double d = (double)exitPortalRectangle.width;
		double e = (double)exitPortalRectangle.height;
		EntityDimensions entityDimensions = entity.getDimensions(entity.getPose());
		int i = axis == axis2 ? 0 : 90;
		Vec3d vec3d = axis == axis2 ? velocity : new Vec3d(velocity.z, velocity.y, -velocity.x);
		double f = (double)entityDimensions.width() / 2.0 + (d - (double)entityDimensions.width()) * positionInPortal.getX();
		double g = (e - (double)entityDimensions.height()) * positionInPortal.getY();
		double h = 0.5 + positionInPortal.getZ();
		boolean bl = axis2 == Direction.Axis.X;
		Vec3d vec3d2 = new Vec3d((double)blockPos.getX() + (bl ? f : h), (double)blockPos.getY() + g, (double)blockPos.getZ() + (bl ? h : f));
		Vec3d vec3d3 = NetherPortal.findOpenPosition(vec3d2, world, entity, entityDimensions);
		return new TeleportTarget(world, vec3d3, vec3d, yaw + (float)i, pitch, postDimensionTransition);
	}

}
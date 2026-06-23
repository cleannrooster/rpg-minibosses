package com.cleannrooster.rpg_minibosses;

import com.cleannrooster.rpg_minibosses.block.RPGMinibossesBlocks;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.config.Default;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.OrbEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.item.Armors;
import com.cleannrooster.rpg_minibosses.item.SummonHorn;
import com.cleannrooster.rpg_minibosses.item.SummonItem;
import com.cleannrooster.rpg_minibosses.patrols.Patrol;
import com.cleannrooster.rpg_minibosses.platform.Platform;
import net.fabric_extras.structure_pool.api.StructurePoolAPI;
import net.fabric_extras.structure_pool.api.StructurePoolConfig;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricEntityTypeBuilder;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.InstrumentTags;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypeFilter;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;
import net.spell_engine.api.config.ConfigFile;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.api.spell.event.SpellHandlers;
import net.spell_engine.rpg_series.loot.LootConfig;
import net.tiny_config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.minecraft.registry.Registries.ENTITY_TYPE;
import static net.spell_engine.rpg_series.config.Defaults.itemLootConfig;

/**
 * Loader-independent core. Does NOT implement {@code ModInitializer} / NeoForge entrypoint types.
 *
 * <p>Registration is decomposed so each registry-mutating method touches exactly one registry.
 * Fabric ({@code RPGMinibossesFabric}) calls them directly during {@code onInitialize}; NeoForge
 * ({@code RPGMinibossesNeoForge}) dispatches each into the matching {@code RegisterEvent} phase.
 * {@link #init()} performs only non-registry work (config, events, spell handlers).</p>
 */
public final class RPGMinibosses {
    /** Persistent content namespace — identical on every loader. */
    public static final String CONTENT_NAMESPACE = "rpg-minibosses";

    public static final Logger LOGGER = LoggerFactory.getLogger(CONTENT_NAMESPACE);

    public static Item LAVOSHORN;
    public static Item GEMINI;
    public static Item DEATOMIZED_FRAGMENT;

    public static final Identifier EXPLOSION = Identifier.of(CONTENT_NAMESPACE, "explosion");
    public static final Identifier ANTICIPATION = Identifier.of(CONTENT_NAMESPACE, "boom");
    public static final Identifier WARCRY = Identifier.of(CONTENT_NAMESPACE, "warcry");

    public static final SoundEvent EXPLOSION_SOUND = SoundEvent.of(EXPLOSION);
    public static final SoundEvent ANTICIPATION_SOUND = SoundEvent.of(ANTICIPATION);
    public static final SoundEvent WARCRY_SOUND = SoundEvent.of(WARCRY);

    public static EntityType<OrbEntity> ORBENTITY;

    public static final Identifier INFAMY = Identifier.of(CONTENT_NAMESPACE, "infamy");
    public static final Identifier BENEVOLENCE = Identifier.of(CONTENT_NAMESPACE, "benevolence");

    public static final RegistryKey<World> DIMENSIONKEY =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of(CONTENT_NAMESPACE, "guild_hall"));

    public static ConfigManager<StructurePoolConfig> villageConfig = new ConfigManager<>
            ("villages", Default.villageConfig)
            .builder()
            .setDirectory(CONTENT_NAMESPACE)
            .sanitize(true)
            .build();
    public static ConfigManager<ConfigFile.Equipment> itemConfig = new ConfigManager<ConfigFile.Equipment>
            ("items_v2", Default.itemConfig)
            .builder()
            .setDirectory(CONTENT_NAMESPACE)
            .sanitize(true)
            .build();

    private RPGMinibosses() {
    }

    /** Build a content {@link Identifier} under the shared namespace. */
    public static Identifier id(String path) {
        return Identifier.of(CONTENT_NAMESPACE, path);
    }

    // ── Non-registry initialization ─────────────────────────────────────────
    /** Load configs. Must run before {@link #registerItems()} (Armors read {@link #itemConfig}). */
    public static void initConfig() {
        itemConfig.refresh();
        villageConfig.refresh();
    }

    /** Spell handlers, gameplay events, world-gen injection, model ids. No registry mutation. */
    public static void init() {
        registerSpellHandlers();
        registerModelIds();
        registerEvents();
        registerWorldgenInjection();

        Patrol.patrolList.add(new Patrol());

        itemConfig.save();
        LOGGER.info("[{}] common init complete", CONTENT_NAMESPACE);
    }

    // ── Registry phases (one registry each) ─────────────────────────────────
    /** CUSTOM_STAT phase. */
    public static void registerStats() {
        Registry.register(Registries.CUSTOM_STAT, INFAMY, INFAMY);
        Registry.register(Registries.CUSTOM_STAT, BENEVOLENCE, BENEVOLENCE);
    }

    /** SOUND_EVENT phase. */
    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, EXPLOSION, EXPLOSION_SOUND);
        Registry.register(Registries.SOUND_EVENT, WARCRY, WARCRY_SOUND);
        Registry.register(Registries.SOUND_EVENT, ANTICIPATION, ANTICIPATION_SOUND);
    }

    /** ENTITY_TYPE phase. */
    public static void registerEntities() {
        RPGMinibossesEntities.registerEntityTypes();
        ORBENTITY = Registry.register(
                ENTITY_TYPE,
                id("dark_matter"),
                FabricEntityTypeBuilder.<OrbEntity>create(SpawnGroup.MONSTER, OrbEntity::new)
                        .dimensions(EntityDimensions.fixed(4, 4))
                        .trackRangeBlocks(128)
                        .trackedUpdateRate(1)
                        .build()
        );
        String W1 = "#rpg_series:loot_tier/tier_1_weapons";
        String W2 = "#rpg_series:loot_tier/tier_2_weapons";
        String W3 = "#rpg_series:loot_tier/tier_3_weapons";
        String   A2 = "#rpg_series:loot_tier/tier_2_armors";
        String   X2 = "#rpg_series:loot_tier/tier_2_accessories";
        String   R2 = "#rpg_series:loot_tier/tier_2_relics";
        for(RPGMinibossesEntities.Entry entry : RPGMinibossesEntities.entries) {
            if(entry.shouldSpawn){
                itemLootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                        new LootConfig.Pool().bonus_rolls(0.2F).rolls(2)
                                .add(W1, true,3)
                                .add(W2, true,3)
                                .add(A2, true,2)
                                .add(X2)
                                .add(R2))
                ;
            }
            else{
                itemLootConfig.injectors.put("rpg-minibosses:entities/"+entry.id.getPath(),
                        new LootConfig.Pool().bonus_rolls(0.2F).rolls(2)
                                .add(W2, true,3)
                                .add(W3, true,3)

                                .add(A2, true,2)
                                .add(X2)
                                .add(R2));
            }
        }
    }

    /** BLOCK phase. */
    public static void registerBlocks() {
        RPGMinibossesBlocks.registerBlocks();
    }

    /** MOB_EFFECT (status effect) phase. */
    public static void registerEffects() {
        Effects.register();
    }

    /** FEATURE phase. Adds the shared miniboss-encounter worldgen feature. */
    public static void registerFeatures() {
        com.cleannrooster.rpg_minibosses.worldgen.MinibossEncounters.registerFeature();
    }

    /**
     * ITEM phase. Per the artificers baseline (same author/ecosystem), NeoForge accepts armor-material
     * (static-init), item-group, and item registration all within the ITEM {@code RegisterEvent} — so
     * spawn eggs, summon items, weapons, armors, armor materials AND the creative tabs are registered
     * here rather than split across separate phases (which is what caused the frozen-registry crash).
     * Requires entity types (spawn eggs / summon items) and blocks (block items) to already exist.
     */
    public static void registerItems() {
        RPGMinibossesBlocks.registerBlockItems();
        RPGMinibossesEntities.registerSpawnEggs();

        LAVOSHORN = new SummonHorn<MagusPrimeEntity>(new Item.Settings().maxCount(1).maxDamage(1),
                RPGMinibossesEntities.MAGuS_PRIME.entityType, InstrumentTags.GOAT_HORNS);
        GEMINI = new SummonItem<>(new Item.Settings().maxCount(1).maxDamage(1),
                List.of(RPGMinibossesEntities.GEMINI_ALPHA.entityType, RPGMinibossesEntities.GEMINI_BETA.entityType),
                InstrumentTags.GOAT_HORNS, "Gemini");
        DEATOMIZED_FRAGMENT = new SummonItem<>(new Item.Settings().maxCount(1).maxDamage(1),
                List.of(RPGMinibossesEntities.GEMINI_ALPHA_UBER.entityType, RPGMinibossesEntities.GEMINI_BETA_UBER.entityType),
                InstrumentTags.GOAT_HORNS, "Eye of the Storm");

        Registry.register(Registries.ITEM, id("lavos_horn"), LAVOSHORN);
        Registry.register(Registries.ITEM, id("gemini_fragment"), GEMINI);
        Registry.register(Registries.ITEM, id("deatomized_fragment"), DEATOMIZED_FRAGMENT);

        Armors.register(itemConfig.value.weapons);
        Armors.registerArmors(itemConfig.value.armor_sets);

        // Creative tab (ITEM_GROUP) registered in-phase, like artificers — icon needs items above.
        RPGMinibossesEntities.registerItemGroup();
        ItemGroupEvents.modifyEntriesEvent(RPGMinibossesEntities.KEY).register((content) -> {
            content.add(LAVOSHORN);
            content.add(GEMINI);
            content.add(DEATOMIZED_FRAGMENT);
        });
        RPGMinibossesBlocks.addToItemGroup();
    }

    // ── Event / handler registration (no registry mutation; FFAPI-backed on NeoForge) ─────
    private static void registerSpellHandlers() {
        // Custom spell delivery: spawn a StormAnchorEntity that follows the caster for 30s.
        SpellHandlers.registerCustomDelivery(
                id("summon_storm"),
                (world, spellRegistry, caster, targets, context, position) -> {
                    com.cleannrooster.rpg_minibosses.entity.StormAnchorEntity storm =
                            new com.cleannrooster.rpg_minibosses.entity.StormAnchorEntity(
                                    RPGMinibossesEntities.STORM_ANCHOR, world);
                    storm.setPosition(caster.getX(), caster.getY(), caster.getZ());
                    storm.setOwnerEntity(caster);
                    storm.setPlayerCentered(true);
                    storm.setLifespan(30 * 20);
                    world.spawnEntity(storm);
                    return true;
                }
        );
    }

    private static void registerModelIds() {
        CustomModels.registerModelIds(List.of(id("projectile/feather")));
        CustomModels.registerModelIds(List.of(id("projectile/flamewaveprojectile")));
        CustomModels.registerModelIds(List.of(id("projectile/iron_dagger")));
    }

    private static void registerWorldgenInjection() {
        if (!Platform.isModLoaded("lithostitched")) {
            // Only inject the village if Lithostitched is not present.
            StructurePoolAPI.injectAll(villageConfig.value);
        }
        // Add the miniboss encounter placed features to their preferred biomes (loader-agnostic via FFAPI).
        com.cleannrooster.rpg_minibosses.worldgen.MinibossEncounters.injectBiomes();
    }

    private static void registerEvents() {
        // Deferred miniboss-encounter spawns: feature records the marker, this spawns it on chunk load.
        com.cleannrooster.rpg_minibosses.worldgen.MinibossEncounterSpawnManager.register();

        // Creative-tab additions for the summon items (callback re-reads the static fields at fire time).


        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("spawnAnarchyPatrol")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes((ctx) -> {
                            PlayerEntity player = ((ServerCommandSource) ctx.getSource()).getPlayer();
                            if (player != null && !player.getWorld().isClient()) {
                                Patrol.forceSpawn((ServerWorld) player.getWorld(), true, true, player);
                            }
                            return 1;
                        })));

        PlayerBlockBreakEvents.BEFORE.register(((world, player, pos, state, blockEntity) ->
                !player.getWorld().getRegistryKey().equals(DIMENSIONKEY) || player.isCreative()));

        UseItemCallback.EVENT.register(((player, world, hand) -> {
            if (world.getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative()) {
                return TypedActionResult.fail(player.getStackInHand(hand));
            }
            return TypedActionResult.pass(player.getStackInHand(hand));
        }));
        UseBlockCallback.EVENT.register(((player, world, hand, blockHitResult) ->
                world.getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative() ? ActionResult.FAIL : ActionResult.PASS));
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
                player.getWorld().getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative() ? ActionResult.FAIL : ActionResult.PASS);
        AttackBlockCallback.EVENT.register((player, world, hand, pos, direction) ->
                player.getWorld().getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative() ? ActionResult.FAIL : ActionResult.PASS);

        ServerEntityEvents.ENTITY_LOAD.register(((entity, world) -> {
            if (world.getRegistryKey().equals(DIMENSIONKEY) && entity instanceof PlayerEntity) {
                BlockPos pos = BlockPos.ofFloored(new Vec3d(74.5, 74, 19.5)).down();
                if (world.getBlockState(pos).isAir()) {
                    Platform.placeGuildHall(world, BlockPos.ofFloored(0, 64, 0));
                }
                entity.teleportTo(new TeleportTarget(world, new Vec3d(74.5, 74, 19.5), Vec3d.ZERO,
                        entity.getYaw(), entity.getPitch(), TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
            }
        }));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("guildHideout")
                        .executes((ctx) -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p != null && RPGMinibossesEntities.config.guild
                                    && (p.getLastAttacker() == null || p.age - p.getLastAttackedTime() == 0)
                                    && !p.getWorld().getRegistryKey().equals(DIMENSIONKEY)) {
                                p.teleportTo(new TeleportTarget(ctx.getSource().getServer().getWorld(DIMENSIONKEY),
                                        new Vec3d(74.5, 74, 19.5), Vec3d.ZERO, p.getYaw(), p.getPitch(),
                                        TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                            }
                            return 1;
                        })));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("leaveGuildHideout")
                        .executes((ctx) -> {
                            ServerPlayerEntity p = ctx.getSource().getPlayer();
                            if (p != null && RPGMinibossesEntities.config.guild
                                    && (p.getLastAttacker() == null || p.age - p.getLastAttackedTime() == 0)
                                    && p.getWorld().getRegistryKey().equals(DIMENSIONKEY)) {
                                p.teleportTo(p.getRespawnTarget(true, TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
                            }
                            return 1;
                        })));

        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayerEntity) {
                entity.getWorld().getEntitiesByType(
                        TypeFilter.instanceOf(MagusPrimeEntity.class),
                        entity.getBoundingBox().expand(64),
                        magus -> true
                ).forEach(magus -> {
                    magus.contemptFulfilledStacks++;
                    EntityAttributeInstance atkSpeed = magus.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_SPEED);
                    if (atkSpeed != null) {
                        atkSpeed.removeModifier(MagusPrimeEntity.CONTEMPT_ATKSPEED_ID);
                        atkSpeed.addPersistentModifier(new EntityAttributeModifier(
                                MagusPrimeEntity.CONTEMPT_ATKSPEED_ID,
                                magus.contemptFulfilledStacks * 0.04,
                                EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                    }
                });
            }
        });
    }
}

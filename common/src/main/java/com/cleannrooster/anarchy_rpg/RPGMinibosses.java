package com.cleannrooster.anarchy_rpg;

import com.cleannrooster.anarchy_rpg.block.RPGMinibossesBlocks;
import com.cleannrooster.anarchy_rpg.client.entity.effect.Effects;
import com.cleannrooster.anarchy_rpg.compat.SpellEngineCompat;
import com.cleannrooster.anarchy_rpg.compat.StructurePoolCompat;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.item.SummonItem;
import com.cleannrooster.anarchy_rpg.patrols.Patrol;
import com.cleannrooster.anarchy_rpg.platform.Platform;
import dev.architectury.event.CompoundEventResult;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.BlockEvent;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import dev.architectury.event.events.common.InteractionEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.registry.CreativeTabRegistry;
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
import net.tiny_config.ConfigManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static net.minecraft.registry.Registries.ENTITY_TYPE;

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
    public static final String CONTENT_NAMESPACE = "anarchy_rpg";

    public static final Logger LOGGER = LoggerFactory.getLogger(CONTENT_NAMESPACE);

    public static Item GEMINI;
    public static Item DEATOMIZED_FRAGMENT;

    public static final Identifier EXPLOSION = Identifier.of(CONTENT_NAMESPACE, "explosion");
    public static final Identifier ANTICIPATION = Identifier.of(CONTENT_NAMESPACE, "boom");
    public static final Identifier WARCRY = Identifier.of(CONTENT_NAMESPACE, "warcry");

    public static final SoundEvent EXPLOSION_SOUND = SoundEvent.of(EXPLOSION);
    public static final SoundEvent ANTICIPATION_SOUND = SoundEvent.of(ANTICIPATION);
    public static final SoundEvent WARCRY_SOUND = SoundEvent.of(WARCRY);

    public static final RegistryKey<World> DIMENSIONKEY =
            RegistryKey.of(RegistryKeys.WORLD, Identifier.of(CONTENT_NAMESPACE, "guild_hall"));


    private RPGMinibosses() {
    }

    /** Build a content {@link Identifier} under the shared namespace. */
    public static Identifier id(String path) {
        return Identifier.of(CONTENT_NAMESPACE, path);
    }

    // ── Non-registry initialization ─────────────────────────────────────────
    /** Load configs. Must run before {@link #registerItems()} (Spell Engine equipment reads its item config). */
    public static void initConfig() {
        SpellEngineCompat.hooks().initConfig();
        // Wanted posters are deprecated in 3.0: no villages config, no poster house (StructurePoolCompat).
    }

    /** Spell handlers, gameplay events, world-gen injection, model ids. No registry mutation. */
    public static void init() {
        // Declared on both sides: the server needs the payload type to send, the client to receive.
        com.cleannrooster.anarchy_rpg.entity.combat.net.AttackNetwork.register();
        com.cleannrooster.anarchy_rpg.magic.TickScheduler.register();
        com.cleannrooster.anarchy_rpg.magic.MagicDebugCommand.register();
        com.cleannrooster.anarchy_rpg.faction.Factions.register();
        com.cleannrooster.anarchy_rpg.conversation.Conversations.register();
        com.cleannrooster.anarchy_rpg.exchange.ExchangeMenus.register();
        com.cleannrooster.anarchy_rpg.hub.HubTurnover.register();
        com.cleannrooster.anarchy_rpg.companion.Companions.register();
        com.cleannrooster.anarchy_rpg.rumor.Rumors.register();
        com.cleannrooster.anarchy_rpg.journal.GuildJournal.register();
        com.cleannrooster.anarchy_rpg.service.CampServices.register();
        com.cleannrooster.anarchy_rpg.hub.CampMap.register();
        SpellEngineCompat.hooks().init();
        registerEvents();
        registerWorldgenInjection();

        Patrol.patrolList.add(new Patrol());

        LOGGER.info("[{}] common init complete", CONTENT_NAMESPACE);
    }

    // ── Registry phases (one registry each) ─────────────────────────────────
    /** SOUND_EVENT phase. */
    public static void registerSounds() {
        Registry.register(Registries.SOUND_EVENT, EXPLOSION, EXPLOSION_SOUND);
        Registry.register(Registries.SOUND_EVENT, WARCRY, WARCRY_SOUND);
        Registry.register(Registries.SOUND_EVENT, ANTICIPATION, ANTICIPATION_SOUND);
        com.cleannrooster.anarchy_rpg.magic.BossSounds.register();
    }

    /** ENTITY_TYPE phase. */
    public static void registerEntities() {
        SpellEngineCompat.hooks().declareEntities();
        RPGMinibossesEntities.registerEntityTypes();
        com.cleannrooster.anarchy_rpg.magic.entity.MagicEntities.register();
        SpellEngineCompat.hooks().registerEntities();
    }

    /** BLOCK phase. */
    public static void registerBlocks() {
        RPGMinibossesBlocks.registerBlocks();
    }

    /** MOB_EFFECT (status effect) phase. */
    public static void registerEffects() {
        SpellEngineCompat.hooks().declareEffects();
        Effects.register();
    }

    /** FEATURE phase. Adds the shared miniboss-encounter worldgen feature. */
    public static void registerFeatures() {
        com.cleannrooster.anarchy_rpg.worldgen.MinibossEncounters.registerFeature();
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

        SpellEngineCompat.hooks().registerSummonItems();
        GEMINI = new SummonItem<>(new Item.Settings().maxCount(1).maxDamage(1),
                List.of(RPGMinibossesEntities.GEMINI_ALPHA.entityType, RPGMinibossesEntities.GEMINI_BETA.entityType),
                InstrumentTags.GOAT_HORNS, "Gemini");
        DEATOMIZED_FRAGMENT = new SummonItem<>(new Item.Settings().maxCount(1).maxDamage(1),
                List.of(RPGMinibossesEntities.GEMINI_ALPHA_UBER.entityType, RPGMinibossesEntities.GEMINI_BETA_UBER.entityType),
                InstrumentTags.GOAT_HORNS, "Eye of the Storm");

        Registry.register(Registries.ITEM, id("gemini_fragment"), GEMINI);
        Registry.register(Registries.ITEM, id("deatomized_fragment"), DEATOMIZED_FRAGMENT);

        SpellEngineCompat.hooks().registerEquipment();

        // Creative tab (ITEM_GROUP) registered in-phase, like artificers — icon needs items above.
        RPGMinibossesEntities.registerItemGroup();
        CreativeTabRegistry.append(CreativeTabRegistry.defer(RPGMinibossesEntities.KEY), GEMINI, DEATOMIZED_FRAGMENT);
        RPGMinibossesBlocks.addToItemGroup();
    }

    // ── Event / handler registration (no registry mutation; Architectury-backed on both loaders) ─────
    private static void registerWorldgenInjection() {
        // The wanted-poster village house is deprecated in 3.0: neither StructurePoolCompat nor the
        // Lithostitched modifiers (shipped as .json.deprecated) inject it any more.
        // Add the miniboss encounter placed features to their preferred biomes (loader-agnostic via Architectury).
        com.cleannrooster.anarchy_rpg.worldgen.MinibossEncounters.injectBiomes();
    }

    private static void registerEvents() {
        // Deferred miniboss-encounter spawns: feature records the marker, this spawns it on chunk load.
        com.cleannrooster.anarchy_rpg.worldgen.MinibossEncounterSpawnManager.register();

        // Creative-tab additions for the summon items (callback re-reads the static fields at fire time).


        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("spawnAnarchyPatrol")
                        .requires(src -> src.hasPermissionLevel(2))
                        .executes((ctx) -> {
                            PlayerEntity player = ((ServerCommandSource) ctx.getSource()).getPlayer();
                            if (player != null && !player.getWorld().isClient()) {
                                Patrol.forceSpawn((ServerWorld) player.getWorld(), true, true, player);
                            }
                            return 1;
                        })));

    }


    /**
     * The guild hall: deprecated in 3.0. Its dimension is disabled (data/anarchy_rpg/dimension/guild_hall.json
     * is shipped as .deprecated), and these hooks and the /guildHideout and /leaveGuildHideout commands are no
     * longer registered. Kept intact for its return in 3.1 (M7).
     */
    @Deprecated
    @SuppressWarnings("unused")
    private static void registerGuildHall() {
        // The guild hall is look-only for anyone not in creative.
        BlockEvent.BREAK.register((world, pos, state, player, xp) ->
                isLockedInGuildHall(player) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.RIGHT_CLICK_ITEM.register((player, hand) ->
                isLockedInGuildHall(player) ? CompoundEventResult.interruptFalse(player.getStackInHand(hand)) : CompoundEventResult.pass());
        InteractionEvent.RIGHT_CLICK_BLOCK.register((player, hand, pos, face) ->
                isLockedInGuildHall(player) ? EventResult.interruptFalse() : EventResult.pass());
        PlayerEvent.ATTACK_ENTITY.register((player, world, target, hand, hitResult) ->
                isLockedInGuildHall(player) ? EventResult.interruptFalse() : EventResult.pass());
        InteractionEvent.LEFT_CLICK_BLOCK.register((player, hand, pos, face) ->
                isLockedInGuildHall(player) ? EventResult.interruptFalse() : EventResult.pass());

        // A player arriving in the guild hall by any route — joining, changing dimension, respawning —
        // lands on the hall's floor, building the hall first if this world has never had one.
        PlayerEvent.PLAYER_JOIN.register(RPGMinibosses::placeInGuildHall);
        PlayerEvent.CHANGE_DIMENSION.register((player, from, to) -> placeInGuildHall(player));
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, reason) -> placeInGuildHall(player));

        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
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
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
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
    }

    @Deprecated
    private static boolean isLockedInGuildHall(PlayerEntity player) {
        return player.getWorld().getRegistryKey().equals(DIMENSIONKEY) && !player.isCreative();
    }

    @Deprecated
    private static void placeInGuildHall(ServerPlayerEntity player) {
        if (!(player.getWorld() instanceof ServerWorld world) || !world.getRegistryKey().equals(DIMENSIONKEY)) {
            return;
        }
        BlockPos pos = BlockPos.ofFloored(new Vec3d(74.5, 74, 19.5)).down();
        if (world.getBlockState(pos).isAir()) {
            Platform.placeGuildHall(world, BlockPos.ofFloored(0, 64, 0));
        }
        player.teleportTo(new TeleportTarget(world, new Vec3d(74.5, 74, 19.5), Vec3d.ZERO,
                player.getYaw(), player.getPitch(), TeleportTarget.SEND_TRAVEL_THROUGH_PORTAL_PACKET));
    }
}

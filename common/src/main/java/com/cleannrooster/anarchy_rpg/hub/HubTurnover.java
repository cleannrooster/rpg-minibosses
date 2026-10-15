package com.cleannrooster.anarchy_rpg.hub;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.faction.Faction;
import com.cleannrooster.anarchy_rpg.faction.FactionFlag;
import com.cleannrooster.anarchy_rpg.faction.FactionState;
import com.cleannrooster.anarchy_rpg.faction.PartyMembership;
import com.cleannrooster.anarchy_rpg.faction.PackedFlag;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BannerPatternsComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;
import net.minecraft.world.gen.structure.Structure;
import net.minecraft.structure.StructureStart;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Camps and towers as living hubs. Adventurers come and go on a schedule, which is what keeps their
 * Exchange stock fresh: a resident's stock never refills, but residents are replaced.
 *
 * <p>Found by scanning loaded chunks near players for hub structure starts. The first time a hub is seen it
 * gets a party (so a faction and flag), its residents, and a banner. After that, each time it comes back
 * into loaded range, whole rotation intervals that passed while it was away are caught up in one step: the
 * longest-staying residents leave and the hub refills. World time drives all of it, never a ticking timer,
 * so a hub nobody visits costs nothing.
 *
 * <p>A hub never rotates while a player is close enough to see it, so nobody watches adventurers vanish
 * or appear; the rotation waits for a later scan with nobody near. (First setup does not wait.) Residents killed or recruited are
 * not replaced until the next rotation, so a hub can be emptied for a while but not farmed.
 */
public final class HubTurnover {
    private static final int SCAN_INTERVAL = 100;
    /** Wait this long after a hub loads before touching it, so its residents' entities have loaded too. */
    private static final int SETTLE_TICKS = 100;
    private static final int SPAWN_ATTEMPTS = 48;

    /** Per world: hubs currently loaded, and the tick each was first seen in this stretch of being loaded. */
    private static final Map<ServerWorld, Map<Long, Long>> LOADED = new HashMap<>();
    /** Per world: hubs already handled during the current stretch of being loaded. */
    private static final Map<ServerWorld, Set<Long>> HANDLED = new HashMap<>();

    private HubTurnover() {
    }

    /** Hubs within this distance of a player are sent to them as banner markers. */
    private static final double MARKER_SEND_RADIUS = 96.0;

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            // A client declares this by registering its receiver; declaring it twice is an error.
            NetworkManager.registerS2CPayloadType(HubMarkersPayload.ID, HubMarkersPayload.CODEC);
        }
        TickEvent.SERVER_LEVEL_POST.register(HubTurnover::tick);
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            LOADED.clear();
            HANDLED.clear();
        });
        HubDebugCommand.register();
    }

    private static void tick(ServerWorld world) {
        if (world.getTime() % SCAN_INTERVAL != 0 || world.getPlayers().isEmpty()) {
            return;
        }
        HubState state = HubState.get(world);
        Map<Long, Long> loaded = LOADED.computeIfAbsent(world, w -> new HashMap<>());
        Set<Long> handled = HANDLED.computeIfAbsent(world, w -> new HashSet<>());
        Set<Long> seen = new HashSet<>();
        Registry<Structure> structures = world.getRegistryManager().get(RegistryKeys.STRUCTURE);
        int radius = RPGMinibossesEntities.config.hubScanRadiusChunks;

        for (ServerPlayerEntity player : world.getPlayers()) {
            ChunkPos center = player.getChunkPos();
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    WorldChunk chunk = world.getChunkManager().getWorldChunk(center.x + dx, center.z + dz);
                    if (chunk == null) {
                        continue;
                    }
                    for (Map.Entry<Structure, StructureStart> entry : chunk.getStructureStarts().entrySet()) {
                        HubKind kind = kindOf(structures, entry.getKey());
                        StructureStart start = entry.getValue();
                        if (kind == null || !start.hasChildren()) {
                            continue;
                        }
                        long key = start.getPos().toLong();
                        seen.add(key);
                        if (!state.anchors.containsKey(key)) {
                            state.anchors.put(key, new HubAnchor(kind, start.getBoundingBox()));
                            state.markDirty();
                        }
                        loaded.putIfAbsent(key, world.getTime());
                    }
                }
            }
        }
        // A hub that dropped out of range is handled again the next time it loads.
        loaded.keySet().retainAll(seen);
        handled.retainAll(seen);

        sendMarkers(world, state);

        for (Map.Entry<Long, Long> entry : loaded.entrySet()) {
            long key = entry.getKey();
            if (handled.contains(key) || world.getTime() - entry.getValue() < SETTLE_TICKS) {
                continue;
            }
            HubAnchor anchor = state.anchors.get(key);
            if (anchor != null && handle(world, anchor, false)) {
                handled.add(key);
                state.markDirty();
            }
        }
    }

    /** Tells each player where the set-up hubs near them are, and which flag each flies. */
    private static void sendMarkers(ServerWorld world, HubState state) {
        double radiusSq = MARKER_SEND_RADIUS * MARKER_SEND_RADIUS;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!NetworkManager.canPlayerReceive(player, HubMarkersPayload.ID)) {
                continue;
            }
            List<HubMarkersPayload.Marker> markers = new ArrayList<>();
            for (HubAnchor anchor : state.anchors.values()) {
                if (anchor.party == null) {
                    continue;
                }
                BlockPos center = anchor.center();
                if (player.squaredDistanceTo(center.getX() + 0.5, player.getY(), center.getZ() + 0.5) > radiusSq) {
                    continue;
                }
                var flag = FactionState.roster(world.getServer()).get(anchor.party.faction()).flag();
                BlockPos marker = new BlockPos(center.getX(), anchor.bounds.getMaxY() + 2, center.getZ());
                markers.add(new HubMarkersPayload.Marker(marker, PackedFlag.pack(flag, world.getRegistryManager())));
            }
            NetworkManager.sendToPlayer(player, new HubMarkersPayload(markers));
        }
    }

    private static @Nullable HubKind kindOf(Registry<Structure> structures, Structure structure) {
        var entry = structures.getEntry(structure);
        for (HubKind kind : HubKind.values()) {
            if (entry.isIn(kind.tag())) {
                return kind;
            }
        }
        return null;
    }

    /**
     * Sets up or rotates a hub. Returns true once the hub is settled for this stretch of being loaded;
     * false if it must wait (a player is watching), so it is tried again on a later scan.
     *
     * @param force rotate now regardless of the clock and of who is watching (operator command)
     */
    static boolean handle(ServerWorld world, HubAnchor anchor, boolean force) {
        ServerConfig config = RPGMinibossesEntities.config;
        long now = world.getTime();
        // First setup never waits: a player arriving straight into a camp should not find it empty.
        if (!anchor.initialized) {
            anchor.party = PartyMembership.create(world.getRandom());
            anchor.lastRotation = now;
            anchor.initialized = true;
            fill(world, anchor, config.hubResidents);
            placeBanner(world, anchor);
            return true;
        }
        if (!force && isWatched(world, anchor, config.hubWatchRadius)) {
            return false;
        }
        long interval = Math.max(1L, config.hubRotationTicks);
        long missed = force ? Math.max(1, (now - anchor.lastRotation) / interval) : (now - anchor.lastRotation) / interval;
        if (missed < 1) {
            return true;
        }
        pruneDeparted(world, anchor);
        int retire = (int) Math.min((long) config.hubRetireCount * missed, anchor.residents.size());
        retireLongestStaying(world, anchor, retire);
        fill(world, anchor, config.hubResidents);
        anchor.lastRotation = force ? now : anchor.lastRotation + missed * interval;
        return true;
    }

    private static boolean isWatched(ServerWorld world, HubAnchor anchor, double radius) {
        BlockPos center = anchor.center();
        double radiusSq = radius * radius;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (!player.isSpectator() && player.squaredDistanceTo(center.getX() + 0.5, center.getY(), center.getZ() + 0.5) < radiusSq) {
                return true;
            }
        }
        return false;
    }

    /** Residents who died, were recruited, or are otherwise gone leave the roster; their places open up. */
    private static void pruneDeparted(ServerWorld world, HubAnchor anchor) {
        anchor.residents.removeIf(resident -> {
            Entity entity = world.getEntity(resident.uuid());
            return !(entity instanceof MinibossEntity npc) || !npc.isAlive() || npc.getOwnerUuid() != null;
        });
    }

    /** The longest-staying leave first. Anyone mid-conversation or mid-trade stays. */
    private static void retireLongestStaying(ServerWorld world, HubAnchor anchor, int count) {
        List<HubAnchor.Resident> byArrival = new ArrayList<>(anchor.residents);
        byArrival.sort(Comparator.comparingLong(HubAnchor.Resident::arrivedAt));
        int retired = 0;
        for (HubAnchor.Resident resident : byArrival) {
            if (retired >= count) {
                break;
            }
            if (world.getEntity(resident.uuid()) instanceof MinibossEntity npc
                    && npc.getConversationPartner() == null && npc.getCustomer() == null) {
                npc.discard();
                anchor.residents.remove(resident);
                retired++;
            }
        }
    }

    /** Brings the hub up to {@code target} residents, all in the hub's party. */
    private static void fill(ServerWorld world, HubAnchor anchor, int target) {
        List<RPGMinibossesEntities.Entry> types = RPGMinibossesEntities.minibosses;
        if (types.isEmpty() || anchor.party == null) {
            return;
        }
        Random random = world.getRandom();
        while (anchor.residents.size() < target) {
            BlockPos pos = findStandable(world, anchor.bounds, random);
            if (pos == null) {
                return;
            }
            EntityType<?> type = types.get(random.nextInt(types.size())).entityType;
            if (!(type.create(world) instanceof MinibossEntity npc)) {
                return;
            }
            npc.refreshPositionAndAngles(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            npc.initialize(world, world.getLocalDifficulty(pos), SpawnReason.STRUCTURE, null);
            npc.markFromStructure();
            npc.setPartyMembership(anchor.party);
            if (!world.spawnEntity(npc)) {
                return;
            }
            anchor.residents.add(new HubAnchor.Resident(npc.getUuid(), world.getTime()));
        }
    }

    /**
     * A loaded spot inside the hub with solid ground and two blocks of air. Columns are scanned from the
     * bottom up, so adventurers land on the ground floor (inside a tower, not on its roof). The spot must
     * see sky light: a hub's bounds reach below the surface, and the first air pocket from the bottom is
     * otherwise often a cave under the camp.
     */
    private static @Nullable BlockPos findStandable(ServerWorld world, BlockBox bounds, Random random) {
        for (int attempt = 0; attempt < SPAWN_ATTEMPTS; attempt++) {
            int x = bounds.getMinX() + 1 + random.nextInt(Math.max(1, bounds.getBlockCountX() - 2));
            int z = bounds.getMinZ() + 1 + random.nextInt(Math.max(1, bounds.getBlockCountZ() - 2));
            if (!world.isChunkLoaded(ChunkPos.toLong(x >> 4, z >> 4))) {
                continue;
            }
            for (int y = bounds.getMinY() + 1; y < bounds.getMaxY(); y++) {
                BlockPos pos = new BlockPos(x, y, z);
                if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                        && world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir()
                        && world.getFluidState(pos).isEmpty()
                        && world.getLightLevel(LightType.SKY, pos) > 0) {
                    return pos;
                }
            }
        }
        return null;
    }

    /** A standing banner in the hub's faction colors, on standable ground near the hub's center. */
    private static void placeBanner(ServerWorld world, HubAnchor anchor) {
        if (anchor.party == null) {
            return;
        }
        Faction faction = FactionState.roster(world.getServer()).get(anchor.party.faction());
        FactionFlag flag = faction.flag();
        BlockPos center = anchor.center();
        BlockBox near = new BlockBox(center.getX() - 4, anchor.bounds.getMinY(), center.getZ() - 4,
                center.getX() + 4, anchor.bounds.getMaxY(), center.getZ() + 4);
        BlockPos pos = findStandable(world, near, world.getRandom());
        if (pos == null) {
            return;
        }
        BlockState banner = BannerBlock.getForColor(flag.base()).getDefaultState();
        world.setBlockState(pos, banner);
        if (world.getBlockEntity(pos) instanceof BannerBlockEntity blockEntity) {
            ItemStack stack = new ItemStack(banner.getBlock().asItem());
            world.getRegistryManager().get(RegistryKeys.BANNER_PATTERN)
                    .getEntry(RegistryKey.of(RegistryKeys.BANNER_PATTERN, flag.pattern()))
                    .ifPresent(pattern -> stack.set(DataComponentTypes.BANNER_PATTERNS,
                            new BannerPatternsComponent.Builder().add(pattern, flag.patternColor()).build()));
            stack.set(DataComponentTypes.CUSTOM_NAME, faction.name());
            blockEntity.readFrom(stack, flag.base());
            blockEntity.markDirty();
        }
    }

    /** The hub nearest a position in this world, within the given distance of its center. */
    static @Nullable HubAnchor nearest(ServerWorld world, BlockPos pos, double maxDistance) {
        HubAnchor best = null;
        double bestSq = maxDistance * maxDistance;
        for (HubAnchor anchor : HubState.get(world).anchors.values()) {
            double d = anchor.center().getSquaredDistance(pos);
            if (d < bestSq) {
                bestSq = d;
                best = anchor;
            }
        }
        return best;
    }
}

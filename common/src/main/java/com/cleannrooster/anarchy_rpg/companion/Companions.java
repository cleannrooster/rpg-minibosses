package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.tag.DamageTypeTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.UUID;

/**
 * The companion system: recruiting, orders, immortality, travel home, and bringing lost companions back.
 *
 * <p>Invariants:
 * <ul>
 *   <li>One companion per player. Recruiting refuses rather than replacing; dismissing frees the slot.</li>
 *   <li>Immortal. A lethal blow downs the companion for a while; it never dies. Damage that ignores
 *   immortality (void, /kill) kills the entity, and the companion is re-created at base.</li>
 *   <li>Every clock is world time, so an unloaded companion's timers are simply checked when it loads or
 *   when the sweep runs.</li>
 *   <li>A Stay always ends: there is no order that stays forever.</li>
 *   <li>At most one live copy: a copy whose incarnation is behind the record's removes itself, and nothing
 *   is re-created while a live entity with the companion's UUID exists anywhere.</li>
 * </ul>
 */
public final class Companions {
    private static final int SWEEP_INTERVAL = 100;
    /** How long a companion may be missing from its own loaded chunk before it counts as lost. */
    private static final long LOST_AFTER = 200;
    private static final int SNAPSHOT_INTERVAL = 200;
    private static final double ARRIVED = 3.0;
    /** A companion at base starts walking again if its base has moved further than this. */
    private static final double BASE_MOVED = 8.0;
    /** A departing ex-companion walks this far away before simply leaving. */
    private static final double DEPART_DISTANCE = 64.0;
    private static final double DEPART_WATCH = 48.0;
    private static final int DEPART_MAX_TICKS = 2400;

    public enum RecruitResult { BOUND, ALREADY_HAS_COMPANION }

    private Companions() {
    }

    public static void register() {
        TickEvent.SERVER_POST.register(server -> {
            if (server.getTicks() % SWEEP_INTERVAL == 0) {
                sweep(server);
            }
        });
        CompanionOptions.register();
        CompanionMenus.registerOption();
        CompanionDebugCommand.register();
    }

    static long now(MinecraftServer server) {
        return server.getOverworld().getTime();
    }

    private static void say(ServerPlayerEntity player, MinibossEntity npc, String line) {
        player.sendMessage(npc.getName().copy().append(Text.literal(": " + line)));
    }

    // ── Recruiting ──────────────────────────────────────────────────────────

    /** Makes {@code npc} this player's companion, if they have none yet. */
    public static RecruitResult tryRecruit(ServerPlayerEntity player, MinibossEntity npc) {
        CompanionRegistry registry = CompanionRegistry.get(player.getServer());
        CompanionRecord record = registry.bind(player.getUuid(), npc.getUuid());
        if (record == null) {
            return RecruitResult.ALREADY_HAS_COMPANION;
        }
        npc.setOwnerUuid(player.getUuid());
        record.incarnation = 1;
        npc.setCompanionIncarnation(record.incarnation);
        record.mode = CompanionMode.FOLLOWING;
        npc.setSitting(false);
        // Its trade stock is not the player's to take: storage starts empty and stays the companion's own.
        npc.getExchangeStock().clearForRecruit();
        // What it already wears and holds stays its own: replaceable, never taken out.
        CompanionGear.bindExisting(npc);
        snapshot(npc, record);
        registry.markDirty();
        return RecruitResult.BOUND;
    }

    // ── Base ────────────────────────────────────────────────────────────────

    public static GlobalPos spawnOf(ServerPlayerEntity player) {
        BlockPos spawn = player.getSpawnPointPosition();
        if (spawn != null) {
            return GlobalPos.create(player.getSpawnPointDimension(), spawn);
        }
        return GlobalPos.create(World.OVERWORLD, player.getServer().getOverworld().getSpawnPos());
    }

    /**
     * The owner's rendezvous point: the only place orders and timers send a companion. Null until the
     * owner sets one. A rendezvous no longer near the spawn point is dropped here.
     */
    public static @Nullable GlobalPos rendezvousOf(MinecraftServer server, UUID owner) {
        CompanionRegistry registry = CompanionRegistry.get(server);
        GlobalPos rendezvous = registry.rendezvousOf(owner);
        if (rendezvous != null && !withinRendezvousRadius(rendezvous, spawnPointOf(server, owner))) {
            registry.setRendezvous(owner, null);
            return null;
        }
        return rendezvous;
    }

    /**
     * Where a lost companion (missing, or killed through its immortality) reappears: the rendezvous point,
     * or the owner's spawn point when there is none. Never a place orders send a companion to.
     */
    public static GlobalPos baseOf(MinecraftServer server, UUID owner) {
        GlobalPos rendezvous = rendezvousOf(server, owner);
        return rendezvous != null ? rendezvous : spawnPointOf(server, owner);
    }

    private static GlobalPos spawnPointOf(MinecraftServer server, UUID owner) {
        GlobalPos spawn = CompanionRegistry.get(server).lastSpawnOf(owner);
        return spawn != null ? spawn : GlobalPos.create(World.OVERWORLD, server.getOverworld().getSpawnPos());
    }

    static boolean withinRendezvousRadius(GlobalPos point, GlobalPos spawn) {
        double radius = RPGMinibossesEntities.config.companionRendezvousRadius;
        return point.dimension().equals(spawn.dimension()) && point.pos().getSquaredDistance(spawn.pos()) <= radius * radius;
    }

    // ── Orders ──────────────────────────────────────────────────────────────

    static void follow(ServerPlayerEntity player, MinibossEntity npc, CompanionRecord record) {
        record.mode = CompanionMode.FOLLOWING;
        npc.setSitting(false);
        dirty(player.getServer(), npc, record);
        say(player, npc, "I will follow.");
    }

    static void stay(ServerPlayerEntity player, MinibossEntity npc, CompanionRecord record, long ticks, String length) {
        record.mode = CompanionMode.STAYING;
        record.stayUntil = now(player.getServer()) + Math.max(1, ticks);
        npc.setSitting(true);
        npc.getNavigation().stop();
        dirty(player.getServer(), npc, record);
        say(player, npc, "I will wait here for " + length + ".");
    }

    /** Only with a rendezvous point: there is nowhere else to wait. */
    static void goToRendezvous(ServerPlayerEntity player, MinibossEntity npc, CompanionRecord record) {
        if (rendezvousOf(player.getServer(), player.getUuid()) == null) {
            return;
        }
        record.mode = CompanionMode.TRAVELING_TO_BASE;
        npc.setSitting(true);
        dirty(player.getServer(), npc, record);
        say(player, npc, "I will wait at the rendezvous point.");
    }

    static void setRendezvous(ServerPlayerEntity player, MinibossEntity npc) {
        GlobalPos here = GlobalPos.create(player.getWorld().getRegistryKey(), player.getBlockPos());
        CompanionRegistry.get(player.getServer()).setRendezvous(player.getUuid(), here);
        say(player, npc, "Understood. I will meet you here.");
    }

    /** Frees the slot at once, hands back everything in storage, and the ex-companion walks off for good. */
    static void dismiss(ServerPlayerEntity player, MinibossEntity npc) {
        CompanionRegistry.get(player.getServer()).release(player.getUuid());
        CompanionGear.returnGear(npc, player);
        var storage = npc.getExchangeStock().inventory();
        for (int i = 0; i < storage.size(); i++) {
            var stack = storage.removeStack(i);
            if (!stack.isEmpty()) {
                player.getInventory().offerOrDrop(stack);
            }
        }
        npc.setOwnerUuid(null);
        npc.setSitting(true);
        say(player, npc, "Farewell, then.");
        Vec3d away = npc.getPos().subtract(player.getPos()).multiply(1, 0, 1);
        if (away.lengthSquared() < 1.0E-4) {
            away = new Vec3d(1, 0, 0);
        }
        Vec3d target = npc.getPos().add(away.normalize().multiply(DEPART_DISTANCE));
        npc.startDeparting(BlockPos.ofFloored(target), DEPART_MAX_TICKS);
    }

    private static void dirty(MinecraftServer server, MinibossEntity npc, CompanionRecord record) {
        snapshot(npc, record);
        CompanionRegistry.get(server).markDirty();
    }

    // ── Immortality ─────────────────────────────────────────────────────────

    /**
     * Called instead of dying. Returns true when the companion went down instead, so the caller must not
     * let it die. For damage that ignores immortality, returns false: the entity dies, and the companion is
     * re-created at base by the sweep (its equipment is not dropped, so nothing is duplicated).
     */
    public static boolean onLethalDamage(MinibossEntity npc, DamageSource source) {
        if (!(npc.getWorld() instanceof ServerWorld world) || npc.getOwnerUuid() == null) {
            return false;
        }
        CompanionRegistry registry = CompanionRegistry.get(world.getServer());
        CompanionRecord record = registry.ofNpc(npc.getUuid());
        if (record == null) {
            return false;
        }
        if (source.isIn(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            record.needsRestore = true;
            npc.setHealth(npc.getMaxHealth());
            snapshot(npc, record);
            registry.markDirty();
            return false;
        }
        if (record.mode != CompanionMode.DOWNED) {
            record.modeBeforeDown = record.mode;
        }
        record.mode = CompanionMode.DOWNED;
        record.downUntil = now(world.getServer()) + RPGMinibossesEntities.config.companionDownedTicks;
        npc.setHealth(1.0F);
        npc.setCompanionDowned(true);
        npc.setTarget(null);
        npc.getNavigation().stop();
        registry.markDirty();
        if (npc.getOwner() instanceof ServerPlayerEntity owner) {
            owner.sendMessage(Text.translatable("companion." + RPGMinibosses.CONTENT_NAMESPACE + ".downed", npc.getName()), true);
        }
        return true;
    }

    private static void revive(MinibossEntity npc, CompanionRecord record) {
        npc.setCompanionDowned(false);
        npc.setHealth(Math.max(1.0F, npc.getMaxHealth() * RPGMinibossesEntities.config.companionReviveHealth));
        record.mode = record.modeBeforeDown == CompanionMode.DOWNED ? CompanionMode.FOLLOWING : record.modeBeforeDown;
    }

    // ── The companion's own tick ────────────────────────────────────────────

    /** Called every server tick from the adventurer; does real work once a second. */
    public static void tick(MinibossEntity npc) {
        if (!(npc.getWorld() instanceof ServerWorld world)) {
            return;
        }
        if (npc.isDeparting()) {
            tickDeparting(world, npc);
            return;
        }
        if (npc.getOwnerUuid() == null || npc.age % 20 != 0) {
            return;
        }
        MinecraftServer server = world.getServer();
        CompanionRegistry registry = CompanionRegistry.get(server);
        CompanionRecord record = registry.ofNpc(npc.getUuid());
        if (record == null) {
            // Owned, but not on the books: a companion from before 3.0's registry, or one that was released.
            if (registry.ofOwner(npc.getOwnerUuid()) == null && registry.bind(npc.getOwnerUuid(), npc.getUuid()) != null) {
                record = registry.ofNpc(npc.getUuid());
                record.incarnation = Math.max(1, npc.getCompanionIncarnation());
                npc.setCompanionIncarnation(record.incarnation);
            } else {
                npc.setOwnerUuid(null);
                npc.setSitting(false);
                return;
            }
        }
        if (npc.getCompanionIncarnation() != record.incarnation) {
            // A copy left behind in a chunk saved before the companion moved on.
            npc.discard();
            return;
        }
        record.lastKnown = GlobalPos.create(world.getRegistryKey(), npc.getBlockPos());
        record.missingSince = 0;
        long now = now(server);
        switch (record.mode) {
            case DOWNED -> {
                npc.setCompanionDowned(true);
                if (now >= record.downUntil) {
                    revive(npc, record);
                }
            }
            case STAYING -> {
                npc.setSitting(true);
                if (now >= record.stayUntil) {
                    record.mode = rendezvousOf(server, record.owner) != null ? CompanionMode.TRAVELING_TO_BASE : CompanionMode.FOLLOWING;
                }
            }
            case TRAVELING_TO_BASE -> {
                npc.setSitting(true);
                walkHome(server, world, npc, record);
            }
            case AT_BASE -> {
                npc.setSitting(true);
                GlobalPos base = rendezvousOf(server, record.owner);
                if (base == null) {
                    record.mode = CompanionMode.FOLLOWING;
                } else if (!base.dimension().equals(world.getRegistryKey()) || npc.getBlockPos().getSquaredDistance(base.pos()) > BASE_MOVED * BASE_MOVED) {
                    record.mode = CompanionMode.TRAVELING_TO_BASE;
                }
            }
            case FOLLOWING -> npc.setSitting(false);
        }
        if (npc.age % SNAPSHOT_INTERVAL == 0 && !npc.isRemoved()) {
            snapshot(npc, record);
        }
        registry.markDirty();
    }

    /** Walk to the rendezvous point if it is near and in this dimension; otherwise vanish onto a simulated trip. */
    private static void walkHome(MinecraftServer server, ServerWorld world, MinibossEntity npc, CompanionRecord record) {
        GlobalPos base = rendezvousOf(server, record.owner);
        if (base == null) {
            record.mode = CompanionMode.FOLLOWING;
            return;
        }
        double range = RPGMinibossesEntities.config.companionWalkRange;
        Vec3d target = Vec3d.ofBottomCenter(base.pos());
        boolean sameWorld = base.dimension().equals(world.getRegistryKey());
        if (!sameWorld || npc.squaredDistanceTo(target) > range * range) {
            beginTrip(server, npc, record, base);
            npc.discard();
            return;
        }
        if (npc.squaredDistanceTo(target) <= ARRIVED * ARRIVED) {
            record.mode = CompanionMode.AT_BASE;
            npc.getNavigation().stop();
            return;
        }
        npc.getNavigation().startMovingTo(target.x, target.y, target.z, 1.0);
    }

    /**
     * Records a simulated trip home and bumps the incarnation, so any copy of this entity still saved in
     * a chunk removes itself if it ever loads.
     */
    private static void beginTrip(MinecraftServer server, MinibossEntity npc, CompanionRecord record, GlobalPos base) {
        snapshot(npc, record);
        GlobalPos from = GlobalPos.create(npc.getWorld().getRegistryKey(), npc.getBlockPos());
        record.tripDestination = base;
        record.tripArrivesAt = now(server) + travelTicks(from, base);
        record.incarnation++;
        record.mode = CompanionMode.TRAVELING_TO_BASE;
        CompanionRegistry.get(server).markDirty();
    }

    /**
     * Distance over the configured speed. Between dimensions the distance is measured in overworld terms
     * (the Nether counts eight to one) and the trip is capped.
     */
    static long travelTicks(GlobalPos from, GlobalPos to) {
        ServerConfig config = RPGMinibossesEntities.config;
        double speed = Math.max(0.01, config.companionTravelBlocksPerTick);
        Vec3d a = overworldEquivalent(from);
        Vec3d b = overworldEquivalent(to);
        long ticks = (long) Math.ceil(a.distanceTo(b) / speed);
        if (!from.dimension().equals(to.dimension())) {
            ticks = Math.min(ticks, config.companionCrossDimensionMaxTicks);
        }
        return Math.max(20, ticks);
    }

    private static Vec3d overworldEquivalent(GlobalPos pos) {
        double scale = pos.dimension().equals(World.NETHER) ? 8.0 : 1.0;
        return new Vec3d(pos.pos().getX() * scale, pos.pos().getY(), pos.pos().getZ() * scale);
    }

    private static void tickDeparting(ServerWorld world, MinibossEntity npc) {
        if (npc.age % 20 != 0) {
            return;
        }
        boolean watched = world.getPlayers().stream().anyMatch(player -> player.squaredDistanceTo(npc) < DEPART_WATCH * DEPART_WATCH);
        BlockPos target = npc.getDepartTarget();
        if (!watched || npc.departTimedOut() || target == null) {
            npc.discard();
            return;
        }
        npc.getNavigation().startMovingTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5, 1.0);
    }

    // ── Sweep: trips, timers and lost companions while unloaded ────────────

    private static void sweep(MinecraftServer server) {
        CompanionRegistry registry = CompanionRegistry.get(server);
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            // A new spawn point carries the rendezvous point with it: a companion waiting there moves to it.
            GlobalPos spawn = spawnOf(player);
            if (registry.setLastSpawn(player.getUuid(), spawn) && registry.rendezvousOf(player.getUuid()) != null) {
                registry.setRendezvous(player.getUuid(), spawn);
            }
        }
        long now = now(server);
        for (CompanionRecord record : new ArrayList<>(registry.all())) {
            if (record.tripDestination != null) {
                if (now >= record.tripArrivesAt) {
                    restore(server, record, record.tripDestination);
                }
                continue;
            }
            if (findAnywhere(server, record.npc) != null) {
                continue;
            }
            if (record.needsRestore) {
                restore(server, record, baseOf(server, record.owner));
                continue;
            }
            GlobalPos last = record.lastKnown;
            ServerWorld lastWorld = last == null ? null : server.getWorld(last.dimension());
            if (lastWorld != null && lastWorld.isChunkLoaded(last.pos())) {
                // Its chunk is loaded but it is not there: lost (chunk regenerated, removed by another mod...).
                if (record.missingSince == 0) {
                    record.missingSince = now;
                } else if (now - record.missingSince >= LOST_AFTER) {
                    restore(server, record, baseOf(server, record.owner));
                }
                continue;
            }
            record.missingSince = 0;
            // Unloaded: carry on with whatever its orders say. This is also how a companion whose chunk unloads
            // partway through walking home switches to a simulated trip, from where it was last seen.
            GlobalPos base = rendezvousOf(server, record.owner);
            boolean stayOver = record.mode == CompanionMode.STAYING && now >= record.stayUntil;
            if (base == null) {
                // Nowhere to wait: an ended Wait, or a trip whose rendezvous is gone, means back to following.
                if (stayOver || record.mode == CompanionMode.TRAVELING_TO_BASE || record.mode == CompanionMode.AT_BASE) {
                    record.mode = CompanionMode.FOLLOWING;
                    registry.markDirty();
                }
                continue;
            }
            boolean baseMoved = record.mode == CompanionMode.AT_BASE && last != null
                    && (!last.dimension().equals(base.dimension()) || last.pos().getSquaredDistance(base.pos()) > BASE_MOVED * BASE_MOVED);
            if (stayOver || record.mode == CompanionMode.TRAVELING_TO_BASE || baseMoved) {
                record.tripDestination = base;
                record.tripArrivesAt = now + (last == null ? 20 : travelTicks(last, base));
                record.incarnation++;
                record.mode = CompanionMode.TRAVELING_TO_BASE;
                registry.markDirty();
            }
        }
    }

    /**
     * Re-creates the companion from its snapshot at {@code at} and puts it AT_BASE. Waits (does nothing)
     * until that chunk is loaded. Never makes a second copy: if a live entity with the companion's UUID
     * exists anywhere, the trip or restore is simply cleared.
     */
    private static void restore(MinecraftServer server, CompanionRecord record, GlobalPos at) {
        CompanionRegistry registry = CompanionRegistry.get(server);
        Entity existing = findAnywhere(server, record.npc);
        if (existing instanceof MinibossEntity live && live.getCompanionIncarnation() == record.incarnation) {
            record.tripDestination = null;
            record.needsRestore = false;
            registry.markDirty();
            return;
        }
        ServerWorld world = server.getWorld(at.dimension());
        if (world == null || !world.isChunkLoaded(at.pos())) {
            return;
        }
        if (existing != null) {
            existing.discard();
        }
        NbtCompound snapshot = record.snapshot.copy();
        if (snapshot.isEmpty()) {
            // Nothing to rebuild from (should not happen): free the slot rather than hold a ghost.
            registry.release(record.owner);
            return;
        }
        Entity entity = EntityType.getEntityFromNbt(snapshot, world).orElse(null);
        if (!(entity instanceof MinibossEntity npc)) {
            registry.release(record.owner);
            return;
        }
        Vec3d pos = Vec3d.ofBottomCenter(standableNear(world, at.pos()));
        npc.refreshPositionAndAngles(pos.x, pos.y, pos.z, npc.getYaw(), 0.0F);
        npc.setOwnerUuid(record.owner);
        npc.setCompanionDowned(false);
        if (record.needsRestore) {
            npc.setHealth(npc.getMaxHealth());
        }
        record.incarnation++;
        npc.setCompanionIncarnation(record.incarnation);
        npc.setSitting(true);
        if (!world.spawnEntity(npc)) {
            return;
        }
        record.mode = CompanionMode.AT_BASE;
        record.tripDestination = null;
        record.needsRestore = false;
        record.missingSince = 0;
        record.lastKnown = at;
        snapshot(npc, record);
        registry.markDirty();
    }

    /**
     * The nearest spot within a few blocks of {@code target} with solid ground and two blocks of air, so a
     * companion arriving at a bed or a cramped spawn point stands beside it. Falls back to the target.
     */
    private static BlockPos standableNear(ServerWorld world, BlockPos target) {
        BlockPos best = null;
        double bestSq = Double.MAX_VALUE;
        for (BlockPos pos : BlockPos.iterateOutwards(target, 4, 3, 4)) {
            if (world.getBlockState(pos.down()).isSolidBlock(world, pos.down())
                    && world.getBlockState(pos).getCollisionShape(world, pos).isEmpty()
                    && world.getBlockState(pos.up()).getCollisionShape(world, pos.up()).isEmpty()
                    && world.getFluidState(pos).isEmpty()) {
                double d = pos.getSquaredDistance(target);
                if (d < bestSq) {
                    bestSq = d;
                    best = pos.toImmutable();
                }
            }
        }
        return best != null ? best : target;
    }

    static @Nullable Entity findAnywhere(MinecraftServer server, UUID uuid) {
        for (ServerWorld world : server.getWorlds()) {
            Entity entity = world.getEntity(uuid);
            if (entity != null && !entity.isRemoved()) {
                return entity;
            }
        }
        return null;
    }

    static void snapshot(MinibossEntity npc, CompanionRecord record) {
        NbtCompound nbt = new NbtCompound();
        if (npc.saveSelfNbt(nbt)) {
            record.snapshot = nbt;
        }
    }

    /** For orders: the record behind this player's companion, if {@code npc} is it. */
    static @Nullable CompanionRecord recordFor(ServerPlayerEntity player, MinibossEntity npc) {
        CompanionRecord record = CompanionRegistry.get(player.getServer()).ofNpc(npc.getUuid());
        return record != null && record.owner.equals(player.getUuid()) ? record : null;
    }

    static boolean isOwner(PlayerEntity player, MinibossEntity npc) {
        return player.getUuid().equals(npc.getOwnerUuid());
    }
}

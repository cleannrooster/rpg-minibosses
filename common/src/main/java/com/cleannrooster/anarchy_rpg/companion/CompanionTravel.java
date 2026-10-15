package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * A following companion goes where its owner goes: through teleports and between dimensions.
 *
 * <p>Detection is deliberately mod-agnostic. Waystones, {@code /tp}, {@code /home}-style commands, portals,
 * respawning and anything else all end in the player being somewhere else, so the owner's position is
 * sampled every tick and a jump of more than {@code companionTeleportJumpDistance} in one tick, or a change of
 * dimension, means a teleport. A short moment later (so the destination has loaded) the companion joins them:
 * <ul>
 *   <li>in the same dimension and still loaded, it is moved beside the owner;</li>
 *   <li>otherwise it is saved, removed, and re-created beside the owner from that save, as a new incarnation,
 *   so any copy left behind removes itself if it ever loads.</li>
 * </ul>
 * A mod that already brings pets along (Waystones can) just leaves nothing to do: a companion already beside
 * its owner is left alone. Only a FOLLOWING companion comes along; one waiting, patrolling, at base or down
 * stays where it is. A Rogue leading the way stops leading and comes along.
 *
 * <p>One exception: when the owner dies and respawns, a downed companion gets up at full health and joins them
 * at the respawn point, wherever it went down.</p>
 */
final class CompanionTravel {
    /** Ticks after the jump before joining, so the owner's destination chunks are in. */
    private static final int JOIN_DELAY = 5;
    /** Gives up after this long if the destination never loads (the failsafes take over from there). */
    private static final int GIVE_UP = 200;
    /** Already this close: nothing to do. */
    private static final double JOINED = 8.0;

    private record Sample(RegistryKey<World> dimension, Vec3d pos) {
    }

    private static final Map<UUID, Sample> LAST = new HashMap<>();
    /** Owner to the server tick their teleport was noticed. */
    private static final Map<UUID, Integer> PENDING = new HashMap<>();

    private CompanionTravel() {
    }

    static void register() {
        TickEvent.SERVER_POST.register(CompanionTravel::tick);
        PlayerEvent.PLAYER_RESPAWN.register((player, conqueredEnd, removalReason) -> {
            if (!conqueredEnd) {
                onRespawn(player);
            }
        });
        PlayerEvent.PLAYER_QUIT.register(player -> {
            LAST.remove(player.getUuid());
            PENDING.remove(player.getUuid());
        });
        LifecycleEvent.SERVER_STOPPED.register(server -> {
            LAST.clear();
            PENDING.clear();
        });
    }

    private static void tick(MinecraftServer server) {
        // With teleport-following off, only a respawn rejoin (a downed companion getting up) still runs.
        boolean follow = RPGMinibossesEntities.config.companionFollowTeleports;
        CompanionRegistry registry = CompanionRegistry.get(server);
        double jump = RPGMinibossesEntities.config.companionTeleportJumpDistance;
        int now = server.getTicks();
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            CompanionRecord record = registry.ofOwner(player.getUuid());
            if (record == null) {
                LAST.remove(player.getUuid());
                PENDING.remove(player.getUuid());
                continue;
            }
            Sample here = new Sample(player.getWorld().getRegistryKey(), player.getPos());
            Sample last = LAST.put(player.getUuid(), here);
            if (follow && last != null && (!last.dimension().equals(here.dimension()) || last.pos().squaredDistanceTo(here.pos()) > jump * jump)) {
                PENDING.put(player.getUuid(), now);
            }
            Integer since = PENDING.get(player.getUuid());
            if (since == null || now - since < JOIN_DELAY) {
                continue;
            }
            if (record.mode == CompanionMode.GUIDING) {
                // The owner went somewhere else entirely: the lead is over, and it comes along instead.
                record.mode = CompanionMode.FOLLOWING;
                GuideSpecialty.clear(record);
            }
            if (record.mode != CompanionMode.FOLLOWING || record.onTrip() || player.isSpectator() || !player.isAlive()
                    || joinOwner(server, record, player) || now - since > GIVE_UP) {
                PENDING.remove(player.getUuid());
            }
        }
    }

    /** The owner died: a downed companion gets up, healed, and is brought to where they respawned. */
    private static void onRespawn(ServerPlayerEntity player) {
        MinecraftServer server = player.getServer();
        CompanionRegistry registry = CompanionRegistry.get(server);
        CompanionRecord record = registry.ofOwner(player.getUuid());
        if (record == null || record.mode != CompanionMode.DOWNED || record.onTrip()) {
            return;
        }
        record.mode = CompanionMode.FOLLOWING;
        record.healOnJoin = true;
        // Loaded: up at once, so it is never left flagged as down if the join below gives up. An unloaded one is
        // healed as it is re-created beside the owner.
        if (Companions.findAnywhere(server, record.npc) instanceof MinibossEntity live
                && live.getCompanionIncarnation() == record.incarnation) {
            getUp(live, record);
        }
        registry.markDirty();
        // Joined even when the respawn point is close by: it should be on its feet beside them, not left where it fell.
        PENDING.put(player.getUuid(), server.getTicks());
    }

    /** On its feet at full health, if its owner respawned while it was down. The flag is cleared once it has joined. */
    private static void getUp(MinibossEntity npc, CompanionRecord record) {
        if (record.healOnJoin) {
            npc.setCompanionDowned(false);
            npc.setHealth(npc.getMaxHealth());
        }
    }

    /** Brings the companion beside its owner. False if the destination is not loaded yet (try again). */
    private static boolean joinOwner(MinecraftServer server, CompanionRecord record, ServerPlayerEntity owner) {
        ServerWorld to = owner.getServerWorld();
        BlockPos ownerPos = owner.getBlockPos();
        if (!to.isChunkLoaded(ownerPos)) {
            return false;
        }
        Entity found = Companions.findAnywhere(server, record.npc);
        MinibossEntity live = found instanceof MinibossEntity npc && npc.getCompanionIncarnation() == record.incarnation ? npc : null;
        BlockPos at = Companions.standableNear(to, ownerPos);

        if (live != null && GuideBoat.isBoating(live)) {
            GuideBoat.disembark(live);
        }
        if (live != null && live.getWorld() == to) {
            live.setSitting(false);
            getUp(live, record);
            record.healOnJoin = false;
            if (live.squaredDistanceTo(owner) > JOINED * JOINED) {
                live.getNavigation().stop();
                live.requestTeleport(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
            }
            return true;
        }
        // Another dimension, or no longer loaded: re-create it beside the owner from a fresh save.
        if (live != null) {
            Companions.snapshot(live, record);
            live.discard();
        } else if (found != null) {
            found.discard(); // A stale copy: never two.
        }
        NbtCompound snapshot = record.snapshot.copy();
        if (snapshot.isEmpty() || !(EntityType.getEntityFromNbt(snapshot, to).orElse(null) instanceof MinibossEntity npc)) {
            return true; // Nothing to rebuild from; the sweep's failsafes handle it.
        }
        npc.refreshPositionAndAngles(at.getX() + 0.5, at.getY(), at.getZ() + 0.5, owner.getYaw(), 0.0F);
        npc.setOwnerUuid(record.owner);
        npc.setCompanionDowned(false);
        npc.setSitting(false);
        record.incarnation++;
        npc.setCompanionIncarnation(record.incarnation);
        getUp(npc, record);
        if (!to.spawnEntity(npc)) {
            return false;
        }
        record.healOnJoin = false;
        record.lastKnown = GlobalPos.create(to.getRegistryKey(), at);
        record.lastSeen = Companions.now(server);
        record.separatedSince = 0;
        record.missingSince = 0;
        Companions.snapshot(npc, record);
        CompanionRegistry.get(server).markDirty();
        return true;
    }
}

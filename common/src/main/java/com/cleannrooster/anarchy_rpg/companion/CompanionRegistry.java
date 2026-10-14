package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every player's companion, plus each player's rendezvous point and last known spawn point. Kept in the
 * overworld's saved data. One companion per player: {@link #bind} refuses rather than replacing.
 *
 * <p>The rendezvous point is per player, not per companion, so it carries over to the next companion.
 */
public final class CompanionRegistry extends PersistentState {
    private static final String ID = RPGMinibosses.CONTENT_NAMESPACE + "_companions";

    private final Map<UUID, CompanionRecord> byOwner = new HashMap<>();
    private final Map<UUID, UUID> ownerByNpc = new HashMap<>();
    private final Map<UUID, GlobalPos> rendezvous = new HashMap<>();
    /** The owner's spawn point as last seen online, so the base can be found while they are offline. */
    private final Map<UUID, GlobalPos> lastSpawn = new HashMap<>();

    public static CompanionRegistry get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(CompanionRegistry::new, CompanionRegistry::fromNbt, null), ID);
    }

    public @Nullable CompanionRecord ofOwner(UUID owner) {
        return this.byOwner.get(owner);
    }

    public @Nullable CompanionRecord ofNpc(UUID npc) {
        UUID owner = this.ownerByNpc.get(npc);
        return owner == null ? null : this.byOwner.get(owner);
    }

    public Collection<CompanionRecord> all() {
        return this.byOwner.values();
    }

    /** Records a new companion. Returns null when the owner already has one. */
    @Nullable
    CompanionRecord bind(UUID owner, UUID npc) {
        if (this.byOwner.containsKey(owner)) {
            return null;
        }
        CompanionRecord record = new CompanionRecord(owner, npc);
        this.byOwner.put(owner, record);
        this.ownerByNpc.put(npc, owner);
        this.markDirty();
        return record;
    }

    void release(UUID owner) {
        CompanionRecord record = this.byOwner.remove(owner);
        if (record != null) {
            this.ownerByNpc.remove(record.npc);
            this.markDirty();
        }
    }

    public @Nullable GlobalPos rendezvousOf(UUID owner) {
        return this.rendezvous.get(owner);
    }

    void setRendezvous(UUID owner, @Nullable GlobalPos pos) {
        if (pos == null) {
            this.rendezvous.remove(owner);
        } else {
            this.rendezvous.put(owner, pos);
        }
        this.markDirty();
    }

    public @Nullable GlobalPos lastSpawnOf(UUID owner) {
        return this.lastSpawn.get(owner);
    }

    /** Returns true when the spawn point changed from one already known (not the first sighting). */
    boolean setLastSpawn(UUID owner, GlobalPos pos) {
        GlobalPos previous = this.lastSpawn.get(owner);
        if (pos.equals(previous)) {
            return false;
        }
        this.lastSpawn.put(owner, pos);
        this.markDirty();
        return previous != null;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList companions = new NbtList();
        this.byOwner.values().forEach(record -> companions.add(record.toNbt()));
        nbt.put("Companions", companions);
        NbtList points = new NbtList();
        this.rendezvous.forEach((owner, pos) -> points.add(ownerPos(owner, pos)));
        nbt.put("Rendezvous", points);
        NbtList spawns = new NbtList();
        this.lastSpawn.forEach((owner, pos) -> spawns.add(ownerPos(owner, pos)));
        nbt.put("LastSpawn", spawns);
        return nbt;
    }

    private static NbtCompound ownerPos(UUID owner, GlobalPos pos) {
        NbtCompound entry = new NbtCompound();
        entry.putUuid("Owner", owner);
        CompanionRecord.putPos(entry, "Pos", pos);
        return entry;
    }

    private static CompanionRegistry fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        CompanionRegistry registry = new CompanionRegistry();
        for (NbtElement element : nbt.getList("Companions", NbtElement.COMPOUND_TYPE)) {
            CompanionRecord record = CompanionRecord.fromNbt((NbtCompound) element);
            if (record != null) {
                registry.byOwner.put(record.owner, record);
                registry.ownerByNpc.put(record.npc, record.owner);
            }
        }
        readOwnerPositions(nbt.getList("Rendezvous", NbtElement.COMPOUND_TYPE), registry.rendezvous);
        readOwnerPositions(nbt.getList("LastSpawn", NbtElement.COMPOUND_TYPE), registry.lastSpawn);
        return registry;
    }

    private static void readOwnerPositions(NbtList list, Map<UUID, GlobalPos> into) {
        for (NbtElement element : list) {
            NbtCompound entry = (NbtCompound) element;
            GlobalPos pos = CompanionRecord.getPos(entry, "Pos");
            if (entry.containsUuid("Owner") && pos != null) {
                into.put(entry.getUuid("Owner"), pos);
            }
        }
    }
}

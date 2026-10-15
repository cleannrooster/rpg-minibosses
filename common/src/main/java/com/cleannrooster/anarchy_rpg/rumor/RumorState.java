package com.cleannrooster.anarchy_rpg.rumor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.faction.FactionSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rumor bookkeeping, per player and shared by every adventurer, kept in the overworld's saved data:
 * <ul>
 *   <li>who can be the subject of a rumor: players who have opened a conversation with any adventurer;</li>
 *   <li>which subjects each player has been told truthfully (false rumors are not remembered, so the truth
 *   can still be learned);</li>
 *   <li>gossip waiting for each player: who asked about them, delivered once by whichever adventurer they
 *   talk to next.</li>
 * </ul>
 */
public final class RumorState extends PersistentState {
    private static final String ID = RPGMinibosses.CONTENT_NAMESPACE + "_rumors";
    /** Older gossip is dropped beyond this many waiting entries per player. */
    private static final int MAX_GOSSIP = 8;

    /**
     * Someone asked an adventurer about a player. {@code faction} is null when it was a companion;
     * {@code fromTarget} (where the asking happened, seen from the player) is null across dimensions.
     */
    public record AskedAbout(String askerName, @Nullable FactionSlot faction, @Nullable Bearing fromTarget, long askedAt) {
    }

    private final Set<UUID> engaged = new HashSet<>();
    private final Map<UUID, Set<String>> told = new HashMap<>();
    private final Map<UUID, Deque<AskedAbout>> gossip = new HashMap<>();

    public static RumorState get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(RumorState::new, RumorState::fromNbt, null), ID);
    }

    public boolean isRumorSubject(UUID player) {
        return this.engaged.contains(player);
    }

    /** Permanent: set the first time the player opens a conversation. */
    public void markEngaged(UUID player) {
        if (this.engaged.add(player)) {
            this.markDirty();
        }
    }

    public boolean alreadyTold(UUID player, String subjectKey) {
        Set<String> keys = this.told.get(player);
        return keys != null && keys.contains(subjectKey);
    }

    public void markTold(UUID player, String subjectKey) {
        if (this.told.computeIfAbsent(player, uuid -> new HashSet<>()).add(subjectKey)) {
            this.markDirty();
        }
    }

    /** Forgets everything this player has been told (debug). */
    public void forget(UUID player) {
        if (this.told.remove(player) != null) {
            this.markDirty();
        }
    }

    public void recordAskedAbout(UUID target, AskedAbout entry) {
        Deque<AskedAbout> queue = this.gossip.computeIfAbsent(target, uuid -> new ArrayDeque<>());
        queue.addLast(entry);
        while (queue.size() > MAX_GOSSIP) {
            queue.removeFirst();
        }
        this.markDirty();
    }

    /** The oldest gossip waiting for this player, removed as it is handed over. */
    public @Nullable AskedAbout takeGossip(UUID target) {
        Deque<AskedAbout> queue = this.gossip.get(target);
        if (queue == null || queue.isEmpty()) {
            return null;
        }
        AskedAbout entry = queue.removeFirst();
        if (queue.isEmpty()) {
            this.gossip.remove(target);
        }
        this.markDirty();
        return entry;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtList engagedList = new NbtList();
        for (UUID uuid : this.engaged) {
            engagedList.add(NbtString.of(uuid.toString()));
        }
        nbt.put("Engaged", engagedList);

        NbtCompound toldNbt = new NbtCompound();
        this.told.forEach((player, keys) -> {
            NbtList list = new NbtList();
            keys.forEach(key -> list.add(NbtString.of(key)));
            toldNbt.put(player.toString(), list);
        });
        nbt.put("Told", toldNbt);

        NbtCompound gossipNbt = new NbtCompound();
        this.gossip.forEach((target, queue) -> {
            NbtList list = new NbtList();
            for (AskedAbout entry : queue) {
                NbtCompound e = new NbtCompound();
                e.putString("Asker", entry.askerName());
                if (entry.faction() != null) {
                    e.putString("Faction", entry.faction().name());
                }
                if (entry.fromTarget() != null) {
                    e.putInt("Bearing", entry.fromTarget().ordinal());
                }
                e.putLong("At", entry.askedAt());
                list.add(e);
            }
            gossipNbt.put(target.toString(), list);
        });
        nbt.put("Gossip", gossipNbt);
        return nbt;
    }

    private static RumorState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        RumorState state = new RumorState();
        for (NbtElement element : nbt.getList("Engaged", NbtElement.STRING_TYPE)) {
            state.engaged.add(UUID.fromString(element.asString()));
        }
        NbtCompound toldNbt = nbt.getCompound("Told");
        for (String player : toldNbt.getKeys()) {
            Set<String> keys = new HashSet<>();
            for (NbtElement element : toldNbt.getList(player, NbtElement.STRING_TYPE)) {
                keys.add(element.asString());
            }
            state.told.put(UUID.fromString(player), keys);
        }
        NbtCompound gossipNbt = nbt.getCompound("Gossip");
        for (String target : gossipNbt.getKeys()) {
            Deque<AskedAbout> queue = new ArrayDeque<>();
            for (NbtElement element : gossipNbt.getList(target, NbtElement.COMPOUND_TYPE)) {
                NbtCompound e = (NbtCompound) element;
                FactionSlot faction = null;
                if (e.contains("Faction")) {
                    try {
                        faction = FactionSlot.valueOf(e.getString("Faction"));
                    } catch (IllegalArgumentException ignored) {
                        // A guild slot that no longer exists: the gossip just names no guild.
                    }
                }
                queue.addLast(new AskedAbout(e.getString("Asker"), faction, e.contains("Bearing") ? Bearing.fromIndex(e.getInt("Bearing")) : null, e.getLong("At")));
            }
            state.gossip.put(UUID.fromString(target), queue);
        }
        return state;
    }
}

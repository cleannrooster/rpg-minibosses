package com.cleannrooster.anarchy_rpg.journal;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.faction.FactionSlot;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.text.Text;
import net.minecraft.world.PersistentState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Every player's Guild Journal entries: the rumors and gossip they have been told, newest last, kept in
 * the overworld's saved data. Rumors are written down exactly as they were said, so a false one reads
 * the same as a true one and the journal gives nothing away.
 */
public final class JournalLog extends PersistentState {
    private static final String ID = RPGMinibosses.CONTENT_NAMESPACE + "_journal";
    /** Older entries are dropped beyond this many per player. */
    public static final int MAX_ENTRIES = 30;

    public enum Kind {
        RUMOR,
        GOSSIP;

        static Kind fromIndex(int index) {
            Kind[] values = values();
            return values[Math.floorMod(index, values.length)];
        }
    }

    /**
     * One thing a player was told. {@code faction} is the teller's guild, null for a companion; {@code day}
     * is the in-game day it was told.
     */
    public record Entry(Kind kind, Text teller, @Nullable FactionSlot faction, Text line, long day) {
    }

    private final Map<UUID, Deque<Entry>> entries = new HashMap<>();

    public static JournalLog get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(JournalLog::new, JournalLog::fromNbt, null), ID);
    }

    public void record(UUID player, Entry entry) {
        Deque<Entry> queue = this.entries.computeIfAbsent(player, uuid -> new ArrayDeque<>());
        queue.addLast(entry);
        while (queue.size() > MAX_ENTRIES) {
            queue.removeFirst();
        }
        this.markDirty();
    }

    /** This player's entries, oldest first. */
    public List<Entry> of(UUID player) {
        Deque<Entry> queue = this.entries.get(player);
        return queue == null ? List.of() : List.copyOf(queue);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound players = new NbtCompound();
        this.entries.forEach((player, queue) -> {
            NbtList list = new NbtList();
            for (Entry entry : queue) {
                NbtCompound e = new NbtCompound();
                e.putInt("Kind", entry.kind().ordinal());
                e.putString("Teller", Text.Serialization.toJsonString(entry.teller(), registries));
                if (entry.faction() != null) {
                    e.putString("Faction", entry.faction().name());
                }
                e.putString("Line", Text.Serialization.toJsonString(entry.line(), registries));
                e.putLong("Day", entry.day());
                list.add(e);
            }
            players.put(player.toString(), list);
        });
        nbt.put("Players", players);
        return nbt;
    }

    private static JournalLog fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        JournalLog log = new JournalLog();
        NbtCompound players = nbt.getCompound("Players");
        for (String key : players.getKeys()) {
            Deque<Entry> queue = new ArrayDeque<>();
            for (NbtElement element : players.getList(key, NbtElement.COMPOUND_TYPE)) {
                NbtCompound e = (NbtCompound) element;
                FactionSlot faction = null;
                if (e.contains("Faction")) {
                    try {
                        faction = FactionSlot.valueOf(e.getString("Faction"));
                    } catch (IllegalArgumentException ignored) {
                        // A guild slot that no longer exists: the entry just names no guild.
                    }
                }
                Text teller = textOf(e.getString("Teller"), registries);
                Text line = textOf(e.getString("Line"), registries);
                queue.addLast(new Entry(Kind.fromIndex(e.getInt("Kind")), teller, faction, line, e.getLong("Day")));
            }
            try {
                log.entries.put(UUID.fromString(key), queue);
            } catch (IllegalArgumentException ignored) {
                // A malformed key loses only that player's journal.
            }
        }
        return log;
    }

    private static Text textOf(String json, RegistryWrapper.WrapperLookup registries) {
        try {
            Text text = Text.Serialization.fromJson(json, registries);
            return text != null ? text : Text.empty();
        } catch (RuntimeException e) {
            return Text.empty();
        }
    }
}

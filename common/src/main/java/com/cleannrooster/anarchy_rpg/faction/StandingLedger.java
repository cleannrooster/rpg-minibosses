package com.cleannrooster.anarchy_rpg.faction;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Every player's standing with each of the three factions, kept in the overworld's saved data. It is
 * keyed by player UUID rather than attached to the player, so it works for offline players and can be
 * read for anyone (rumors about other players will need that).
 *
 * <p>{@link #apply} is the only way standing changes. A change with one faction also reaches the other
 * two, scaled by their relation to it ({@link FactionRelation#propagation}).
 */
public final class StandingLedger extends PersistentState {
    private static final String ID = RPGMinibosses.CONTENT_NAMESPACE + "_standing";

    private final Map<UUID, int[]> standings = new HashMap<>();

    public static StandingLedger get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(StandingLedger::new, StandingLedger::fromNbt, null), ID);
    }

    public int get(UUID player, FactionSlot faction) {
        int[] values = this.standings.get(player);
        return values == null ? 0 : values[faction.index()];
    }

    /** Applies {@code event} to its faction, spreads it to the other two by relation, and clamps all three. */
    public void apply(UUID player, StandingEvent event, FactionRoster roster) {
        if (event.delta() == 0) {
            return;
        }
        int[] values = this.standings.computeIfAbsent(player, uuid -> new int[FactionSlot.values().length]);
        for (FactionSlot slot : FactionSlot.values()) {
            int delta = slot == event.faction()
                    ? event.delta()
                    : (int) (event.delta() * roster.between(event.faction(), slot).propagation());
            values[slot.index()] = clamp(values[slot.index()] + delta);
        }
        this.markDirty();
    }

    /** Sets one faction's standing outright, without spreading it. For operator commands. */
    public void set(UUID player, FactionSlot faction, int value) {
        this.standings.computeIfAbsent(player, uuid -> new int[FactionSlot.values().length])[faction.index()] = clamp(value);
        this.markDirty();
    }

    private static int clamp(int value) {
        var config = RPGMinibossesEntities.config;
        return MathHelper.clamp(value, config.standingMin, config.standingMax);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound players = new NbtCompound();
        this.standings.forEach((uuid, values) -> players.putIntArray(uuid.toString(), values));
        nbt.put("Players", players);
        return nbt;
    }

    private static StandingLedger fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        StandingLedger ledger = new StandingLedger();
        NbtCompound players = nbt.getCompound("Players");
        for (String key : players.getKeys()) {
            int[] stored = players.getIntArray(key);
            int[] values = new int[FactionSlot.values().length];
            System.arraycopy(stored, 0, values, 0, Math.min(stored.length, values.length));
            try {
                ledger.standings.put(UUID.fromString(key), values);
            } catch (IllegalArgumentException ignored) {
                // A malformed key loses only that one entry.
            }
        }
        return ledger;
    }
}

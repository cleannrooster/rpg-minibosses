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
 * <p>{@link #apply} is the only way deeds change standing. A change with one faction also reaches the other
 * two, scaled by their relation to it ({@link FactionRelation#propagation}).
 *
 * <p>Grudges cool: negative standing climbs back toward 0 by {@code standingRecoveryPerDay} for every 24000
 * ticks of world time, for every player, online or not ({@link #recover}). Recovery is not a deed, so it is
 * set per guild and never spread to the others. Positive standing never decays.
 */
public final class StandingLedger extends PersistentState {
    private static final String ID = RPGMinibosses.CONTENT_NAMESPACE + "_standing";

    private static final double TICKS_PER_DAY = 24000.0;

    private final Map<UUID, int[]> standings = new HashMap<>();
    /** World time recovery has been settled up to; -1 until the first {@link #recover}. */
    private long recoveredTo = -1;

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

    /**
     * Brings every negative standing up toward 0 by the recovery owed since the last call, in whole points.
     * The remainder carries over, so calling it often loses nothing.
     */
    public void recover(long worldTime) {
        int perDay = RPGMinibossesEntities.config.standingRecoveryPerDay;
        if (this.recoveredTo < 0 || worldTime < this.recoveredTo || perDay <= 0) {
            // First run, time went backwards (a world edit), or recovery is off: start counting from now.
            this.recoveredTo = worldTime;
            this.markDirty();
            return;
        }
        double ticksPerPoint = TICKS_PER_DAY / perDay;
        int points = (int) ((worldTime - this.recoveredTo) / ticksPerPoint);
        if (points <= 0) {
            return;
        }
        this.recoveredTo += (long) (points * ticksPerPoint);
        for (int[] values : this.standings.values()) {
            for (int i = 0; i < values.length; i++) {
                if (values[i] < 0) {
                    values[i] = Math.min(0, values[i] + points);
                }
            }
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
        nbt.putLong("RecoveredTo", this.recoveredTo);
        return nbt;
    }

    private static StandingLedger fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        StandingLedger ledger = new StandingLedger();
        ledger.recoveredTo = nbt.contains("RecoveredTo") ? nbt.getLong("RecoveredTo") : -1;
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

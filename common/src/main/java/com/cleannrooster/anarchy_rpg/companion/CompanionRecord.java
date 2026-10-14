package com.cleannrooster.anarchy_rpg.companion;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Everything the server keeps about one player's companion, so the companion can be found, timed and
 * brought back even when its chunk is not loaded.
 *
 * <p>{@link #incarnation} counts how many times the companion has been re-created (arriving from a
 * simulated trip, being restored). The live entity carries the same number; a copy that loads with an older
 * number is stale (left behind in a chunk saved before the companion moved on) and removes itself.
 */
public final class CompanionRecord {
    final UUID owner;
    final UUID npc;
    CompanionMode mode = CompanionMode.FOLLOWING;
    /** The mode to go back to after being downed. */
    CompanionMode modeBeforeDown = CompanionMode.FOLLOWING;
    /** World time a Stay order ends. Kept through being downed, so the Stay resumes with what was left. */
    long stayUntil;
    /** World time the companion gets back up after being downed. */
    long downUntil;
    @Nullable
    GlobalPos lastKnown;
    /** Set while the companion is on a simulated trip home: where to, and when it gets there. */
    @Nullable
    GlobalPos tripDestination;
    long tripArrivesAt;
    /** The companion as last saved, used to re-create it. */
    NbtCompound snapshot = new NbtCompound();
    long incarnation;
    /** Killed by something that ignores immortality (void, /kill); comes back at base. */
    boolean needsRestore;
    /** Server time the companion was first noticed missing from its loaded chunk; 0 when not missing. */
    long missingSince;

    CompanionRecord(UUID owner, UUID npc) {
        this.owner = owner;
        this.npc = npc;
    }

    public UUID owner() {
        return this.owner;
    }

    public UUID npc() {
        return this.npc;
    }

    public CompanionMode mode() {
        return this.mode;
    }

    public long stayUntil() {
        return this.stayUntil;
    }

    public long downUntil() {
        return this.downUntil;
    }

    public boolean onTrip() {
        return this.tripDestination != null;
    }

    public long tripArrivesAt() {
        return this.tripArrivesAt;
    }

    public @Nullable GlobalPos lastKnown() {
        return this.lastKnown;
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("Owner", this.owner);
        nbt.putUuid("Npc", this.npc);
        nbt.putString("Mode", this.mode.name());
        nbt.putString("ModeBeforeDown", this.modeBeforeDown.name());
        nbt.putLong("StayUntil", this.stayUntil);
        nbt.putLong("DownUntil", this.downUntil);
        putPos(nbt, "LastKnown", this.lastKnown);
        putPos(nbt, "TripDestination", this.tripDestination);
        nbt.putLong("TripArrivesAt", this.tripArrivesAt);
        nbt.put("Snapshot", this.snapshot);
        nbt.putLong("Incarnation", this.incarnation);
        nbt.putBoolean("NeedsRestore", this.needsRestore);
        return nbt;
    }

    static @Nullable CompanionRecord fromNbt(NbtCompound nbt) {
        if (!nbt.containsUuid("Owner") || !nbt.containsUuid("Npc")) {
            return null;
        }
        CompanionRecord record = new CompanionRecord(nbt.getUuid("Owner"), nbt.getUuid("Npc"));
        record.mode = mode(nbt.getString("Mode"), CompanionMode.FOLLOWING);
        record.modeBeforeDown = mode(nbt.getString("ModeBeforeDown"), CompanionMode.FOLLOWING);
        record.stayUntil = nbt.getLong("StayUntil");
        record.downUntil = nbt.getLong("DownUntil");
        record.lastKnown = getPos(nbt, "LastKnown");
        record.tripDestination = getPos(nbt, "TripDestination");
        record.tripArrivesAt = nbt.getLong("TripArrivesAt");
        record.snapshot = nbt.getCompound("Snapshot");
        record.incarnation = nbt.getLong("Incarnation");
        record.needsRestore = nbt.getBoolean("NeedsRestore");
        return record;
    }

    private static CompanionMode mode(String name, CompanionMode fallback) {
        try {
            return CompanionMode.valueOf(name);
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    static void putPos(NbtCompound nbt, String key, @Nullable GlobalPos pos) {
        if (pos == null) {
            return;
        }
        NbtCompound entry = new NbtCompound();
        entry.putString("Dimension", pos.dimension().getValue().toString());
        entry.putLong("Pos", pos.pos().asLong());
        nbt.put(key, entry);
    }

    static @Nullable GlobalPos getPos(NbtCompound nbt, String key) {
        if (!nbt.contains(key)) {
            return null;
        }
        NbtCompound entry = nbt.getCompound(key);
        Identifier dimension = Identifier.tryParse(entry.getString("Dimension"));
        if (dimension == null) {
            return null;
        }
        RegistryKey<World> world = RegistryKey.of(RegistryKeys.WORLD, dimension);
        return GlobalPos.create(world, BlockPos.fromLong(entry.getLong("Pos")));
    }
}

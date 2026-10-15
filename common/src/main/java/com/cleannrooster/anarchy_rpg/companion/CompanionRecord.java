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
    /** World time the companion was last seen loaded. */
    long lastSeen;
    /** World time a loaded companion was first seen far from (or in another world than) its owner; 0 when not. */
    long separatedSince;
    /** In-game day its daily specialty was last used (see {@link CompanionSpecialties#today}); -1 for never. */
    long specialtyDay = -1;
    /** Juggernaut's standing order to guard the owner's spawn: survives trips and being downed. */
    boolean patrolling;
    /** Which ring point the patrol is walking to. Not saved: a reload just starts the ring over. */
    int patrolWaypoint;
    /** The current round (see PatrolSpecialty): the point being walked to, points settled and reached, time spent. Not saved. */
    @Nullable
    BlockPos patrolGoal;
    int patrolResolved;
    int patrolReached;
    long patrolRoundTicks;
    /** Complete rounds walked, and times it was leashed back for being stuck. Saved, for the debug command. */
    long completedPatrols;
    long patrolLeashes;
    /** Rogue guiding: the surface spot it is leading to, the structure there, and when it set off. */
    @Nullable
    GlobalPos guideTarget;
    @Nullable
    Identifier guideStructure;
    long guideStartedAt;
    /** Seconds spent waiting for the owner to catch up, for spacing out the hint. Not saved. */
    int guideWaited;
    /** A Rogue that waited too long for its owner and is heading home; meeting them resumes the lead. Not saved. */
    boolean guideReturning;
    /** Set when the owner respawned while it was down: it gets up at full health when it joins them. Not saved. */
    boolean healOnJoin;
    /** World time of the last vault while leading, for its cooldown. Not saved. */
    long guideVaultAt;
    /** Where the owner was a second ago, to measure how fast they are going. Not saved. */
    @Nullable
    net.minecraft.util.math.Vec3d guideOwnerLast;

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

    public long specialtyDay() {
        return this.specialtyDay;
    }

    public boolean patrolling() {
        return this.patrolling;
    }

    public long completedPatrols() {
        return this.completedPatrols;
    }

    public long patrolLeashes() {
        return this.patrolLeashes;
    }

    public long patrolRoundTicks() {
        return this.patrolRoundTicks;
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
        nbt.putLong("LastSeen", this.lastSeen);
        nbt.putLong("SpecialtyDay", this.specialtyDay);
        nbt.putBoolean("Patrolling", this.patrolling);
        nbt.putLong("CompletedPatrols", this.completedPatrols);
        nbt.putLong("PatrolLeashes", this.patrolLeashes);
        putPos(nbt, "GuideTarget", this.guideTarget);
        if (this.guideStructure != null) {
            nbt.putString("GuideStructure", this.guideStructure.toString());
        }
        nbt.putLong("GuideStartedAt", this.guideStartedAt);
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
        record.lastSeen = nbt.getLong("LastSeen");
        record.specialtyDay = nbt.contains("SpecialtyDay") ? nbt.getLong("SpecialtyDay") : -1;
        record.patrolling = nbt.getBoolean("Patrolling");
        record.completedPatrols = nbt.getLong("CompletedPatrols");
        record.patrolLeashes = nbt.getLong("PatrolLeashes");
        record.guideTarget = getPos(nbt, "GuideTarget");
        record.guideStructure = nbt.contains("GuideStructure") ? Identifier.tryParse(nbt.getString("GuideStructure")) : null;
        record.guideStartedAt = nbt.getLong("GuideStartedAt");
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

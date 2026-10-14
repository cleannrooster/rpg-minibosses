package com.cleannrooster.anarchy_rpg.hub;

import com.cleannrooster.anarchy_rpg.faction.PartyMembership;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One camp or tower: where it is, which party lives there, and who lives there now.
 *
 * <p>The party, and with it the faction and flag, is fixed when the hub is first set up. Every resident,
 * replacements included, joins that party, so a hub never changes flag.
 */
public final class HubAnchor {
    /** Someone living at the hub, and when they arrived (world time), so the longest-staying leave first. */
    public record Resident(UUID uuid, long arrivedAt) {
    }

    final HubKind kind;
    final BlockBox bounds;
    @Nullable
    PartyMembership party;
    final List<Resident> residents = new ArrayList<>();
    long lastRotation;
    boolean initialized;

    HubAnchor(HubKind kind, BlockBox bounds) {
        this.kind = kind;
        this.bounds = bounds;
    }

    public HubKind kind() {
        return this.kind;
    }

    public BlockBox bounds() {
        return this.bounds;
    }

    public BlockPos center() {
        return this.bounds.getCenter();
    }

    public @Nullable PartyMembership party() {
        return this.party;
    }

    public List<Resident> residents() {
        return List.copyOf(this.residents);
    }

    public long lastRotation() {
        return this.lastRotation;
    }

    NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putString("Kind", this.kind.name());
        nbt.putIntArray("Bounds", new int[]{this.bounds.getMinX(), this.bounds.getMinY(), this.bounds.getMinZ(),
                this.bounds.getMaxX(), this.bounds.getMaxY(), this.bounds.getMaxZ()});
        if (this.party != null) {
            nbt.put("Party", this.party.toNbt());
        }
        NbtList list = new NbtList();
        for (Resident resident : this.residents) {
            NbtCompound entry = new NbtCompound();
            entry.putUuid("Id", resident.uuid());
            entry.putLong("Arrived", resident.arrivedAt());
            list.add(entry);
        }
        nbt.put("Residents", list);
        nbt.putLong("LastRotation", this.lastRotation);
        nbt.putBoolean("Initialized", this.initialized);
        return nbt;
    }

    static @Nullable HubAnchor fromNbt(NbtCompound nbt) {
        HubKind kind;
        try {
            kind = HubKind.valueOf(nbt.getString("Kind"));
        } catch (IllegalArgumentException e) {
            return null;
        }
        int[] b = nbt.getIntArray("Bounds");
        if (b.length != 6) {
            return null;
        }
        HubAnchor anchor = new HubAnchor(kind, new BlockBox(b[0], b[1], b[2], b[3], b[4], b[5]));
        if (nbt.contains("Party")) {
            anchor.party = PartyMembership.fromNbt(nbt.getCompound("Party"));
        }
        for (NbtElement element : nbt.getList("Residents", NbtElement.COMPOUND_TYPE)) {
            NbtCompound entry = (NbtCompound) element;
            if (entry.containsUuid("Id")) {
                anchor.residents.add(new Resident(entry.getUuid("Id"), entry.getLong("Arrived")));
            }
        }
        anchor.lastRotation = nbt.getLong("LastRotation");
        anchor.initialized = nbt.getBoolean("Initialized");
        return anchor;
    }
}

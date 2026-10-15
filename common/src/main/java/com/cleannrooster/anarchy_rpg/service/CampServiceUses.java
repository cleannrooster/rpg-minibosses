package com.cleannrooster.anarchy_rpg.service;

import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * How many times each player has bought each camp service from one adventurer today. Saved on the adventurer,
 * so a resident replaced by hub turnover takes its counts with it.
 */
public final class CampServiceUses {
    /** Player, then service, to the day counted and the uses on it. */
    private final Map<UUID, Map<PaidServiceKind, long[]>> uses = new HashMap<>();

    public int usesOn(UUID player, PaidServiceKind kind, long day) {
        Map<PaidServiceKind, long[]> byKind = this.uses.get(player);
        long[] entry = byKind == null ? null : byKind.get(kind);
        return entry == null || entry[0] != day ? 0 : (int) entry[1];
    }

    public void record(UUID player, PaidServiceKind kind, long day) {
        long[] entry = this.uses.computeIfAbsent(player, uuid -> new HashMap<>()).computeIfAbsent(kind, k -> new long[]{day, 0});
        if (entry[0] != day) {
            entry[0] = day;
            entry[1] = 0;
        }
        entry[1]++;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        this.uses.forEach((player, byKind) -> {
            NbtCompound kinds = new NbtCompound();
            byKind.forEach((kind, entry) -> kinds.putLongArray(kind.name(), entry));
            nbt.put(player.toString(), kinds);
        });
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        this.uses.clear();
        for (String player : nbt.getKeys()) {
            NbtCompound kinds = nbt.getCompound(player);
            Map<PaidServiceKind, long[]> byKind = new HashMap<>();
            for (String name : kinds.getKeys()) {
                long[] entry = kinds.getLongArray(name);
                if (entry.length != 2) {
                    continue;
                }
                try {
                    byKind.put(PaidServiceKind.valueOf(name), entry);
                } catch (IllegalArgumentException ignored) {
                    // A service that no longer exists: its count is simply dropped.
                }
            }
            try {
                this.uses.put(UUID.fromString(player), byKind);
            } catch (IllegalArgumentException ignored) {
                // Not a UUID: skip it.
            }
        }
    }
}

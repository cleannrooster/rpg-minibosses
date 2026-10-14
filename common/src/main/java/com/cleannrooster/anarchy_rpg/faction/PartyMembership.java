package com.cleannrooster.anarchy_rpg.faction;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The party an adventurer belongs to, and through it the adventurer's faction. Every adventurer is in a
 * party: a patrol is one party, a lone wanderer is a party of one. Faction is assigned per party, so
 * everyone in a party flies the same flag.
 *
 * <p>The faction is stored alongside the party id on each member rather than looked up in a registry;
 * members of one party are always created with the same faction, and a party never changes faction.
 */
public record PartyMembership(UUID party, FactionSlot faction) {

    /** A new party with a faction drawn uniformly. */
    public static PartyMembership create(Random random) {
        return new PartyMembership(new UUID(random.nextLong(), random.nextLong()),
                FactionSlot.fromIndex(random.nextInt(FactionSlot.values().length)));
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        nbt.putUuid("Id", this.party);
        nbt.putInt("Faction", this.faction.index());
        return nbt;
    }

    public static @Nullable PartyMembership fromNbt(NbtCompound nbt) {
        if (!nbt.containsUuid("Id")) {
            return null;
        }
        return new PartyMembership(nbt.getUuid("Id"), FactionSlot.fromIndex(nbt.getInt("Faction")));
    }
}

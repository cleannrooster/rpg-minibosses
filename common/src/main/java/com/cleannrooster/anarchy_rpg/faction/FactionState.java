package com.cleannrooster.anarchy_rpg.faction;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.PersistentState;

/**
 * Holds the world's {@link FactionRoster} in the overworld's saved data. The roster is rolled from the
 * world seed the first time anything asks for it, and read from the save from then on.
 */
public final class FactionState extends PersistentState {
    private static final String ID = RPGMinibosses.CONTENT_NAMESPACE + "_factions";

    private final FactionRoster roster;

    private FactionState(FactionRoster roster) {
        this.roster = roster;
    }

    public FactionRoster roster() {
        return this.roster;
    }

    public static FactionRoster roster(MinecraftServer server) {
        return get(server).roster;
    }

    public static FactionState get(MinecraftServer server) {
        var overworld = server.getOverworld();
        long seed = overworld.getSeed();
        return overworld.getPersistentStateManager().getOrCreate(new PersistentState.Type<>(
                () -> {
                    FactionState created = new FactionState(FactionRoster.generate(seed, FactionPools.current()));
                    created.markDirty();
                    return created;
                },
                FactionState::fromNbt,
                null), ID);
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        nbt.put("Roster", this.roster.toNbt());
        return nbt;
    }

    private static FactionState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        return new FactionState(FactionRoster.fromNbt(nbt.getCompound("Roster")));
    }
}

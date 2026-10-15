package com.cleannrooster.anarchy_rpg.rumor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where worldgen encounters placed their bosses, so rumors can tell of a boss whose chunk is not loaded.
 * An entry is added when the encounter spawns its boss and removed when that boss truly dies or is found
 * recruited. Encounters placed before 3.0 are not listed; their bosses are only found while loaded.
 */
public final class EncounterLedger extends PersistentState {
    private static final String ID = RPGMinibosses.CONTENT_NAMESPACE + "_encounters";

    public record Placement(UUID boss, RegistryKey<World> dimension, BlockPos pos, Identifier entityType) {
    }

    private final Map<UUID, Placement> placements = new HashMap<>();

    public static EncounterLedger get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(EncounterLedger::new, EncounterLedger::fromNbt, null), ID);
    }

    public void add(Placement placement) {
        this.placements.put(placement.boss(), placement);
        this.markDirty();
    }

    public void remove(UUID boss) {
        if (this.placements.remove(boss) != null) {
            this.markDirty();
        }
    }

    /** A copy, so callers may remove entries while going through it. */
    public Collection<Placement> all() {
        return List.copyOf(this.placements.values());
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound list = new NbtCompound();
        for (Placement placement : this.placements.values()) {
            NbtCompound e = new NbtCompound();
            e.putString("Dimension", placement.dimension().getValue().toString());
            e.putLong("Pos", placement.pos().asLong());
            e.putString("Type", placement.entityType().toString());
            list.put(placement.boss().toString(), e);
        }
        nbt.put("Placements", list);
        return nbt;
    }

    private static EncounterLedger fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        EncounterLedger ledger = new EncounterLedger();
        NbtCompound list = nbt.getCompound("Placements");
        for (String key : list.getKeys()) {
            NbtCompound e = list.getCompound(key);
            Identifier dimension = Identifier.tryParse(e.getString("Dimension"));
            Identifier type = Identifier.tryParse(e.getString("Type"));
            if (dimension == null || type == null) {
                continue;
            }
            UUID boss = UUID.fromString(key);
            ledger.placements.put(boss, new Placement(boss, RegistryKey.of(RegistryKeys.WORLD, dimension),
                    BlockPos.fromLong(e.getLong("Pos")), type));
        }
        return ledger;
    }
}

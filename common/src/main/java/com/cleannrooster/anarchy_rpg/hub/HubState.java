package com.cleannrooster.anarchy_rpg.hub;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;

/** Every hub found in one world, keyed by the chunk its structure starts in. Saved with that world. */
public final class HubState extends PersistentState {
    private static final String ID = RPGMinibosses.CONTENT_NAMESPACE + "_hubs";

    final Map<Long, HubAnchor> anchors = new HashMap<>();

    public static HubState get(ServerWorld world) {
        return world.getPersistentStateManager().getOrCreate(
                new PersistentState.Type<>(HubState::new, HubState::fromNbt, null), ID);
    }

    public Map<Long, HubAnchor> anchors() {
        return this.anchors;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        NbtCompound all = new NbtCompound();
        this.anchors.forEach((key, anchor) -> all.put(Long.toString(key), anchor.toNbt()));
        nbt.put("Hubs", all);
        return nbt;
    }

    private static HubState fromNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registries) {
        HubState state = new HubState();
        NbtCompound all = nbt.getCompound("Hubs");
        for (String key : all.getKeys()) {
            try {
                HubAnchor anchor = HubAnchor.fromNbt(all.getCompound(key));
                if (anchor != null) {
                    state.anchors.put(Long.parseLong(key), anchor);
                }
            } catch (NumberFormatException ignored) {
                // A malformed key loses only that hub; it is found again as new.
            }
        }
        return state;
    }
}

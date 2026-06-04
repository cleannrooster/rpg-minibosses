package com.cleannrooster.rpg_minibosses.entity.brain;

import java.util.HashMap;
import java.util.Map;

public final class AbilityCooldownRegistry {
    private final Map<String, Integer> cooldownsInTicks = new HashMap<>();

    public boolean isReady(String id) {
        return cooldownsInTicks.getOrDefault(id, 0) <= 0;
    }

    /** Start or reset a cooldown. durationTicks is clamped to >= 1. */
    public void trigger(String id, int durationTicks) {
        cooldownsInTicks.put(id, Math.max(1, durationTicks));
    }

    /** Decrement all active cooldowns by one tick. Call once per server tick. */
    public void tick() {
        cooldownsInTicks.replaceAll((id, ticks) -> Math.max(0, ticks - 1));
    }

    /** Ticks remaining on the given cooldown (0 if ready). */
    public int remaining(String id) {
        return cooldownsInTicks.getOrDefault(id, 0);
    }

    /** Immediately clear a cooldown (mark as ready). */
    public void clear(String id) {
        cooldownsInTicks.put(id, 0);
    }
}

package com.cleannrooster.rpg_minibosses.entity.combat;

import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every {@link CombatAction} in the mod, keyed by id.
 *
 * <p>This is what lets a swing be networked as a single id plus an
 * {@link com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackFrame}: the client already knows
 * the arc span, the reach, the plane and the ribbon's look, so nothing about the attack's shape has to go
 * over the wire. A hundred-and-sixty-degree cleave costs exactly as much bandwidth as a shove.
 *
 * <p>Actions register themselves as they are built. Brains are constructed on both sides — {@code initGoals}
 * runs in the entity constructor — so any client that has a Juggernaut loaded already has the Juggernaut's
 * attacks. Registration is first-wins: every Juggernaut builds the same geometry under the same ids.
 */
public final class AttackRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, CombatAction> ACTIONS = new ConcurrentHashMap<>();

    private AttackRegistry() {
    }

    static void register(CombatAction action) {
        ACTIONS.putIfAbsent(action.id(), action);
    }

    /**
     * Look up an action received over the network. Logs rather than throwing, so a mismatch costs a
     * missing visual instead of a client crash.
     */
    public static @Nullable CombatAction lookupForClient(String id) {
        var action = ACTIONS.get(id);
        if (action == null) {
            LOGGER.warn("[rpg-minibosses] received an unknown attack id for a swing visual: {}", id);
        }
        return action;
    }
}

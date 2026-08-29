package com.cleannrooster.rpg_minibosses.entity.combat;

import com.mojang.logging.LogUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Every {@link CombatAction} in the mod, keyed by id.
 *
 * <p>This is what lets a swing be networked as a single id plus an
 * {@link com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackFrame}: the client already knows
 * the arc span, the reach, the plane and the ribbon's look, so nothing about the attack's shape has to go
 * over the wire. A hundred-and-sixty-degree cleave costs exactly as much bandwidth as a shove.
 *
 * <p>Actions register themselves as they are built, and registration is first-wins: every Juggernaut
 * builds the same geometry under the same ids, so one registration per mob class is enough.
 *
 * <p><b>The client does not get these for free.</b> {@code MobEntity}'s constructor guards its call to
 * {@code initGoals()} with {@code if (world != null && !world.isClient)}, so brains — and therefore
 * every action they build — are only ever constructed server-side. On an integrated server that is
 * invisible, because the client shares this JVM and reads the map the server thread filled in. On a
 * dedicated server the client's map stays empty, every incoming swing id misses, and no ribbon is ever
 * drawn. {@link #ensureClientPrototypes} exists to close that gap.
 */
public final class AttackRegistry {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<String, CombatAction> ACTIONS = new ConcurrentHashMap<>();

    /** Mob classes whose attack geometry this client has already built. */
    private static final Set<Class<?>> CLIENT_PROTOTYPED = ConcurrentHashMap.newKeySet();

    private AttackRegistry() {
    }

    /**
     * Make sure this client knows the geometry of the given mob's attacks.
     *
     * <p>Builds one throwaway brain per mob class, purely so its actions register themselves. Cheap:
     * a set lookup on every call after the first for a given class, and the brain is discarded — the
     * client never runs one, it only needs the shapes they defined.
     */
    public static void ensureClientPrototypes(
            com.cleannrooster.rpg_minibosses.entity.MinibossEntity entity) {
        if (CLIENT_PROTOTYPED.add(entity.getClass())) {
            entity.registerAttackPrototypes();
        }
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

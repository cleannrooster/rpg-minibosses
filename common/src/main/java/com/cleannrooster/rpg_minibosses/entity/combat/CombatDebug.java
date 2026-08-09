package com.cleannrooster.rpg_minibosses.entity.combat;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.mojang.logging.LogUtils;
import net.minecraft.util.math.Vec3d;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * Lightweight invariant checks for the combat movement system.
 *
 * <p>These exist because the failures this overhaul is most exposed to are silent: a mob left owning its
 * own movement forever, an integrator that accumulates speed nobody asked for, or an airborne mob that
 * never lands. None of those throw — they just make the fight feel wrong three minutes later. Each check
 * logs once per mob per kind, so a broken action reports itself without flooding the log.
 *
 * <p>Off unless {@link #enabled} is set; the calls are cheap enough to leave in the tick path.
 */
public final class CombatDebug {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** Flipped on by a developer; there is deliberately no config knob for this. */
    public static boolean enabled = false;

    /** Speed no grounded miniboss should ever reach through steering alone, blocks/tick. */
    private static final double RUNAWAY_SPEED = 1.2;
    /** Ticks an action may own movement before something is clearly wrong. */
    private static final int MAX_OWNERSHIP_TICKS = 200;

    private static final Map<Integer, Integer> OWNERSHIP_TICKS = new HashMap<>();
    private static final Map<String, Boolean> REPORTED = new HashMap<>();

    private CombatDebug() {
    }

    /** Call once per server tick per mob, at the end of the brain tick. */
    public static void verify(MinibossEntity entity, GroundLocomotion locomotion, ActionRunner runner) {
        if (!enabled || entity.getWorld().isClient()) {
            return;
        }
        var id = entity.getId();

        if (locomotion.owner() == MovementOwner.ATTACK_MOTION) {
            var ticks = OWNERSHIP_TICKS.merge(id, 1, Integer::sum);
            if (ticks > MAX_OWNERSHIP_TICKS && !runner.isBusy()) {
                report(entity, "stuck-ownership",
                        "held ATTACK_MOTION for " + ticks + " ticks with no action running");
            }
        } else {
            OWNERSHIP_TICKS.remove(id);
        }

        var speed = locomotion.speed();
        if (speed > RUNAWAY_SPEED) {
            report(entity, "runaway-speed", "steering reached " + String.format("%.2f", speed) + " b/t");
        }
        if (Double.isNaN(speed)) {
            report(entity, "nan-steering", "steering velocity became NaN");
        }

        var velocity = entity.getVelocity();
        if (Double.isNaN(velocity.x) || Double.isNaN(velocity.z)) {
            report(entity, "nan-velocity", "entity velocity became NaN");
        }
    }

    /** Called when an action starts, so a definition with impossible timing is caught at authoring time. */
    public static void verifyAction(CombatAction action) {
        if (!enabled) {
            return;
        }
        if (action.activeTicks() <= 0 && action.advance() != 0.0) {
            report("action:" + action.id(), action.id() + " has advance but no active window");
        }
        if (action.motion().isCommitted() && action.committedSpeed() <= 0.0) {
            report("action:" + action.id(), action.id() + " is committed but has zero committed speed");
        }
        if (action.windupTicks() <= 0 && action.motion() != AttackMotion.MOBILE) {
            report("action:" + action.id(), action.id() + " commits with no telegraph");
        }
    }

    /** Sanity check for a leap destination before it is committed to. */
    public static boolean isSaneDestination(MinibossEntity entity, Vec3d destination) {
        var delta = destination.subtract(entity.getPos());
        return delta.horizontalLength() < 32.0 && Math.abs(delta.y) < 16.0;
    }

    private static void report(MinibossEntity entity, String kind, String message) {
        report(entity.getType().getUntranslatedName() + ":" + kind, message);
    }

    private static void report(String key, String message) {
        if (REPORTED.putIfAbsent(key, Boolean.TRUE) == null) {
            LOGGER.warn("[rpg-minibosses combat] {}", message);
        }
    }
}

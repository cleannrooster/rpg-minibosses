package com.cleannrooster.rpg_minibosses.magic;

import dev.architectury.event.events.common.LifecycleEvent;
import dev.architectury.event.events.common.TickEvent;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Delayed server work, per world — the core replacement for Spell Engine's {@code WorldScheduler}.
 *
 * <p>A task runs at the end of the world tick {@code delay} ticks from now (a delay of 0 or less runs it
 * at the end of the current tick). Tasks scheduled while tasks are running wait for the next tick, so a
 * task that reschedules itself cannot loop within one tick. Everything is dropped when the server stops.</p>
 */
public final class TickScheduler {
    private record Task(long dueTick, Runnable action) {
    }

    private static final Map<RegistryKey<World>, List<Task>> PENDING = new HashMap<>();

    private TickScheduler() {
    }

    /** Wire the drain once, from common init. */
    public static void register() {
        TickEvent.SERVER_LEVEL_POST.register(TickScheduler::drain);
        LifecycleEvent.SERVER_STOPPED.register(server -> PENDING.clear());
    }

    public static void schedule(ServerWorld world, int delay, Runnable action) {
        PENDING.computeIfAbsent(world.getRegistryKey(), key -> new ArrayList<>())
                .add(new Task(world.getTime() + Math.max(0, delay), action));
    }

    private static void drain(ServerWorld world) {
        var tasks = PENDING.get(world.getRegistryKey());
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        long now = world.getTime();
        List<Task> due = new ArrayList<>();
        tasks.removeIf(task -> {
            if (task.dueTick() <= now) {
                due.add(task);
                return true;
            }
            return false;
        });
        for (Task task : due) {
            task.action().run();
        }
    }
}

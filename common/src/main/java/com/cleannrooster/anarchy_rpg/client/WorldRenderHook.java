package com.cleannrooster.anarchy_rpg.client;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A world-space draw pass after entities. Architectury has no world-render event, so each loader's client
 * entrypoint forwards its own after-entities hook into {@link #fire} (Fabric: {@code WorldRenderEvents.AFTER_ENTITIES};
 * NeoForge: {@code RenderLevelStageEvent} at {@code AFTER_ENTITIES}).
 */
public final class WorldRenderHook {
    @FunctionalInterface
    public interface Listener {
        void render(MatrixStack matrices, Camera camera, VertexConsumerProvider consumers, float partialTick);
    }

    private static final List<Listener> LISTENERS = new ArrayList<>();

    private WorldRenderHook() {
    }

    public static void register(Listener listener) {
        LISTENERS.add(listener);
    }

    /** Called by the loader adapters only. */
    public static void fire(MatrixStack matrices, Camera camera, VertexConsumerProvider consumers, float partialTick) {
        for (Listener listener : LISTENERS) {
            listener.render(matrices, camera, consumers, partialTick);
        }
    }
}

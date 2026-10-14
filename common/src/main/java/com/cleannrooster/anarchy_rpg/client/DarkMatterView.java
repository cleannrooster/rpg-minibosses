package com.cleannrooster.anarchy_rpg.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

/**
 * Whether the local player is inside Forsaken Magus's dark-matter view (end sky, no terrain, only the
 * Magus and players drawn). The render mixins that implement the view also serve the guild hall, so
 * they stay in the core mod and ask this class; the Spell Engine compat installs the real {@link Source}.
 * Without it the view is never active.
 */
public final class DarkMatterView {
    public interface Source {
        boolean isActive(PlayerEntity viewer);

        /** Living entities that stay visible while the view is active, besides players. */
        boolean isExempt(Entity entity);
    }

    private static Source source = new Source() {
        @Override public boolean isActive(PlayerEntity viewer) { return false; }
        @Override public boolean isExempt(Entity entity) { return false; }
    };

    private DarkMatterView() {
    }

    public static void install(Source source) {
        DarkMatterView.source = source;
    }

    public static boolean isActive() {
        MinecraftClient client = MinecraftClient.getInstance();
        return client != null && client.player != null && source.isActive(client.player);
    }

    public static boolean isExempt(Entity entity) {
        return source.isExempt(entity);
    }
}

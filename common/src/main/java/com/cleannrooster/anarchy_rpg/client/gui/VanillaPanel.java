package com.cleannrooster.anarchy_rpg.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

/**
 * Vanilla's container panel at any size, cut from the dispenser texture: corners as they are, edges and
 * interior <em>tiled</em> from plain regions (never stretched), so a resource pack's textured panel stays
 * intact.
 */
public final class VanillaPanel {
    private static final Identifier PANEL = Identifier.ofVanilla("textures/gui/container/dispenser.png");
    private static final int TEXTURE_WIDTH = 176;
    private static final int TEXTURE_HEIGHT = 166;
    /** The bevel sliced off each edge and corner. */
    private static final int BORDER = 4;
    /** A plain stretch of the panel left of its 3x3 grid (56x76 in vanilla). */
    private static final int PLAIN_U = BORDER;
    private static final int PLAIN_V = BORDER;
    private static final int PLAIN_WIDTH = 56;
    private static final int PLAIN_HEIGHT = 76;

    private VanillaPanel() {
    }

    public static void draw(DrawContext context, int x, int y, int width, int height) {
        int b = BORDER;
        int innerW = width - 2 * b;
        int innerH = height - 2 * b;
        tile(context, x, y, b, b, 0, 0, b, b);
        tile(context, x + width - b, y, b, b, TEXTURE_WIDTH - b, 0, b, b);
        tile(context, x, y + height - b, b, b, 0, TEXTURE_HEIGHT - b, b, b);
        tile(context, x + width - b, y + height - b, b, b, TEXTURE_WIDTH - b, TEXTURE_HEIGHT - b, b, b);
        tile(context, x + b, y, innerW, b, PLAIN_U, 0, PLAIN_WIDTH, b);
        tile(context, x + b, y + height - b, innerW, b, PLAIN_U, TEXTURE_HEIGHT - b, PLAIN_WIDTH, b);
        tile(context, x, y + b, b, innerH, 0, PLAIN_V, b, PLAIN_HEIGHT);
        tile(context, x + width - b, y + b, b, innerH, TEXTURE_WIDTH - b, PLAIN_V, b, PLAIN_HEIGHT);
        tile(context, x + b, y + b, innerW, innerH, PLAIN_U, PLAIN_V, PLAIN_WIDTH, PLAIN_HEIGHT);
    }

    /** Repeats the texture region (u, v, tileWidth x tileHeight) over the area, clipping the last tiles. */
    private static void tile(DrawContext context, int x, int y, int width, int height, int u, int v, int tileWidth, int tileHeight) {
        for (int dy = 0; dy < height; dy += tileHeight) {
            int h = Math.min(tileHeight, height - dy);
            for (int dx = 0; dx < width; dx += tileWidth) {
                int w = Math.min(tileWidth, width - dx);
                context.drawTexture(PANEL, x + dx, y + dy, u, v, w, h, 256, 256);
            }
        }
    }
}

package com.cleannrooster.anarchy_rpg.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The look shared by the mod's container windows (Exchange, payment, companion storage and equipment), matching the
 * conversation screen: vanilla's {@code popup/background} panel with white, shadowed text, and vanilla's slot sprite
 * under every slot. All vanilla sprites, so resource packs restyle them; the panel is a nine-slice sprite, so it fits
 * any size without stretching its border. Drawn on the panel rather than on the dispenser texture, the text reads under
 * packs that repaint containers dark.
 */
public final class ContainerChrome {
    private static final Identifier PANEL = Identifier.ofVanilla("popup/background");
    private static final Identifier SLOT = Identifier.ofVanilla("container/slot");
    public static final int SLOT_SIZE = 18;

    /** Text on the panel, as in the conversation screen. */
    public static final int TEXT = 0xFFFFFF;
    /** Secondary text: labels and amounts that matter less. */
    public static final int MUTED = 0xAAAAAA;
    /** A price the offer covers, or does not: the anvil's green and red, bright enough for the gray panel. */
    public static final int AFFORDABLE = 0x80FF20;
    public static final int UNAFFORDABLE = 0xFF6060;

    private ContainerChrome() {
    }

    public static void panel(DrawContext context, int x, int y, int width, int height) {
        context.drawGuiTexture(PANEL, x, y, width, height);
    }

    /** Vanilla's slot sprite under every slot of the handler (window-relative slot positions, offset by x, y). */
    public static void slots(DrawContext context, int x, int y, ScreenHandler handler) {
        for (Slot slot : handler.slots) {
            context.drawGuiTexture(SLOT, x + slot.x - 1, y + slot.y - 1, SLOT_SIZE, SLOT_SIZE);
        }
    }

    public static void text(DrawContext context, TextRenderer renderer, Text text, int x, int y, int color) {
        context.drawText(renderer, text, x, y, color, true);
    }

    public static void text(DrawContext context, TextRenderer renderer, OrderedText text, int x, int y, int color) {
        context.drawText(renderer, text, x, y, color, true);
    }

    public static void text(DrawContext context, TextRenderer renderer, String text, int x, int y, int color) {
        context.drawText(renderer, text, x, y, color, true);
    }
}

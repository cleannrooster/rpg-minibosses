package com.cleannrooster.anarchy_rpg.client.companion;

import com.cleannrooster.anarchy_rpg.client.gui.ContainerChrome;
import com.cleannrooster.anarchy_rpg.companion.CompanionEquipmentScreenHandler;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A companion's equipment, laid out like vanilla's inventory screen: armor column, the companion drawn in
 * vanilla's preview box beside it, then its main and off hand. All vanilla art, in the conversation screen's look
 * ({@link ContainerChrome}): the gray popup panel, the inventory screen's preview box, slot sprites, the empty-slot
 * icons, and white shadowed text.
 */
public final class CompanionEquipmentScreen extends HandledScreen<CompanionEquipmentScreenHandler> {
    private static final Identifier INVENTORY = Identifier.ofVanilla("textures/gui/container/inventory.png");
    /** The inventory screen's black preview box, where the player is drawn. */
    private static final int PREVIEW_U = 25;
    private static final int PREVIEW_V = 7;
    private static final int PREVIEW_WIDTH = 51;
    private static final int PREVIEW_HEIGHT = 72;
    private static final int TEXT = ContainerChrome.TEXT;

    public CompanionEquipmentScreen(CompanionEquipmentScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.titleX = 98;
        this.titleY = 8;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        ContainerChrome.panel(context, this.x, this.y, this.backgroundWidth, this.backgroundHeight);
        context.drawTexture(INVENTORY, this.x + PREVIEW_U, this.y + PREVIEW_V, PREVIEW_U, PREVIEW_V, PREVIEW_WIDTH, PREVIEW_HEIGHT);
        ContainerChrome.slots(context, this.x, this.y, this.handler);
        MinibossEntity companion = this.handler.companion();
        if (companion != null) {
            InventoryScreen.drawEntity(context, this.x + 26, this.y + 8, this.x + 75, this.y + 78, 30, 0.0625F,
                    mouseX, mouseY, companion);
        }
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        // The name wraps in the free space right of the hand slots. Like vanilla's inventory screen, there is
        // no "Inventory" label: the armor column runs down to where it would sit.
        int room = this.backgroundWidth - this.titleX - 8;
        int y = this.titleY;
        for (net.minecraft.text.OrderedText line : this.textRenderer.wrapLines(this.title, room)) {
            ContainerChrome.text(context, this.textRenderer, line, this.titleX, y, TEXT);
            y += 10;
        }
    }
}

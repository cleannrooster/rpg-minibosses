package com.cleannrooster.anarchy_rpg.client.companion;

import com.cleannrooster.anarchy_rpg.client.gui.ContainerChrome;
import com.cleannrooster.anarchy_rpg.companion.CompanionStorageScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A companion's storage, laid out like vanilla's dispenser, in the conversation screen's look
 * ({@link ContainerChrome}): the gray popup panel, a slot sprite under every slot, white shadowed text.
 */
public final class CompanionStorageScreen extends HandledScreen<CompanionStorageScreenHandler> {
    public CompanionStorageScreen(CompanionStorageScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        ContainerChrome.panel(context, this.x, this.y, this.backgroundWidth, this.backgroundHeight);
        ContainerChrome.slots(context, this.x, this.y, this.handler);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        ContainerChrome.text(context, this.textRenderer, this.title, this.titleX, this.titleY, ContainerChrome.TEXT);
        ContainerChrome.text(context, this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX,
                this.playerInventoryTitleY, ContainerChrome.TEXT);
    }
}

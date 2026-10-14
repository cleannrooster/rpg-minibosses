package com.cleannrooster.anarchy_rpg.client.companion;

import com.cleannrooster.anarchy_rpg.companion.CompanionStorageScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A companion's storage, drawn with vanilla's dispenser texture at its native size, so it looks exactly like
 * a vanilla container and follows resource packs.
 */
public final class CompanionStorageScreen extends HandledScreen<CompanionStorageScreenHandler> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/dispenser.png");

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
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }
}

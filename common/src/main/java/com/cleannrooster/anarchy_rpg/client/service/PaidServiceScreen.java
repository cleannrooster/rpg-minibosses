package com.cleannrooster.anarchy_rpg.client.service;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.exchange.ItemValues;
import com.cleannrooster.anarchy_rpg.service.PaidServiceScreenHandler;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

/**
 * Paying an adventurer for a service: vanilla's dispenser screen as is, with the payment going in its 3x3 grid.
 * The price and the offer's value sit in the empty space left of the grid, the confirm button (labelled by the
 * service: "Hire", ...) right of it. Container text: 0x404040, no shadow.
 */
public final class PaidServiceScreen extends HandledScreen<PaidServiceScreenHandler> {
    private static final Identifier TEXTURE = Identifier.ofVanilla("textures/gui/container/dispenser.png");
    private static final int TEXT = 0x404040;
    private static final int AFFORDABLE = 0x2F7F1F;
    private static final int UNAFFORDABLE = 0xA02828;
    /** The empty space left of the dispenser grid, and right of it. */
    private static final int LEFT_X = 8;
    private static final int RIGHT_X = 122;
    private static final int RIGHT_WIDTH = 46;

    private ButtonWidget confirmButton;

    public PaidServiceScreen(PaidServiceScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
    }

    @Override
    protected void init() {
        super.init();
        this.titleX = (this.backgroundWidth - this.textRenderer.getWidth(this.title)) / 2;
        this.confirmButton = this.addDrawableChild(ButtonWidget.builder(this.handler.kind().buttonLabel(), button -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, PaidServiceScreenHandler.CONFIRM_BUTTON);
                    }
                })
                .dimensions(this.x + RIGHT_X, this.y + 35, RIGHT_WIDTH, 20)
                .build());
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        this.confirmButton.active = this.handler.affordable();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        context.drawTexture(TEXTURE, this.x, this.y, 0, 0, this.backgroundWidth, this.backgroundHeight);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, TEXT, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, TEXT, false);
        int priceColor = this.handler.affordable() ? AFFORDABLE : UNAFFORDABLE;
        drawAmount(context, 20, label("price"), this.handler.price(), priceColor);
        drawAmount(context, 44, label("offered"), this.handler.offerValue(), TEXT);
    }

    /** A label line, then the amount and a small currency icon on the line below. */
    private void drawAmount(DrawContext context, int y, Text label, double amount, int color) {
        context.drawText(this.textRenderer, label, LEFT_X, y, TEXT, false);
        String value = String.format(Locale.ROOT, "%.1f", amount);
        context.drawText(this.textRenderer, value, LEFT_X, y + 10, color, false);
        int iconX = LEFT_X + this.textRenderer.getWidth(value) + 2;
        context.getMatrices().push();
        context.getMatrices().translate(iconX, y + 9, 0);
        context.getMatrices().scale(0.625F, 0.625F, 1.0F);
        context.drawItem(ItemValues.currency().getDefaultStack(), 0, 0);
        context.getMatrices().pop();
    }

    private static Text label(String key) {
        return Text.translatable("service." + RPGMinibosses.CONTENT_NAMESPACE + "." + key);
    }
}

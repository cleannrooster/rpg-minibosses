package com.cleannrooster.anarchy_rpg.client.exchange;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.client.gui.VanillaPanel;
import com.cleannrooster.anarchy_rpg.exchange.ExchangeLayout;
import com.cleannrooster.anarchy_rpg.exchange.ExchangePricing;
import com.cleannrooster.anarchy_rpg.exchange.ExchangeScreenHandler;
import com.cleannrooster.anarchy_rpg.exchange.ExchangeStock;
import com.cleannrooster.anarchy_rpg.exchange.ItemValues;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

/**
 * The exchange window, drawn entirely from vanilla's own GUI art so resource packs restyle it: the
 * dispenser panel cut into corners, edges and interior and <em>tiled</em> to fit three 3x3 panes (tiling
 * rather than stretching keeps a pack's textured panel intact), vanilla's slot sprite under every slot, and
 * the villager trade arrow from stock to request. Text follows vanilla containers: 0x404040, no shadow.
 */
public final class ExchangeScreen extends HandledScreen<ExchangeScreenHandler> {
    private static final Identifier SLOT = Identifier.ofVanilla("container/slot");
    private static final Identifier ARROW = Identifier.ofVanilla("container/villager/trade_arrow");
    private static final int TEXT = 0x404040;
    /** The anvil's colors for an affordable / unaffordable price, darkened to read on the gray panel. */
    private static final int AFFORDABLE = 0x2F7F1F;
    private static final int UNAFFORDABLE = 0xA02828;

    private ButtonWidget tradeButton;

    public ExchangeScreen(ExchangeScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundWidth = ExchangeLayout.WIDTH;
        this.backgroundHeight = ExchangeLayout.HEIGHT;
        this.playerInventoryTitleX = ExchangeLayout.INVENTORY_X - 1;
        this.playerInventoryTitleY = ExchangeLayout.INVENTORY_Y - 11;
    }

    @Override
    protected void init() {
        super.init();
        this.tradeButton = this.addDrawableChild(ButtonWidget.builder(label("trade"), button -> {
                    if (this.client != null && this.client.interactionManager != null) {
                        this.client.interactionManager.clickButton(this.handler.syncId, ExchangeScreenHandler.TRADE_BUTTON);
                    }
                })
                .dimensions(this.x + ExchangeLayout.OFFER_X - 1, this.y + ExchangeLayout.TRADE_BUTTON_Y,
                        ExchangeLayout.GRID * ExchangeLayout.SLOT, 20)
                .build());
    }

    @Override
    protected void handledScreenTick() {
        super.handledScreenTick();
        this.tradeButton.active = this.handler.displayedQuote().acceptable();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        VanillaPanel.draw(context, this.x, this.y, this.backgroundWidth, this.backgroundHeight);
        for (Slot slot : this.handler.slots) {
            context.drawGuiTexture(SLOT, this.x + slot.x - 1, this.y + slot.y - 1, ExchangeLayout.SLOT, ExchangeLayout.SLOT);
        }
        int arrowX = this.x + ExchangeLayout.STOCK_X + ExchangeLayout.GRID * ExchangeLayout.SLOT + 2;
        int arrowY = this.y + ExchangeLayout.PANES_Y + ExchangeLayout.SLOT + 4;
        context.drawGuiTexture(ARROW, arrowX, arrowY, 10, 9);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, 8, 6, TEXT, false);
        int labelY = ExchangeLayout.PANES_Y - 10;
        context.drawText(this.textRenderer, label("stock"), ExchangeLayout.STOCK_X - 1, labelY, TEXT, false);
        context.drawText(this.textRenderer, label("request"), ExchangeLayout.REQUEST_X - 1, labelY, TEXT, false);
        context.drawText(this.textRenderer, label("offer"), ExchangeLayout.OFFER_X - 1, labelY, TEXT, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle, this.playerInventoryTitleX, this.playerInventoryTitleY, TEXT, false);

        ExchangePricing.Quote quote = this.handler.displayedQuote();
        int priceColor = quote.cost() <= 0.0 ? TEXT : (quote.acceptable() ? AFFORDABLE : UNAFFORDABLE);
        int paneWidth = ExchangeLayout.GRID * ExchangeLayout.SLOT - 1;
        drawAmount(context, ExchangeLayout.REQUEST_X - 1 + paneWidth, ExchangeLayout.QUOTE_Y, label("cost"), quote.cost(), priceColor);
        drawAmount(context, ExchangeLayout.OFFER_X - 1 + paneWidth, ExchangeLayout.QUOTE_Y, label("value"), quote.offer(), TEXT);
    }

    /** Currency icon drawn at 10px. */
    private static final int ICON_SIZE = 10;

    /** "Cost 12.5" and a small currency icon, ending exactly at {@code right} so it stays under its pane. */
    private void drawAmount(DrawContext context, int right, int y, Text label, double amount, int color) {
        Text text = label.copy().append(" " + String.format(Locale.ROOT, "%.1f", amount));
        int iconX = right - ICON_SIZE;
        int x = iconX - 2 - this.textRenderer.getWidth(text);
        context.drawText(this.textRenderer, text, x, y, color, false);
        context.getMatrices().push();
        context.getMatrices().translate(iconX, y - 1, 0);
        context.getMatrices().scale(0.625F, 0.625F, 1.0F);
        context.drawItem(ItemValues.currency().getDefaultStack(), 0, 0);
        context.getMatrices().pop();
    }

    private static Text label(String key) {
        return Text.translatable("exchange." + RPGMinibosses.CONTENT_NAMESPACE + "." + key);
    }

    static {
        // Stock size is part of the layout; keep the two in step.
        if (ExchangeStock.SIZE != ExchangeLayout.GRID * ExchangeLayout.GRID) {
            throw new IllegalStateException("Exchange stock size must fill the 3x3 pane");
        }
    }
}

package com.cleannrooster.anarchy_rpg.exchange;

/**
 * Where everything sits in the exchange window, shared by the screen handler (slot positions) and the
 * screen (drawing), so the two can never disagree. Positions are item positions, as vanilla slots use;
 * a slot's frame is drawn one pixel up and left of them.
 */
public final class ExchangeLayout {
    public static final int WIDTH = 226;
    public static final int HEIGHT = 206;

    public static final int GRID = 3;
    public static final int SLOT = 18;

    public static final int PANES_Y = 30;
    public static final int STOCK_X = 8;
    public static final int REQUEST_X = 86;
    public static final int OFFER_X = 164;

    /** The quote lines under the request and offer panes, each right-aligned to its pane. */
    public static final int QUOTE_Y = PANES_Y + GRID * SLOT + 4;
    public static final int TRADE_BUTTON_Y = QUOTE_Y + 12;

    public static final int INVENTORY_X = (WIDTH - 9 * SLOT) / 2 + 1;
    public static final int INVENTORY_Y = 124;
    public static final int HOTBAR_Y = INVENTORY_Y + 3 * SLOT + 4;

    private ExchangeLayout() {
    }
}

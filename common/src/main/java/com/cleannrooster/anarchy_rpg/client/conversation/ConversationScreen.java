package com.cleannrooster.anarchy_rpg.client.conversation;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.conversation.net.ConversationActionPayload;
import com.cleannrooster.anarchy_rpg.conversation.net.OpenConversationPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ColorHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The dialogue screen, laid out the way vanilla's own popups are: the dark {@code popup/background} panel
 * with 18px of padding, centered white text with shadow, 12px between blocks and 6px between buttons.
 *
 * <p>Three blocks: a header (name, faction in its flag color, how they regard you), the greeting, and the
 * options with Goodbye last. Purely a view; every choice goes to the server.
 */
public final class ConversationScreen extends Screen {
    private static final Identifier PANEL = Identifier.ofVanilla("popup/background");
    /** Vanilla PopupScreen's padding between the panel edge and its content. */
    private static final int PADDING = 18;
    /** Vanilla PopupScreen's gap between content blocks. */
    private static final int BLOCK_GAP = 12;
    private static final int BUTTON_GAP = 6;
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LINE_HEIGHT = 10;
    private static final int HEADER_LINE_GAP = 2;
    private static final int MIN_CONTENT_WIDTH = BUTTON_WIDTH;
    /** Vanilla's two-column option button width (150), used once options spill into columns. */
    private static final int COLUMN_BUTTON_WIDTH = 150;
    private static final int MAX_COLUMNS = 3;
    private static final int DEFAULT_WAIT_HOURS = 6;
    private static final int MAX_CONTENT_WIDTH = 260;
    private static final int WHITE = 0xFFFFFF;
    private static final int GRAY = 0xAAAAAA;

    private final OpenConversationPayload conversation;
    /** Set when the server ended (or replaced) this conversation, so closing the screen sends nothing back. */
    private boolean closedByServer;

    private final List<Line> lines = new ArrayList<>();
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;

    /** One centered line of text at a fixed offset from the top of the content. */
    private record Line(OrderedText text, int y, int color) {
    }

    public ConversationScreen(OpenConversationPayload conversation) {
        super(conversation.npcName());
        this.conversation = conversation;
    }

    public int npcId() {
        return this.conversation.npcId();
    }

    @Override
    protected void init() {
        Text name = this.conversation.npcName();
        // A companion has no faction; its header is just its name and how it regards you.
        Text faction = this.conversation.factionName() == null ? null : this.conversation.factionName().copy()
                .withColor(readableOnDark(this.conversation.flagColor().getSignColor()));
        Text standing = Text.translatable("disposition." + RPGMinibosses.CONTENT_NAMESPACE + "."
                + this.conversation.standing().name().toLowerCase(Locale.ROOT));
        Text greeting = Text.literal("“").append(this.conversation.greeting()).append("”");
        Text gossip = this.conversation.heardAboutYou();

        int widest = Math.max(this.textRenderer.getWidth(name), faction == null ? 0 : this.textRenderer.getWidth(faction));
        widest = Math.max(widest, this.textRenderer.getWidth(greeting));
        int maxContent = Math.max(MIN_CONTENT_WIDTH, this.width - 2 * PADDING - 8);
        int textWidth = Math.min(Math.max(widest, MIN_CONTENT_WIDTH), Math.min(MAX_CONTENT_WIDTH, maxContent));

        // Header block.
        this.lines.clear();
        int y = 0;
        y = addWrapped(name, textWidth, y, WHITE, HEADER_LINE_GAP);
        if (faction != null) {
            y = addWrapped(faction, textWidth, y, WHITE, HEADER_LINE_GAP);
        }
        y = addWrapped(standing, textWidth, y, GRAY, HEADER_LINE_GAP) - HEADER_LINE_GAP;
        // Greeting block.
        y += BLOCK_GAP;
        y = addWrapped(greeting, textWidth, y, WHITE, 0);
        // Gossip block, once Rumors exist.
        if (gossip != null) {
            y += BLOCK_GAP;
            y = addWrapped(gossip.copy().formatted(Formatting.ITALIC), textWidth, y, GRAY, 0);
        }
        int textHeight = y;

        // Every option stays visible: when one column would run off the screen, use more (up to three).
        int cells = this.conversation.options().size() + 1;
        for (OpenConversationPayload.Entry entry : this.conversation.options()) {
            if (entry.option() == ConversationOption.STAY) {
                cells++; // its slider
            }
        }
        int availableHeight = this.height - 8 - 2 * PADDING - textHeight - BLOCK_GAP;
        int rowsThatFit = Math.max(1, (availableHeight + BUTTON_GAP) / (BUTTON_HEIGHT + BUTTON_GAP));
        int columns = Math.min(MAX_COLUMNS, Math.max(1, (cells + rowsThatFit - 1) / rowsThatFit));
        int cellWidth = Math.min(columns == 1 ? BUTTON_WIDTH : COLUMN_BUTTON_WIDTH,
                (maxContent - (columns - 1) * BUTTON_GAP) / columns);
        int gridWidth = columns * cellWidth + (columns - 1) * BUTTON_GAP;
        int rows = (cells + columns - 1) / columns;
        int gridHeight = rows * BUTTON_HEIGHT + (rows - 1) * BUTTON_GAP;
        int contentWidth = Math.max(textWidth, gridWidth);

        this.panelWidth = contentWidth + 2 * PADDING;
        this.panelHeight = textHeight + BLOCK_GAP + gridHeight + 2 * PADDING;
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = Math.max(4, (this.height - this.panelHeight) / 2);

        int gridLeft = (this.width - gridWidth) / 2;
        int gridTop = this.panelTop + PADDING + textHeight + BLOCK_GAP;
        int[] cell = {0};
        java.util.function.Consumer<net.minecraft.client.gui.widget.ClickableWidget> place = widget -> {
            int column = cell[0] % columns;
            int row = cell[0] / columns;
            widget.setPosition(gridLeft + column * (cellWidth + BUTTON_GAP), gridTop + row * (BUTTON_HEIGHT + BUTTON_GAP));
            widget.setWidth(cellWidth);
            this.addDrawableChild(widget);
            cell[0]++;
        };
        for (OpenConversationPayload.Entry entry : this.conversation.options()) {
            ButtonWidget.Builder builder = ButtonWidget.builder(entry.option().label(), button ->
                            NetworkManager.sendToServer(new ConversationActionPayload(this.npcId(), entry.option(), this.argumentFor(entry.option()))))
                    .dimensions(0, 0, cellWidth, BUTTON_HEIGHT);
            if (entry.reason() != null) {
                builder.tooltip(Tooltip.of(entry.reason()));
            }
            ButtonWidget button = builder.build();
            button.active = entry.enabled();
            place.accept(button);
            if (entry.option() == ConversationOption.STAY) {
                WaitSlider slider = new WaitSlider(cellWidth, this.waitHours, hours -> this.waitHours = hours);
                slider.active = entry.enabled();
                place.accept(slider);
            }
        }
        place.accept(ButtonWidget.builder(
                        Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".goodbye"), button -> this.close())
                .dimensions(0, 0, cellWidth, BUTTON_HEIGHT).build());
    }

    /** The setting an option carries to the server: Wait sends its hours. */
    private int argumentFor(ConversationOption option) {
        return option == ConversationOption.STAY ? this.waitHours : 0;
    }

    /** How long to wait, in Minecraft hours. Kept across re-layouts (resizing the window). */
    private int waitHours = DEFAULT_WAIT_HOURS;

    /** Vanilla's slider, snapped to whole Minecraft hours between Wait's bounds. */
    private static final class WaitSlider extends net.minecraft.client.gui.widget.SliderWidget {
        private static final int SPAN = ConversationOption.WAIT_MAX_HOURS - ConversationOption.WAIT_MIN_HOURS;
        private final java.util.function.IntConsumer onChange;

        WaitSlider(int width, int hours, java.util.function.IntConsumer onChange) {
            super(0, 0, width, BUTTON_HEIGHT, Text.empty(), (double) (hours - ConversationOption.WAIT_MIN_HOURS) / SPAN);
            this.onChange = onChange;
            this.updateMessage();
        }

        private int hours() {
            return ConversationOption.WAIT_MIN_HOURS + (int) Math.round(this.value * SPAN);
        }

        @Override
        protected void updateMessage() {
            int hours = this.hours();
            this.setMessage(Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE
                    + (hours == 1 ? ".wait_hour" : ".wait_hours"), hours));
        }

        @Override
        protected void applyValue() {
            this.value = (double) (this.hours() - ConversationOption.WAIT_MIN_HOURS) / SPAN;
            this.onChange.accept(this.hours());
        }
    }

    /** Wraps {@code text} to the content width, records each line, and returns the y below the last one. */
    private int addWrapped(Text text, int width, int y, int color, int gapAfter) {
        for (OrderedText line : this.textRenderer.wrapLines(text, width)) {
            this.lines.add(new Line(line, y, color));
            y += LINE_HEIGHT;
        }
        return y + gapAfter;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawGuiTexture(PANEL, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int top = this.panelTop + PADDING;
        for (Line line : this.lines) {
            context.drawCenteredTextWithShadow(this.textRenderer, line.text(), centerX, top + line.y(), line.color());
        }
    }

    /** Flag colors as text on the dark panel: dark ones (black, purple, blue) are lifted toward white. */
    private static int readableOnDark(int rgb) {
        int r = ColorHelper.Argb.getRed(rgb);
        int g = ColorHelper.Argb.getGreen(rgb);
        int b = ColorHelper.Argb.getBlue(rgb);
        double luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0;
        if (luminance >= 0.4) {
            return rgb & 0xFFFFFF;
        }
        double lift = 0.5;
        return ColorHelper.Argb.getArgb(0, (int) (r + (255 - r) * lift), (int) (g + (255 - g) * lift),
                (int) (b + (255 - b) * lift)) & 0xFFFFFF;
    }

    /** The server ended this conversation: close without telling it back. */
    public void closeFromServer() {
        this.closedByServer = true;
        this.close();
    }

    /** A new conversation screen is replacing this one; the server already knows. */
    void markSuperseded() {
        this.closedByServer = true;
    }

    @Override
    public void removed() {
        if (!this.closedByServer) {
            NetworkManager.sendToServer(ConversationActionPayload.leave(this.npcId()));
        }
        super.removed();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

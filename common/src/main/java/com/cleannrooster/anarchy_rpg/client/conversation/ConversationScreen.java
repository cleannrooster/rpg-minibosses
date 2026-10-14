package com.cleannrooster.anarchy_rpg.client.conversation;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
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
        Text faction = this.conversation.factionName().copy()
                .withColor(readableOnDark(this.conversation.flagColor().getSignColor()));
        Text standing = Text.translatable("disposition." + RPGMinibosses.CONTENT_NAMESPACE + "."
                + this.conversation.standing().name().toLowerCase(Locale.ROOT));
        Text greeting = Text.literal("“").append(this.conversation.greeting()).append("”");
        Text gossip = this.conversation.heardAboutYou();

        int widest = Math.max(this.textRenderer.getWidth(name), this.textRenderer.getWidth(faction));
        widest = Math.max(widest, this.textRenderer.getWidth(greeting));
        int contentWidth = Math.min(Math.max(widest, MIN_CONTENT_WIDTH), Math.min(MAX_CONTENT_WIDTH, this.width - 2 * PADDING - 8));

        // Header block.
        this.lines.clear();
        int y = 0;
        y = addWrapped(name, contentWidth, y, WHITE, HEADER_LINE_GAP);
        y = addWrapped(faction, contentWidth, y, WHITE, HEADER_LINE_GAP);
        y = addWrapped(standing, contentWidth, y, GRAY, HEADER_LINE_GAP) - HEADER_LINE_GAP;
        // Greeting block.
        y += BLOCK_GAP;
        y = addWrapped(greeting, contentWidth, y, WHITE, 0);
        // Gossip block, once Rumors exist.
        if (gossip != null) {
            y += BLOCK_GAP;
            y = addWrapped(gossip.copy().formatted(Formatting.ITALIC), contentWidth, y, GRAY, 0);
        }
        int textHeight = y;

        int buttons = this.conversation.options().size() + 1;
        int buttonsHeight = buttons * BUTTON_HEIGHT + (buttons - 1) * BUTTON_GAP;
        int contentHeight = textHeight + BLOCK_GAP + buttonsHeight;

        this.panelWidth = contentWidth + 2 * PADDING;
        this.panelHeight = contentHeight + 2 * PADDING;
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = Math.max(4, (this.height - this.panelHeight) / 2);

        int buttonX = (this.width - BUTTON_WIDTH) / 2;
        int buttonY = this.panelTop + PADDING + textHeight + BLOCK_GAP;
        for (OpenConversationPayload.Entry entry : this.conversation.options()) {
            ButtonWidget.Builder builder = ButtonWidget.builder(entry.option().label(), button ->
                            NetworkManager.sendToServer(new ConversationActionPayload(this.npcId(), entry.option())))
                    .dimensions(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT);
            if (entry.reason() != null) {
                builder.tooltip(Tooltip.of(entry.reason()));
            }
            ButtonWidget button = this.addDrawableChild(builder.build());
            button.active = entry.enabled();
            buttonY += BUTTON_HEIGHT + BUTTON_GAP;
        }
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".goodbye"), button -> this.close())
                .dimensions(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT).build());
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

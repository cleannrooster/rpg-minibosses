package com.cleannrooster.anarchy_rpg.client.journal;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.client.conversation.ConversationScreen;
import com.cleannrooster.anarchy_rpg.journal.JournalLog;
import com.cleannrooster.anarchy_rpg.journal.net.JournalActionPayload;
import com.cleannrooster.anarchy_rpg.journal.net.JournalPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Guild Journal, on vanilla's dark {@code popup/background} panel with white text, like the
 * conversation screen. Three tabs: Rumors &amp; Gossip, Companion, Guilds. The page between the tabs and
 * the footer scrolls. Purely a view; the one action (releasing the companion) goes to the server, which
 * answers with a fresh journal.
 */
public final class JournalScreen extends Screen {
    private static final String NS = RPGMinibosses.CONTENT_NAMESPACE;
    private static final Identifier PANEL = Identifier.ofVanilla("popup/background");
    private static final int PADDING = 14;
    private static final int GAP = 6;
    private static final int BLOCK_GAP = 10;
    private static final int BUTTON_HEIGHT = 20;
    private static final int LINE_HEIGHT = 10;
    private static final int MAX_PANEL_WIDTH = 320;
    private static final int MAX_PANEL_HEIGHT = 260;
    private static final int SCROLL_STEP = 12;
    private static final int WHITE = 0xFFFFFF;
    private static final int GRAY = 0xAAAAAA;

    private enum Tab {
        RUMORS,
        COMPANION,
        GUILDS;

        Text label() {
            return Text.translatable("journal." + NS + ".tab." + this.name().toLowerCase(Locale.ROOT));
        }
    }

    /** The tab last looked at, so the journal reopens where it was left. */
    private static Tab lastTab = Tab.RUMORS;

    private JournalPayload journal;
    private Tab tab = lastTab;
    private int scroll;

    private final List<Line> lines = new ArrayList<>();
    private int contentHeight;
    private int panelLeft;
    private int panelTop;
    private int panelWidth;
    private int panelHeight;
    private int pageTop;
    private int pageBottom;

    /** One left-aligned line of the page, at a fixed offset from the top of the page. */
    private record Line(OrderedText text, int x, int y, int color) {
    }

    public JournalScreen(JournalPayload journal) {
        super(Text.translatable("journal." + NS + ".title"));
        this.journal = journal;
    }

    /** A fresh journal from the server (after an action): keep the tab, re-lay the page. */
    void update(JournalPayload journal) {
        this.journal = journal;
        this.clearAndInit();
    }

    @Override
    protected void init() {
        this.panelWidth = Math.min(MAX_PANEL_WIDTH, this.width - 16);
        this.panelHeight = Math.min(MAX_PANEL_HEIGHT, this.height - 16);
        this.panelLeft = (this.width - this.panelWidth) / 2;
        this.panelTop = (this.height - this.panelHeight) / 2;
        int contentLeft = this.panelLeft + PADDING;
        int contentWidth = this.panelWidth - 2 * PADDING;

        // Tabs under the title; the open one is greyed out, as vanilla does for the selected tab.
        int tabsTop = this.panelTop + PADDING + LINE_HEIGHT + GAP;
        Tab[] tabs = Tab.values();
        int tabWidth = (contentWidth - (tabs.length - 1) * GAP) / tabs.length;
        for (int i = 0; i < tabs.length; i++) {
            Tab each = tabs[i];
            ButtonWidget button = ButtonWidget.builder(each.label(), b -> this.select(each))
                    .dimensions(contentLeft + i * (tabWidth + GAP), tabsTop, tabWidth, BUTTON_HEIGHT).build();
            button.active = each != this.tab;
            this.addDrawableChild(button);
        }

        // Footer: Done, plus Release on the companion page.
        int footerTop = this.panelTop + this.panelHeight - PADDING - BUTTON_HEIGHT;
        boolean release = this.tab == Tab.COMPANION && this.journal.companion() != null;
        int footerWidth = release ? (contentWidth - GAP) / 2 : Math.min(150, contentWidth);
        if (release) {
            this.addDrawableChild(ButtonWidget.builder(Text.translatable("journal." + NS + ".release"), b -> this.confirmRelease())
                    .dimensions(contentLeft, footerTop, footerWidth, BUTTON_HEIGHT).build());
            this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> this.close())
                    .dimensions(contentLeft + footerWidth + GAP, footerTop, footerWidth, BUTTON_HEIGHT).build());
        } else {
            this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, b -> this.close())
                    .dimensions((this.width - footerWidth) / 2, footerTop, footerWidth, BUTTON_HEIGHT).build());
        }

        this.pageTop = tabsTop + BUTTON_HEIGHT + BLOCK_GAP;
        this.pageBottom = footerTop - BLOCK_GAP;
        this.layOut(contentWidth);
        this.scroll = MathHelper.clamp(this.scroll, 0, this.maxScroll());
    }

    private void select(Tab tab) {
        this.tab = tab;
        lastTab = tab;
        this.scroll = 0;
        this.clearAndInit();
    }

    private void confirmRelease() {
        JournalPayload.Companion companion = this.journal.companion();
        if (companion == null || this.client == null) {
            return;
        }
        this.client.setScreen(new ConfirmScreen(confirmed -> {
            if (confirmed) {
                NetworkManager.sendToServer(new JournalActionPayload(JournalActionPayload.Action.RELEASE_COMPANION));
            }
            this.client.setScreen(this);
        }, Text.translatable("journal." + NS + ".release.title", companion.name()),
                Text.translatable("journal." + NS + ".release.body", companion.name())));
    }

    // ── Pages ───────────────────────────────────────────────────────────────

    private void layOut(int width) {
        this.lines.clear();
        int y = switch (this.tab) {
            case RUMORS -> this.rumorsPage(width);
            case COMPANION -> this.companionPage(width);
            case GUILDS -> this.guildsPage(width);
        };
        this.contentHeight = Math.max(0, y);
    }

    private int rumorsPage(int width) {
        int y = 0;
        if (this.journal.entries().isEmpty()) {
            return this.add(Text.translatable("journal." + NS + ".rumors.empty"), width, 0, y, GRAY);
        }
        for (JournalPayload.Entry entry : this.journal.entries()) {
            MutableText header = Text.translatable("journal." + NS + ".kind." + entry.kind().name().toLowerCase(Locale.ROOT))
                    .append(" · ").append(this.when(entry.day())).append(" · ").append(entry.teller());
            if (entry.guild() != null) {
                header.append(", ").append(this.guildName(entry.guild()));
            }
            y = this.add(header, width, 0, y, GRAY);
            Text line = entry.kind() == JournalLog.Kind.RUMOR
                    ? Text.literal("“").append(entry.line()).append("”")
                    : entry.line().copy().formatted(Formatting.ITALIC);
            y = this.add(line, width - 6, 6, y, WHITE) + BLOCK_GAP;
        }
        return y - BLOCK_GAP;
    }

    private int companionPage(int width) {
        JournalPayload.Companion companion = this.journal.companion();
        if (companion == null) {
            return this.add(Text.translatable("journal." + NS + ".companion.none"), width, 0, 0, GRAY);
        }
        int y = this.add(companion.name(), width, 0, 0, WHITE);
        y = this.add(companion.status(), width, 0, y, GRAY);
        if (companion.where() != null) {
            y = this.add(companion.where(), width, 0, y, GRAY);
        }
        y += BLOCK_GAP;
        return this.add(Text.translatable("journal." + NS + ".companion.release_hint"), width, 0, y, GRAY);
    }

    private int guildsPage(int width) {
        List<JournalPayload.Guild> guilds = this.journal.guilds();
        int y = this.add(Text.translatable("journal." + NS + ".guilds.standing").formatted(Formatting.UNDERLINE), width, 0, 0, WHITE);
        for (JournalPayload.Guild guild : guilds) {
            Text disposition = Text.translatable("disposition." + NS + "." + guild.disposition().name().toLowerCase(Locale.ROOT));
            y = this.add(Text.translatable("journal." + NS + ".guilds.standing_line", this.guildName(guild), disposition,
                    guild.standing()), width, 6, y, WHITE);
        }
        y += BLOCK_GAP;
        y = this.add(Text.translatable("journal." + NS + ".guilds.between").formatted(Formatting.UNDERLINE), width, 0, y, WHITE);
        for (JournalPayload.Relation relation : this.journal.relations()) {
            if (relation.first() >= guilds.size() || relation.second() >= guilds.size()) {
                continue;
            }
            Text kind = Text.translatable("relation." + NS + "." + relation.relation().name().toLowerCase(Locale.ROOT));
            y = this.add(Text.translatable("journal." + NS + ".guilds.relation_line", this.guildName(guilds.get(relation.first())),
                    this.guildName(guilds.get(relation.second())), kind), width, 6, y, WHITE);
        }
        return y;
    }

    private Text guildName(JournalPayload.Guild guild) {
        return guild.name().copy().withColor(ConversationScreen.readableOnDark(guild.flagColor().getSignColor()));
    }

    private Text when(long day) {
        long ago = this.journal.today() - day;
        if (ago <= 0) {
            return Text.translatable("journal." + NS + ".today");
        }
        if (ago == 1) {
            return Text.translatable("journal." + NS + ".yesterday");
        }
        return Text.translatable("journal." + NS + ".days_ago", ago);
    }

    /** Wraps {@code text} to {@code width}, records each line, and returns the y below the last one. */
    private int add(Text text, int width, int x, int y, int color) {
        for (OrderedText line : this.textRenderer.wrapLines(text, Math.max(20, width))) {
            this.lines.add(new Line(line, x, y, color));
            y += LINE_HEIGHT;
        }
        return y;
    }

    // ── Drawing and input ───────────────────────────────────────────────────

    private int maxScroll() {
        return Math.max(0, this.contentHeight - (this.pageBottom - this.pageTop));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        this.scroll = MathHelper.clamp(this.scroll - (int) Math.round(verticalAmount * SCROLL_STEP), 0, this.maxScroll());
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (JournalClient.OPEN.matchesKey(keyCode, scanCode)) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        super.renderBackground(context, mouseX, mouseY, delta);
        context.drawGuiTexture(PANEL, this.panelLeft, this.panelTop, this.panelWidth, this.panelHeight);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, this.panelTop + PADDING, WHITE);

        int left = this.panelLeft + PADDING;
        int right = this.panelLeft + this.panelWidth - PADDING;
        context.enableScissor(left, this.pageTop, right + 4, this.pageBottom);
        for (Line line : this.lines) {
            int y = this.pageTop + line.y() - this.scroll;
            if (y + LINE_HEIGHT < this.pageTop || y > this.pageBottom) {
                continue;
            }
            context.drawTextWithShadow(this.textRenderer, line.text(), left + line.x(), y, line.color());
        }
        context.disableScissor();

        // A thin scrollbar at the right edge when the page runs past the panel.
        int max = this.maxScroll();
        if (max > 0) {
            int track = this.pageBottom - this.pageTop;
            int thumb = Math.max(12, track * track / (track + max));
            int thumbTop = this.pageTop + (track - thumb) * this.scroll / max;
            context.fill(right + 2, this.pageTop, right + 4, this.pageBottom, 0x40FFFFFF);
            context.fill(right + 2, thumbTop, right + 4, thumbTop + thumb, 0xC0FFFFFF);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}

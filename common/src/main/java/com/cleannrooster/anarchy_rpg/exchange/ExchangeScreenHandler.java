package com.cleannrooster.anarchy_rpg.exchange;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
import com.cleannrooster.anarchy_rpg.faction.FactionState;
import com.cleannrooster.anarchy_rpg.faction.Factions;
import com.cleannrooster.anarchy_rpg.faction.StandingCause;
import com.cleannrooster.anarchy_rpg.faction.StandingLedger;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The exchange window: the adventurer's stock (left), what the player asks for (middle), and what the
 * player offers (right), over the player's inventory.
 *
 * <p>Invariants, all enforced here on the server:
 * <ul>
 *   <li>Stock and request items always belong to the adventurer. Those slots can never be picked up or
 *   inserted into; clicking one only moves its stack to the other pane. A disconnect, a closed window or
 *   the adventurer dying returns the request to stock.</li>
 *   <li>The quote is computed only here and mirrored to the client for display.</li>
 *   <li>A trade is all or nothing: the request goes to the player and the offer is destroyed, or nothing
 *   changes.</li>
 * </ul>
 */
public final class ExchangeScreenHandler extends ScreenHandler {
    public static final int TRADE_BUTTON = 0;
    private static final double MAX_DISTANCE = 8.0;

    private static final int STOCK_START = 0;
    private static final int REQUEST_START = STOCK_START + ExchangeStock.SIZE;
    private static final int OFFER_START = REQUEST_START + ExchangeStock.SIZE;
    private static final int PLAYER_START = OFFER_START + ExchangeStock.SIZE;
    private static final int PLAYER_END = PLAYER_START + 36;

    /** Quote values in hundredths of a currency item, each split into two 16-bit properties. */
    private final PropertyDelegate quote;

    private final int npcId;
    /** Server only. */
    @Nullable
    private final MinibossEntity npc;
    private final Inventory stock;
    private final SimpleInventory request = new SimpleInventory(ExchangeStock.SIZE);
    private final SimpleInventory offer = new SimpleInventory(ExchangeStock.SIZE);

    /** Client side: the stock arrives through slot syncing. */
    public static ExchangeScreenHandler client(int syncId, PlayerInventory playerInventory, int npcId) {
        return new ExchangeScreenHandler(syncId, playerInventory, npcId, null, new SimpleInventory(ExchangeStock.SIZE));
    }

    /** Server side. */
    public static ExchangeScreenHandler server(int syncId, PlayerInventory playerInventory, MinibossEntity npc) {
        return new ExchangeScreenHandler(syncId, playerInventory, npc.getId(), npc, npc.getExchangeStock().inventory());
    }

    private ExchangeScreenHandler(int syncId, PlayerInventory playerInventory, int npcId, @Nullable MinibossEntity npc, Inventory stock) {
        super(ExchangeMenus.EXCHANGE, syncId);
        this.npcId = npcId;
        this.npc = npc;
        this.stock = stock;
        this.quote = new ArrayPropertyDelegate(4);
        this.addProperties(this.quote);

        addGrid(stock, STOCK_START, ExchangeLayout.STOCK_X, true);
        addGrid(this.request, REQUEST_START, ExchangeLayout.REQUEST_X, true);
        addGrid(this.offer, OFFER_START, ExchangeLayout.OFFER_X, false);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9,
                        ExchangeLayout.INVENTORY_X + col * ExchangeLayout.SLOT, ExchangeLayout.INVENTORY_Y + row * ExchangeLayout.SLOT));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, ExchangeLayout.INVENTORY_X + col * ExchangeLayout.SLOT, ExchangeLayout.HOTBAR_Y));
        }
        this.offer.addListener(inventory -> this.requote());
        this.request.addListener(inventory -> this.requote());
    }

    private void addGrid(Inventory inventory, int first, int x, boolean locked) {
        for (int row = 0; row < ExchangeLayout.GRID; row++) {
            for (int col = 0; col < ExchangeLayout.GRID; col++) {
                int index = col + row * ExchangeLayout.GRID;
                int slotX = x + col * ExchangeLayout.SLOT;
                int slotY = ExchangeLayout.PANES_Y + row * ExchangeLayout.SLOT;
                this.addSlot(locked ? new LockedSlot(inventory, index, slotX, slotY) : new Slot(inventory, index, slotX, slotY));
            }
        }
    }

    /** A stock or request slot: never picked up or filled directly. */
    private static final class LockedSlot extends Slot {
        LockedSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            return false;
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return false;
        }
    }

    public int npcId() {
        return this.npcId;
    }

    // ── Quote ──────────────────────────────────────────────────────────────

    private void requote() {
        if (this.npc == null || !(this.npc.getWorld() instanceof net.minecraft.server.world.ServerWorld)) {
            return;
        }
        ExchangePricing.Quote current = this.currentQuote();
        writeHundredths(0, current.cost());
        writeHundredths(2, current.offer());
    }

    private ExchangePricing.Quote currentQuote() {
        PlayerEntity customer = this.npc.getCustomer();
        int standing = 0;
        boolean owner = false;
        if (customer instanceof ServerPlayerEntity player) {
            owner = player.getUuid().equals(this.npc.getOwnerUuid());
            var faction = Factions.factionOf(this.npc);
            standing = faction == null ? 0 : StandingLedger.get(player.getServer()).get(player.getUuid(), faction);
        }
        return ExchangePricing.quote(stacks(this.request), stacks(this.offer), standing, owner);
    }

    private void writeHundredths(int index, double value) {
        int hundredths = (int) Math.min(Integer.MAX_VALUE, Math.round(value * 100.0));
        this.quote.set(index, (hundredths >>> 16) & 0xFFFF);
        this.quote.set(index + 1, hundredths & 0xFFFF);
    }

    private double readHundredths(int index) {
        return (((this.quote.get(index) & 0xFFFF) << 16) | (this.quote.get(index + 1) & 0xFFFF)) / 100.0;
    }

    /** The mirrored quote, for display. */
    public ExchangePricing.Quote displayedQuote() {
        return new ExchangePricing.Quote(this.readHundredths(0), this.readHundredths(2));
    }

    private static List<ItemStack> stacks(Inventory inventory) {
        List<ItemStack> stacks = new ArrayList<>(inventory.size());
        for (int i = 0; i < inventory.size(); i++) {
            stacks.add(inventory.getStack(i));
        }
        return stacks;
    }

    // ── Clicks ─────────────────────────────────────────────────────────────

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= STOCK_START && slotIndex < OFFER_START) {
            // Stock and request only ever trade places; nothing in them reaches the cursor or the inventory.
            if (actionType == SlotActionType.PICKUP || actionType == SlotActionType.QUICK_MOVE) {
                boolean fromStock = slotIndex < REQUEST_START;
                Slot from = this.slots.get(slotIndex);
                if (from.hasStack()) {
                    moveBetweenPanes(from, fromStock ? this.request : this.stock);
                }
            }
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    private void moveBetweenPanes(Slot from, Inventory to) {
        ItemStack moving = from.getStack();
        for (int i = 0; i < to.size(); i++) {
            if (to.getStack(i).isEmpty()) {
                to.setStack(i, moving);
                from.inventory.setStack(from.getIndex(), ItemStack.EMPTY);
                to.markDirty();
                from.inventory.markDirty();
                this.sendContentUpdates();
                return;
            }
        }
    }

    /** Shift-click moves between the player's inventory and the offer pane only. */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        if (slotIndex >= OFFER_START && slotIndex < PLAYER_START) {
            if (!this.insertItem(stack, PLAYER_START, PLAYER_END, true)) {
                return ItemStack.EMPTY;
            }
        } else if (slotIndex >= PLAYER_START) {
            if (!this.insertItem(stack, OFFER_START, PLAYER_START, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return original;
    }

    // ── Trading ────────────────────────────────────────────────────────────

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != TRADE_BUTTON || this.npc == null || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        ExchangePricing.Quote current = this.currentQuote();
        if (!current.acceptable()) {
            return false;
        }
        for (int i = 0; i < this.request.size(); i++) {
            ItemStack bought = this.request.removeStack(i);
            if (!bought.isEmpty()) {
                player.getInventory().offerOrDrop(bought);
            }
        }
        // Offered items are destroyed: Exchange is an item sink, so nothing sold can be bought back.
        this.offer.clear();
        Factions.recordDeed(this.npc, serverPlayer, StandingCause.TRADED, RPGMinibossesEntities.config.standingTrade);
        this.npc.getWorld().playSound(null, this.npc.getBlockPos(), SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.NEUTRAL, 0.8F, 1.0F);
        this.requote();
        this.sendContentUpdates();
        return true;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.npc == null) {
            return true;
        }
        return this.npc.isAlive() && !this.npc.isRemoved() && !this.npc.isIncapacitated()
                && this.npc.getCustomer() == player
                && player.squaredDistanceTo(this.npc) <= MAX_DISTANCE * MAX_DISTANCE
                && !(player instanceof ServerPlayerEntity serverPlayer && Factions.disposition(this.npc, serverPlayer) == Disposition.HOSTILE);
    }

    /** Request back to stock, offer back to the player, and the adventurer is free again. */
    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (this.npc == null) {
            return;
        }
        for (int i = 0; i < this.request.size(); i++) {
            ItemStack asked = this.request.removeStack(i);
            if (!asked.isEmpty()) {
                ItemStack left = ((SimpleInventory) this.stock).addStack(asked);
                if (!left.isEmpty()) {
                    // Cannot happen (the request only ever came out of stock), but never lose an item.
                    player.getInventory().offerOrDrop(left);
                }
            }
        }
        this.dropInventory(player, this.offer);
        if (this.npc.getCustomer() == player) {
            this.npc.setCustomer(null);
        }
    }
}

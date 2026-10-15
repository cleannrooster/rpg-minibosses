package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.exchange.ItemValues;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ArrayPropertyDelegate;
import net.minecraft.screen.PropertyDelegate;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Paying an adventurer for a service (hiring it, and the camp services): a 3x3 offer grid over the player's
 * inventory, laid out like vanilla's dispenser, with the price and the offer's value beside it.
 *
 * <p>Paying is all or nothing, decided on the server: the offer must cover the service's price and the service's
 * own rules must still hold ({@link PaidService#whyNot}). The payment is destroyed, as in Exchange. Closing
 * without paying returns the offer.
 */
public final class PaidServiceScreenHandler extends ScreenHandler {
    public static final int CONFIRM_BUTTON = 0;
    private static final double MAX_DISTANCE = 8.0;
    private static final int OFFER_SLOTS = 9;
    private static final int PLAYER_END = OFFER_SLOTS + 36;

    private final int npcId;
    private final PaidServiceKind kind;
    @Nullable
    private final MinibossEntity npc;
    private final SimpleInventory offer = new SimpleInventory(OFFER_SLOTS);
    /** Price and offer value in hundredths, each split over two 16-bit properties. */
    private final PropertyDelegate quote = new ArrayPropertyDelegate(4);

    public static PaidServiceScreenHandler client(int syncId, PlayerInventory playerInventory, int npcId, PaidServiceKind kind) {
        return new PaidServiceScreenHandler(syncId, playerInventory, npcId, kind, null);
    }

    public static PaidServiceScreenHandler server(int syncId, PlayerInventory playerInventory, MinibossEntity npc, PaidServiceKind kind) {
        return new PaidServiceScreenHandler(syncId, playerInventory, npc.getId(), kind, npc);
    }

    private PaidServiceScreenHandler(int syncId, PlayerInventory playerInventory, int npcId, PaidServiceKind kind,
                                     @Nullable MinibossEntity npc) {
        super(PaidServiceMenus.TYPE, syncId);
        this.npcId = npcId;
        this.kind = kind;
        this.npc = npc;
        this.addProperties(this.quote);
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(this.offer, col + row * 3, 62 + col * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
        this.offer.addListener(inventory -> this.requote());
        this.requote();
    }

    public int npcId() {
        return this.npcId;
    }

    public PaidServiceKind kind() {
        return this.kind;
    }

    private void requote() {
        if (this.npc == null || !(this.npc.getCustomer() instanceof ServerPlayerEntity player)) {
            return;
        }
        write(0, this.kind.service().price(this.npc, player));
        write(2, ItemValues.total(this.offered()));
    }

    private List<ItemStack> offered() {
        List<ItemStack> stacks = new ArrayList<>(OFFER_SLOTS);
        for (int i = 0; i < OFFER_SLOTS; i++) {
            stacks.add(this.offer.getStack(i));
        }
        return stacks;
    }

    private void write(int index, double value) {
        int hundredths = (int) Math.min(Integer.MAX_VALUE, Math.round(value * 100.0));
        this.quote.set(index, (hundredths >>> 16) & 0xFFFF);
        this.quote.set(index + 1, hundredths & 0xFFFF);
    }

    private double read(int index) {
        return (((this.quote.get(index) & 0xFFFF) << 16) | (this.quote.get(index + 1) & 0xFFFF)) / 100.0;
    }

    public double price() {
        return this.read(0);
    }

    public double offerValue() {
        return this.read(2);
    }

    public boolean affordable() {
        return this.price() > 0.0 && this.offerValue() + 1.0E-6 >= this.price();
    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        if (id != CONFIRM_BUTTON || this.npc == null || !(player instanceof ServerPlayerEntity serverPlayer)) {
            return false;
        }
        PaidService service = this.kind.service();
        double price = service.price(this.npc, serverPlayer);
        if (ItemValues.total(this.offered()) + 1.0E-6 < price || service.whyNot(this.npc, serverPlayer) != null) {
            return false;
        }
        if (!service.perform(this.npc, serverPlayer)) {
            return false;
        }
        this.offer.clear();
        this.npc.getWorld().playSound(null, this.npc.getBlockPos(), SoundEvents.ENTITY_VILLAGER_TRADE, SoundCategory.NEUTRAL, 0.8F, 1.0F);
        if (service.closesAfter()) {
            serverPlayer.closeHandledScreen();
        } else {
            this.requote();
            this.sendContentUpdates();
        }
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean moved = slotIndex < OFFER_SLOTS
                ? this.insertItem(stack, OFFER_SLOTS, PLAYER_END, true)
                : this.insertItem(stack, 0, OFFER_SLOTS, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setStack(ItemStack.EMPTY);
        } else {
            slot.markDirty();
        }
        return original;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (this.npc == null) {
            return true;
        }
        return this.npc.isAlive() && !this.npc.isRemoved() && !this.npc.isIncapacitated()
                && this.npc.getCustomer() == player
                && player.squaredDistanceTo(this.npc) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        this.dropInventory(player, this.offer);
        if (this.npc != null && this.npc.getCustomer() == player) {
            this.npc.setCustomer(null);
        }
    }
}

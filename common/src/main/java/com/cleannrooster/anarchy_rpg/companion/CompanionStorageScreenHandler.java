package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.exchange.ExchangeStock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import org.jetbrains.annotations.Nullable;

/**
 * A companion's storage: its nine slots, freely moved to and from the owner's inventory. No trading, no
 * prices. Laid out exactly like vanilla's dispenser (3x3 grid over the player inventory), so the screen can
 * draw the dispenser texture as is.
 */
public final class CompanionStorageScreenHandler extends ScreenHandler {
    private static final double MAX_DISTANCE = 8.0;
    private static final int STORAGE_END = ExchangeStock.SIZE;
    private static final int PLAYER_END = STORAGE_END + 36;

    private final int npcId;
    /** Server only. */
    @Nullable
    private final MinibossEntity npc;

    public static CompanionStorageScreenHandler client(int syncId, PlayerInventory playerInventory, int npcId) {
        return new CompanionStorageScreenHandler(syncId, playerInventory, npcId, null, new SimpleInventory(ExchangeStock.SIZE));
    }

    public static CompanionStorageScreenHandler server(int syncId, PlayerInventory playerInventory, MinibossEntity npc) {
        return new CompanionStorageScreenHandler(syncId, playerInventory, npc.getId(), npc, npc.getExchangeStock().inventory());
    }

    private CompanionStorageScreenHandler(int syncId, PlayerInventory playerInventory, int npcId, @Nullable MinibossEntity npc, Inventory storage) {
        super(CompanionMenus.STORAGE, syncId);
        this.npcId = npcId;
        this.npc = npc;
        // The dispenser's own slot positions.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                this.addSlot(new Slot(storage, col + row * 3, 62 + col * 18, 17 + row * 18));
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
    }

    public int npcId() {
        return this.npcId;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getStack();
        ItemStack original = stack.copy();
        boolean moved = slotIndex < STORAGE_END
                ? this.insertItem(stack, STORAGE_END, PLAYER_END, true)
                : this.insertItem(stack, 0, STORAGE_END, false);
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
        return this.npc.isAlive() && !this.npc.isRemoved()
                && player.getUuid().equals(this.npc.getOwnerUuid())
                && this.npc.getCustomer() == player
                && player.squaredDistanceTo(this.npc) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        if (this.npc != null && this.npc.getCustomer() == player) {
            this.npc.setCustomer(null);
        }
    }
}

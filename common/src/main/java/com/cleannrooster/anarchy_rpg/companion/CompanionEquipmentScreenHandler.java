package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.mojang.datafixers.util.Pair;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/**
 * A companion's equipment: four armor slots, main hand and off hand, over the owner's inventory. Laid out
 * like vanilla's inventory screen (armor column at the left, the companion drawn beside it).
 *
 * <p>The six slots are the companion's real equipment, so vanilla applies each item's attribute modifiers
 * the moment it goes on; attributes the companion does not have simply do nothing. Rules
 * ({@link CompanionGear}): weapons only if the companion's fighting style can use them, armor only in its
 * own slot, and bound gear (what it was recruited with) can be replaced but never taken out.
 */
public final class CompanionEquipmentScreenHandler extends ScreenHandler {
    private static final double MAX_DISTANCE = 8.0;
    public static final int GEAR_SLOTS = CompanionGear.SLOTS.length;
    private static final int PLAYER_START = GEAR_SLOTS;
    private static final int PLAYER_END = PLAYER_START + 36;

    /** Slot positions (item positions), shared with the screen. */
    public static final int ARMOR_X = 8;
    public static final int ARMOR_Y = 8;
    public static final int HANDS_X = 80;
    public static final int MAINHAND_Y = 44;
    public static final int OFFHAND_Y = 62;

    private static final Identifier EMPTY_MAINHAND = Identifier.ofVanilla("item/empty_slot_sword");
    private static final Identifier[] EMPTY_ARMOR = {
            PlayerScreenHandler.EMPTY_HELMET_SLOT_TEXTURE, PlayerScreenHandler.EMPTY_CHESTPLATE_SLOT_TEXTURE,
            PlayerScreenHandler.EMPTY_LEGGINGS_SLOT_TEXTURE, PlayerScreenHandler.EMPTY_BOOTS_SLOT_TEXTURE};

    private final int npcId;
    /** The companion on either side: the server's own, or the client's copy looked up by id. */
    private final Supplier<MinibossEntity> npc;
    private final boolean server;

    public static CompanionEquipmentScreenHandler client(int syncId, PlayerInventory playerInventory, int npcId) {
        Supplier<MinibossEntity> lookup = () -> playerInventory.player.getWorld().getEntityById(npcId) instanceof MinibossEntity found ? found : null;
        return new CompanionEquipmentScreenHandler(syncId, playerInventory, npcId, lookup, new SimpleInventory(GEAR_SLOTS), false);
    }

    public static CompanionEquipmentScreenHandler server(int syncId, PlayerInventory playerInventory, MinibossEntity npc) {
        return new CompanionEquipmentScreenHandler(syncId, playerInventory, npc.getId(), () -> npc, new Equipment(npc), true);
    }

    private CompanionEquipmentScreenHandler(int syncId, PlayerInventory playerInventory, int npcId,
                                            Supplier<MinibossEntity> npc, Inventory gear, boolean server) {
        super(CompanionMenus.EQUIPMENT, syncId);
        this.npcId = npcId;
        this.npc = npc;
        this.server = server;
        for (int i = 0; i < 4; i++) {
            this.addSlot(new GearSlot(gear, i, ARMOR_X, ARMOR_Y + i * 18, CompanionGear.SLOTS[i], EMPTY_ARMOR[i]));
        }
        this.addSlot(new GearSlot(gear, 4, HANDS_X, MAINHAND_Y, EquipmentSlot.MAINHAND, EMPTY_MAINHAND));
        this.addSlot(new GearSlot(gear, 5, HANDS_X, OFFHAND_Y, EquipmentSlot.OFFHAND, PlayerScreenHandler.EMPTY_OFFHAND_ARMOR_SLOT));
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

    public @Nullable MinibossEntity companion() {
        return this.npc.get();
    }

    /** The companion's six equipment slots as an inventory, writing straight through to the entity. */
    private static final class Equipment implements Inventory {
        private final MinibossEntity npc;

        Equipment(MinibossEntity npc) {
            this.npc = npc;
        }

        @Override public int size() { return GEAR_SLOTS; }
        @Override public boolean isEmpty() {
            for (EquipmentSlot slot : CompanionGear.SLOTS) {
                if (!this.npc.getEquippedStack(slot).isEmpty()) return false;
            }
            return true;
        }
        @Override public ItemStack getStack(int slot) { return this.npc.getEquippedStack(CompanionGear.SLOTS[slot]); }
        @Override public ItemStack removeStack(int slot, int amount) {
            ItemStack stack = this.getStack(slot);
            ItemStack taken = stack.split(amount);
            this.setStack(slot, stack);
            return taken;
        }
        @Override public ItemStack removeStack(int slot) {
            ItemStack stack = this.getStack(slot);
            this.setStack(slot, ItemStack.EMPTY);
            return stack;
        }
        @Override public void setStack(int slot, ItemStack stack) { this.npc.equipStack(CompanionGear.SLOTS[slot], stack); }
        @Override public void markDirty() { }
        @Override public boolean canPlayerUse(PlayerEntity player) { return true; }
        @Override public void clear() {
            for (EquipmentSlot slot : CompanionGear.SLOTS) this.npc.equipStack(slot, ItemStack.EMPTY);
        }
    }

    private final class GearSlot extends Slot {
        private final EquipmentSlot type;
        private final Identifier emptyIcon;

        GearSlot(Inventory inventory, int index, int x, int y, EquipmentSlot type, Identifier emptyIcon) {
            super(inventory, index, x, y);
            this.type = type;
            this.emptyIcon = emptyIcon;
        }

        EquipmentSlot type() {
            return this.type;
        }

        @Override
        public boolean canInsert(ItemStack stack) {
            MinibossEntity companion = CompanionEquipmentScreenHandler.this.npc.get();
            return companion != null && CompanionGear.accepts(companion, this.type, stack);
        }

        @Override
        public boolean canTakeItems(PlayerEntity player) {
            return !CompanionGear.isBound(this.getStack());
        }

        @Override
        public int getMaxItemCount() {
            return this.type == EquipmentSlot.OFFHAND ? super.getMaxItemCount() : 1;
        }

        @Override
        public Pair<Identifier, Identifier> getBackgroundSprite() {
            return Pair.of(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE, this.emptyIcon);
        }
    }

    // ── Clicks ─────────────────────────────────────────────────────────────

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (slotIndex >= 0 && slotIndex < GEAR_SLOTS) {
            GearSlot slot = (GearSlot) this.slots.get(slotIndex);
            if (actionType == SlotActionType.PICKUP) {
                this.clickGear(slot, player);
            } else if (actionType == SlotActionType.QUICK_MOVE && slot.hasStack() && slot.canTakeItems(player)) {
                ItemStack stack = slot.getStack().copy();
                if (this.insertItem(stack, PLAYER_START, PLAYER_END, true)) {
                    slot.setStack(stack);
                }
            }
            // Hotbar swaps, throws and clones on gear slots are not allowed: they would bypass the rules.
            return;
        }
        super.onSlotClick(slotIndex, button, actionType, player);
    }

    /** Taking, placing, replacing bound gear, or swapping the owner's own gear. */
    private void clickGear(GearSlot slot, PlayerEntity player) {
        ItemStack cursor = this.getCursorStack();
        ItemStack current = slot.getStack();
        if (cursor.isEmpty()) {
            if (!current.isEmpty() && slot.canTakeItems(player)) {
                this.setCursorStack(current.copy());
                slot.setStack(ItemStack.EMPTY);
            }
            return;
        }
        if (!slot.canInsert(cursor)) {
            return;
        }
        int count = Math.min(cursor.getCount(), slot.getMaxItemCount());
        ItemStack placed = cursor.split(count);
        if (!current.isEmpty() && !CompanionGear.isBound(current)) {
            // The owner's own gear comes back to them; bound gear is simply replaced and gone.
            if (cursor.isEmpty()) {
                this.setCursorStack(current.copy());
            } else {
                player.getInventory().offerOrDrop(current.copy());
            }
        }
        slot.setStack(placed);
    }

    /** Shift-click from the inventory: armor to its slot, a usable weapon to the main hand, if free or bound. */
    @Override
    public ItemStack quickMove(PlayerEntity player, int slotIndex) {
        if (slotIndex < PLAYER_START) {
            return ItemStack.EMPTY;
        }
        Slot from = this.slots.get(slotIndex);
        if (!from.hasStack()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = from.getStack();
        for (int i = 0; i < 5; i++) {
            GearSlot target = (GearSlot) this.slots.get(i);
            boolean open = !target.hasStack() || CompanionGear.isBound(target.getStack());
            if (open && target.canInsert(stack)) {
                target.setStack(stack.split(1));
                from.markDirty();
                return ItemStack.EMPTY;
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (!this.server) {
            return true;
        }
        MinibossEntity companion = this.npc.get();
        return companion != null && companion.isAlive() && !companion.isRemoved()
                && player.getUuid().equals(companion.getOwnerUuid())
                && companion.getCustomer() == player
                && player.squaredDistanceTo(companion) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        MinibossEntity companion = this.npc.get();
        if (this.server && companion != null && companion.getCustomer() == player) {
            companion.setCustomer(null);
        }
    }
}

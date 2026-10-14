package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;

/**
 * What a companion may wear and wield.
 *
 * <ul>
 *   <li>Main hand: only weapons its fighting style can use, from the item tag
 *   {@code anarchy_rpg:companion_weapons/<adventurer id>} (a Mercenary needs a crossbow, a Templar a sword
 *   or claymore...). Data, so packs can widen it.</li>
 *   <li>Armor slots: armor for that slot, as for a player. Armor is never drawn on adventurers; it only
 *   gives its stats.</li>
 *   <li>Off hand: anything.</li>
 * </ul>
 *
 * <p>Gear a companion already had when recruited is <em>bound</em>: it can be replaced (and is then gone)
 * but never taken out, so recruiting is not a way to loot an adventurer's weapon.
 */
public final class CompanionGear {
    private static final String BOUND_KEY = RPGMinibosses.CONTENT_NAMESPACE + ":bound";

    public static final EquipmentSlot[] SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET,
            EquipmentSlot.MAINHAND, EquipmentSlot.OFFHAND};

    private CompanionGear() {
    }

    public static TagKey<Item> weaponsFor(MinibossEntity npc) {
        return TagKey.of(RegistryKeys.ITEM, RPGMinibosses.id("companion_weapons/" + Registries.ENTITY_TYPE.getId(npc.getType()).getPath()));
    }

    /** Whether {@code stack} may go in {@code slot} on this companion. Empty always may. */
    public static boolean accepts(MinibossEntity npc, EquipmentSlot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        return switch (slot) {
            case MAINHAND -> stack.isIn(weaponsFor(npc));
            case OFFHAND -> true;
            case HEAD, CHEST, LEGS, FEET -> npc.getPreferredEquipmentSlot(stack) == slot;
            default -> false;
        };
    }

    public static boolean isBound(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null && data.copyNbt().getBoolean(BOUND_KEY);
    }

    private static void bind(ItemStack stack) {
        NbtComponent.set(DataComponentTypes.CUSTOM_DATA, stack, nbt -> nbt.putBoolean(BOUND_KEY, true));
    }

    /** On recruitment: whatever the adventurer already wears and holds becomes bound to it. */
    static void bindExisting(MinibossEntity npc) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = npc.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                bind(stack);
            }
        }
    }

    /** On dismissal: gear the owner gave goes back to them; bound gear leaves with the companion. */
    static void returnGear(MinibossEntity npc, PlayerEntity owner) {
        for (EquipmentSlot slot : SLOTS) {
            ItemStack stack = npc.getEquippedStack(slot);
            if (!stack.isEmpty() && !isBound(stack)) {
                npc.equipStack(slot, ItemStack.EMPTY);
                owner.getInventory().offerOrDrop(stack);
            }
        }
    }
}

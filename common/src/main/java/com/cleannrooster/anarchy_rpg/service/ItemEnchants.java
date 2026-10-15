package com.cleannrooster.anarchy_rpg.service;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;

import java.util.ArrayList;
import java.util.List;

/** Reading and changing an item's enchantments, for the camp services that work on the held item. */
final class ItemEnchants {

    private ItemEnchants() {
    }

    /** An enchantment on an item, with its level. */
    record Entry(RegistryEntry<Enchantment> enchantment, int level) {
    }

    /** The item's enchantments: stored ones for an enchanted book, applied ones otherwise. */
    static ItemEnchantmentsComponent of(ItemStack stack) {
        return stack.getOrDefault(stack.isOf(Items.ENCHANTED_BOOK) ? DataComponentTypes.STORED_ENCHANTMENTS : DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT);
    }

    /** Its curses ({@code curses} true) or everything else. */
    static List<Entry> list(ItemStack stack, boolean curses) {
        List<Entry> entries = new ArrayList<>();
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : of(stack).getEnchantmentEntries()) {
            if (entry.getKey().isIn(EnchantmentTags.CURSE) == curses) {
                entries.add(new Entry(entry.getKey(), entry.getIntValue()));
            }
        }
        return entries;
    }

    /** Takes one enchantment off; an enchanted book left with none is returned as a plain book. */
    static ItemStack remove(ItemStack stack, RegistryEntry<Enchantment> enchantment) {
        EnchantmentHelper.apply(stack, builder -> builder.remove(entry -> entry.equals(enchantment)));
        if (stack.isOf(Items.ENCHANTED_BOOK) && of(stack).isEmpty()) {
            return new ItemStack(Items.BOOK, stack.getCount());
        }
        return stack;
    }

    /** An enchanted book holding exactly this one enchantment. */
    static ItemStack book(Entry entry) {
        ItemEnchantmentsComponent.Builder one = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
        one.add(entry.enchantment(), entry.level());
        ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
        book.set(DataComponentTypes.STORED_ENCHANTMENTS, one.build());
        return book;
    }
}

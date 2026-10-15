package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;

/**
 * Fire Mage, "Enchant an item" (camp service): for a hefty price and some lapis, the item in the player's main
 * hand is enchanted at random, as an enchanting table's level-30 option would: the same pool of enchantments,
 * and no choice of which. Only items a table could enchant (unenchanted and enchantable; a book becomes an
 * enchanted book). From a stack, one is enchanted and handed back.
 */
final class EnchantItemService extends CampService {
    /** The enchanting table's top option. */
    private static final int LEVEL = 30;

    @Override
    protected PaidServiceKind kind() {
        return PaidServiceKind.ENCHANT_ITEM;
    }

    @Override
    protected double basePrice(ServerConfig config) {
        return config.campEnchantItemPrice;
    }

    private static int lapisCost() {
        return Math.max(0, RPGMinibossesEntities.config.campEnchantItemLapis);
    }

    /** What an enchanting table would take: enchantable, not already enchanted, and with some enchantability. */
    private static boolean enchantable(ItemStack stack) {
        return !stack.isEmpty() && stack.isEnchantable() && stack.getItem().getEnchantability() > 0;
    }

    @Override
    protected @Nullable Text ownWhyNot(MinibossEntity npc, ServerPlayerEntity player) {
        if (!enchantable(player.getMainHandStack())) {
            return CampServices.line("enchant.need_item");
        }
        int lapis = lapisCost();
        if (lapis > 0 && player.getInventory().count(Items.LAPIS_LAZULI) < lapis) {
            return CampServices.line("enchant.need_lapis", lapis);
        }
        return null;
    }

    @Override
    protected boolean deliver(MinibossEntity npc, ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        int lapis = lapisCost();
        if (!enchantable(held) || player.getInventory().count(Items.LAPIS_LAZULI) < lapis) {
            return false;
        }
        var enchantments = npc.getWorld().getRegistryManager().get(RegistryKeys.ENCHANTMENT)
                .getEntryList(EnchantmentTags.IN_ENCHANTING_TABLE);
        ItemStack enchanted = EnchantmentHelper.enchant(npc.getRandom(), held.copyWithCount(1), LEVEL,
                npc.getWorld().getRegistryManager(), enchantments);
        if (!EnchantmentHelper.hasEnchantments(enchanted)) {
            return false; // Nothing the table could put on it after all: nothing is taken.
        }
        if (lapis > 0) {
            player.getInventory().remove(stack -> stack.isOf(Items.LAPIS_LAZULI), lapis,
                    player.playerScreenHandler.getCraftingInput());
        }
        Text itemName = held.getName();
        if (held.getCount() > 1) {
            held.decrement(1);
            player.getInventory().offerOrDrop(enchanted);
        } else {
            player.setStackInHand(Hand.MAIN_HAND, enchanted);
        }
        npc.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_ENCHANTMENT_TABLE_USE, SoundCategory.NEUTRAL, 1.0F, 1.0F);
        npc.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 0.5F, 1.4F);
        if (npc.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.ENCHANT, player.getX(), player.getBodyY(0.8), player.getZ(), 40, 0.4, 0.5, 0.4, 0.5);
            world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getBodyY(0.6), player.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        }
        player.sendMessage(npc.getName().copy().append(": ").append(CampServices.line("enchant.done", itemName)));
        return true;
    }
}

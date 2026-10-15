package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.conversation.Conversations;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import dev.architectury.event.events.common.PlayerEvent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Fire Mage, "Burn an enchantment" (camp service): for a price, the item in the player's main hand is immolated,
 * and one of its enchantments, picked at random, survives on a book. Curses never survive. The lesser version of
 * a companion Fire Mage's Disenchant, which returns every enchantment.
 *
 * <p>Because it destroys the item, it asks twice, in the conversation, before the payment window opens: the first
 * ask names the item and what might survive, and arms for that exact item for {@link #CONFIRM_TICKS}; the second
 * opens the window. Paying burns only that same item; swapped for another meanwhile, the burn is refused and
 * nothing is taken.
 */
final class BurnEnchantmentService extends CampService {
    private static final long CONFIRM_TICKS = 1200L;

    /** A warned burn: which adventurer, which item, until when. */
    private record Armed(UUID npc, ItemStack item, long until) {
    }

    private static final Map<UUID, Armed> ARMED = new HashMap<>();

    static void registerCleanup() {
        PlayerEvent.PLAYER_QUIT.register(player -> ARMED.remove(player.getUuid()));
    }

    @Override
    protected PaidServiceKind kind() {
        return PaidServiceKind.BURN_ENCHANTMENT;
    }

    @Override
    protected double basePrice(ServerConfig config) {
        return config.campBurnEnchantmentPrice;
    }

    /** Something to give back: one non-curse enchantment, or two on a book (one book back is no burn at all). */
    private static boolean burnable(ItemStack stack) {
        int kept = ItemEnchants.list(stack, false).size();
        return stack.isOf(Items.ENCHANTED_BOOK) ? kept >= 2 : kept >= 1;
    }

    @Override
    protected @Nullable Text ownWhyNot(MinibossEntity npc, ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        if (burnable(held)) {
            return null;
        }
        return CampServices.line(held.isOf(Items.ENCHANTED_BOOK) ? "burn.single_book" : "burn.need_enchant");
    }

    private static @Nullable Armed armedFor(MinibossEntity npc, ServerPlayerEntity player) {
        Armed armed = ARMED.get(player.getUuid());
        if (armed == null) {
            return null;
        }
        boolean valid = armed.npc().equals(npc.getUuid()) && npc.getWorld().getTime() <= armed.until()
                && ItemStack.areEqual(armed.item(), player.getMainHandStack());
        if (!valid) {
            ARMED.remove(player.getUuid());
            return null;
        }
        return armed;
    }

    /** The conversation gate: warn first, open the payment window on the second ask. */
    static boolean beforeOpen(MinibossEntity npc, ServerPlayerEntity player) {
        if (armedFor(npc, player) != null) {
            return true;
        }
        ItemStack held = player.getMainHandStack();
        List<ItemEnchants.Entry> survivors = ItemEnchants.list(held, false);
        MutableText names = Text.empty();
        for (int i = 0; i < survivors.size(); i++) {
            if (i > 0) {
                names.append(", ");
            }
            names.append(Enchantment.getName(survivors.get(i).enchantment(), survivors.get(i).level()));
        }
        ARMED.put(player.getUuid(), new Armed(npc.getUuid(), held.copy(), npc.getWorld().getTime() + CONFIRM_TICKS));
        MutableText warning = CampServices.line("burn.warn", held.getName(), names).copy();
        if (!ItemEnchants.list(held, true).isEmpty()) {
            warning.append(" ").append(CampServices.line("burn.warn_curses"));
        }
        Conversations.reply(npc, player, warning);
        return false;
    }

    @Override
    protected boolean deliver(MinibossEntity npc, ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        if (armedFor(npc, player) == null || !burnable(held)) {
            return false; // Not the item that was warned about: nothing burns, nothing is taken.
        }
        ARMED.remove(player.getUuid());
        List<ItemEnchants.Entry> survivors = ItemEnchants.list(held, false);
        ItemEnchants.Entry survivor = survivors.get(npc.getRandom().nextInt(survivors.size()));
        Text itemName = held.getName();
        held.decrement(1);
        player.getInventory().offerOrDrop(ItemEnchants.book(survivor));
        npc.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 0.8F, 1.0F);
        npc.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.NEUTRAL, 0.6F, 1.0F);
        if (npc.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getBodyY(0.6), player.getZ(), 30, 0.3, 0.3, 0.3, 0.03);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getBodyY(0.6), player.getZ(), 10, 0.2, 0.2, 0.2, 0.01);
        }
        player.sendMessage(npc.getName().copy().append(": ").append(CampServices.line("burn.done", itemName,
                Enchantment.getName(survivor.enchantment(), survivor.level()))));
        return true;
    }
}

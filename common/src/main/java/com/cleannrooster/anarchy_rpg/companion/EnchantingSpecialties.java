package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.Conversations;
import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The two specialties that work on the item in the owner's main hand (3.0 M4d step 2):
 * <ul>
 *   <li><b>Templar, Cleanse</b> — every curse is removed; nothing else changes.</li>
 *   <li><b>Fire Mage, Disenchant</b> — each of the item's enchantments comes back on its own book and the
 *   item is immolated (destroyed). Curses burn with it rather than becoming books. An enchanted book with
 *   several enchantments is split the same way. Because it destroys the item, the first ask only says what
 *   would happen; asking again within {@link #CONFIRM_TICKS} with the same item does it.</li>
 * </ul>
 * Both are daily, owner-only, and check the held item on the server at the moment of the click.
 */
final class EnchantingSpecialties {
    private static final String NS = RPGMinibosses.CONTENT_NAMESPACE;
    /** How long a Disenchant warning stays armed. */
    private static final long CONFIRM_TICKS = 200L;

    /** A warned Disenchant, waiting for the second ask: which companion, which item, until when. */
    private record Armed(UUID npc, ItemStack item, long until) {
    }

    private static final Map<UUID, Armed> ARMED = new HashMap<>();

    private EnchantingSpecialties() {
    }

    static void register() {
        CompanionSpecialties.register(CompanionSpecialty.CLEANSE, EnchantingSpecialties::cleanseCheck, EnchantingSpecialties::cleanse);
        CompanionSpecialties.register(CompanionSpecialty.DISENCHANT, EnchantingSpecialties::disenchantCheck, EnchantingSpecialties::disenchant);
        dev.architectury.event.events.common.PlayerEvent.PLAYER_QUIT.register(player -> ARMED.remove(player.getUuid()));
    }

    private static Text line(String key, Object... args) {
        return Text.translatable("conversation." + NS + "." + key, args);
    }

    /** The item's enchantments: stored ones for an enchanted book, applied ones otherwise. */
    private static ItemEnchantmentsComponent enchantments(ItemStack stack) {
        return stack.getOrDefault(stack.isOf(Items.ENCHANTED_BOOK) ? DataComponentTypes.STORED_ENCHANTMENTS : DataComponentTypes.ENCHANTMENTS,
                ItemEnchantmentsComponent.DEFAULT);
    }

    private static int count(ItemStack stack, boolean curses) {
        int n = 0;
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments(stack).getEnchantmentEntries()) {
            if (entry.getKey().isIn(EnchantmentTags.CURSE) == curses) {
                n++;
            }
        }
        return n;
    }

    // ── Templar: Cleanse ────────────────────────────────────────────────────

    private static OptionAvailability cleanseCheck(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record) {
        return count(player.getMainHandStack(), true) > 0
                ? OptionAvailability.ENABLED : OptionAvailability.disabled(line("cleanse.need_curse"));
    }

    private static boolean cleanse(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record, int argument) {
        ItemStack stack = player.getMainHandStack();
        if (count(stack, true) == 0) {
            return true;
        }
        Text name = stack.getName();
        EnchantmentHelper.apply(stack, builder -> builder.remove(entry -> entry.isIn(EnchantmentTags.CURSE)));
        if (stack.isOf(Items.ENCHANTED_BOOK) && enchantments(stack).isEmpty()) {
            // A book of nothing but curses is just a book now.
            player.setStackInHand(Hand.MAIN_HAND, new ItemStack(Items.BOOK, stack.getCount()));
        }
        CompanionSpecialties.markUsed(record, player.getServer());
        npc.getWorld().playSound(null, npc.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.NEUTRAL, 0.8F, 1.4F);
        if (npc.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getBodyY(0.6), player.getZ(), 16, 0.3, 0.3, 0.3, 0.02);
        }
        Conversations.reply(npc, player, line("cleanse.done", name));
        return true;
    }

    // ── Fire Mage: Disenchant ───────────────────────────────────────────────

    /** Whether the held item has anything to give: one non-curse enchantment, or two on a book (a split). */
    private static boolean disenchantable(ItemStack stack) {
        int kept = count(stack, false);
        return stack.isOf(Items.ENCHANTED_BOOK) ? kept >= 2 : kept >= 1;
    }

    private static OptionAvailability disenchantCheck(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record) {
        ItemStack stack = player.getMainHandStack();
        if (!disenchantable(stack)) {
            return OptionAvailability.disabled(line(stack.isOf(Items.ENCHANTED_BOOK) ? "disenchant.single_book" : "disenchant.need_enchant"));
        }
        if (armedFor(player, npc, stack) != null) {
            return new OptionAvailability(true, true, line("disenchant.armed"));
        }
        return OptionAvailability.ENABLED;
    }

    private static @Nullable Armed armedFor(ServerPlayerEntity player, MinibossEntity npc, ItemStack stack) {
        Armed armed = ARMED.get(player.getUuid());
        if (armed == null) {
            return null;
        }
        boolean stillValid = armed.npc().equals(npc.getUuid())
                && Companions.now(player.getServer()) <= armed.until()
                && ItemStack.areEqual(armed.item(), stack);
        if (!stillValid) {
            ARMED.remove(player.getUuid());
            return null;
        }
        return armed;
    }

    private static boolean disenchant(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record, int argument) {
        ItemStack stack = player.getMainHandStack();
        if (!disenchantable(stack)) {
            return true;
        }
        List<ItemStack> books = new ArrayList<>();
        MutableText list = Text.empty();
        for (Object2IntMap.Entry<RegistryEntry<Enchantment>> entry : enchantments(stack).getEnchantmentEntries()) {
            if (entry.getKey().isIn(EnchantmentTags.CURSE)) {
                continue;
            }
            if (!books.isEmpty()) {
                list.append(", ");
            }
            list.append(Enchantment.getName(entry.getKey(), entry.getIntValue()));
            ItemEnchantmentsComponent.Builder one = new ItemEnchantmentsComponent.Builder(ItemEnchantmentsComponent.DEFAULT);
            one.add(entry.getKey(), entry.getIntValue());
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            book.set(DataComponentTypes.STORED_ENCHANTMENTS, one.build());
            books.add(book);
        }

        if (armedFor(player, npc, stack) == null) {
            // First ask: say what would happen, and wait for the second.
            ARMED.put(player.getUuid(), new Armed(npc.getUuid(), stack.copy(), Companions.now(player.getServer()) + CONFIRM_TICKS));
            MutableText warning = line("disenchant.warn", stack.getName(), list).copy();
            if (count(stack, true) > 0) {
                warning.append(" ").append(line("disenchant.warn_curses"));
            }
            Conversations.reply(npc, player, warning);
            return true;
        }

        ARMED.remove(player.getUuid());
        stack.decrement(1);
        for (ItemStack book : books) {
            player.getInventory().offerOrDrop(book);
        }
        CompanionSpecialties.markUsed(record, player.getServer());
        npc.getWorld().playSound(null, player.getBlockPos(), SoundEvents.ENTITY_BLAZE_SHOOT, SoundCategory.NEUTRAL, 0.8F, 1.0F);
        npc.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_FIRE_EXTINGUISH, SoundCategory.NEUTRAL, 0.6F, 1.0F);
        if (npc.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.FLAME, player.getX(), player.getBodyY(0.6), player.getZ(), 30, 0.3, 0.3, 0.3, 0.03);
            world.spawnParticles(ParticleTypes.LARGE_SMOKE, player.getX(), player.getBodyY(0.6), player.getZ(), 10, 0.2, 0.2, 0.2, 0.01);
        }
        Conversations.reply(npc, player, line("disenchant.done", books.size()));
        return true;
    }
}

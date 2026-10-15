package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Templar, "Lift a curse" (camp service): for a price, one random curse comes off the item in the player's main
 * hand. The lesser version of a companion Templar's Cleanse, which lifts every curse for free once a day.
 */
final class LiftCurseService extends CampService {

    @Override
    protected PaidServiceKind kind() {
        return PaidServiceKind.LIFT_CURSE;
    }

    @Override
    protected double basePrice(ServerConfig config) {
        return config.campLiftCursePrice;
    }

    @Override
    protected @Nullable Text ownWhyNot(MinibossEntity npc, ServerPlayerEntity player) {
        return ItemEnchants.list(player.getMainHandStack(), true).isEmpty() ? CampServices.line("lift_curse.need_curse") : null;
    }

    @Override
    protected boolean deliver(MinibossEntity npc, ServerPlayerEntity player) {
        ItemStack held = player.getMainHandStack();
        List<ItemEnchants.Entry> curses = ItemEnchants.list(held, true);
        if (curses.isEmpty()) {
            return false;
        }
        ItemEnchants.Entry lifted = curses.get(npc.getRandom().nextInt(curses.size()));
        Text itemName = held.getName();
        player.setStackInHand(Hand.MAIN_HAND, ItemEnchants.remove(held, lifted.enchantment()));
        npc.getWorld().playSound(null, player.getBlockPos(), SoundEvents.BLOCK_BEACON_DEACTIVATE, SoundCategory.NEUTRAL, 0.8F, 1.4F);
        if (npc.getWorld() instanceof ServerWorld world) {
            world.spawnParticles(ParticleTypes.END_ROD, player.getX(), player.getBodyY(0.6), player.getZ(), 12, 0.3, 0.3, 0.3, 0.02);
        }
        player.sendMessage(npc.getName().copy().append(": ").append(CampServices.line("lift_curse.done",
                Enchantment.getName(lifted.enchantment(), lifted.level()), itemName)));
        return true;
    }
}

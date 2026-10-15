package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.hub.HubAnchor;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * Juggernaut, "Guard the camp" (camp service): for a price, it keeps watch over the camp or tower it lives at for
 * {@code campGuardTicks} ({@link CampGuard}). One guard per camp at a time; the payer gets a safer camp to rest
 * at, not a bodyguard.
 */
final class GuardCampService extends CampService {

    @Override
    protected PaidServiceKind kind() {
        return PaidServiceKind.GUARD_CAMP;
    }

    @Override
    protected double basePrice(ServerConfig config) {
        return config.campGuardPrice;
    }

    @Override
    protected @Nullable Text ownWhyNot(MinibossEntity npc, ServerPlayerEntity player) {
        HubAnchor hub = CampServices.hubOf(npc);
        if (hub == null || !(npc.getWorld() instanceof ServerWorld world)) {
            return null;
        }
        for (HubAnchor.Resident resident : hub.residents()) {
            Entity other = world.getEntity(resident.uuid());
            if (other instanceof MinibossEntity guard && CampGuard.onWatch(guard)) {
                return CampServices.line("guard.on_watch");
            }
        }
        return null;
    }

    @Override
    protected boolean deliver(MinibossEntity npc, ServerPlayerEntity player) {
        npc.setGuardUntil(npc.getWorld().getTime() + RPGMinibossesEntities.config.campGuardTicks);
        npc.getNavigation().stop();
        npc.getWorld().playSound(null, npc.getBlockPos(), SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE.value(), SoundCategory.NEUTRAL, 1.0F, 0.8F);
        player.sendMessage(npc.getName().copy().append(": ").append(CampServices.line("guard.done")));
        return true;
    }
}

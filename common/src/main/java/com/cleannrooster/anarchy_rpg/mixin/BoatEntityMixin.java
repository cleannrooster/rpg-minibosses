package com.cleannrooster.anarchy_rpg.mixin;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.vehicle.BoatEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An adventurer in a boat rows it, even with a player aboard.
 *
 * <p>Vanilla seats a boarding player in front of any non-player passenger ({@code Entity.addPassenger}), and the
 * front passenger is the one in control, so a player climbing in beside a Rogue leading the way would take the
 * boat over: their client would simulate it and the server would ignore the Rogue's steering. Here a boat with an
 * adventurer aboard answers to the adventurer instead. Client and server see the same passengers, so both agree:
 * the client stops simulating the boat and sending its moves, and the server runs it, steered by
 * {@code GuideBoat}.
 */
@Mixin(BoatEntity.class)
abstract class BoatEntityMixin {

    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void anarchy_rpg$adventurerRows(CallbackInfoReturnable<LivingEntity> cir) {
        for (Entity passenger : ((Entity) (Object) this).getPassengerList()) {
            if (passenger instanceof MinibossEntity adventurer) {
                cir.setReturnValue(adventurer);
                return;
            }
        }
    }
}

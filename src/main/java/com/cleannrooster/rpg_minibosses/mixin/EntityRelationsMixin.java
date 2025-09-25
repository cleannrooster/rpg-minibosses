package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.entity.GeminiEntity;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.internals.target.EntityRelations;
import net.spell_engine.internals.target.SpellTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

import static com.cleannrooster.rpg_minibosses.entity.MinibossEntity.DOWN;

@Mixin(EntityRelations.class)
public class EntityRelationsMixin {
      @Inject(at = @At("RETURN"), method = "actionAllowed", cancellable = true)

    private static void actionAllowedRPGMinibosses(SpellTarget.FocusMode focusMode, SpellTarget.Intent intent, LivingEntity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if( attacker instanceof MinibossEntity minibossEntity && attacker != target ){
            if(target instanceof MinibossEntity miniTarget && (!( minibossEntity.isTamed()) || ( miniTarget.getDataTracker().get(DOWN)))) {
                cir.setReturnValue(false);

            }
            else{
                if(minibossEntity.getOwner() != null) {
                    boolean areOwnersTheSame = (target instanceof Tameable miniTarget && miniTarget.getOwner() != null && Objects.equals(minibossEntity.getOwner(), miniTarget.getOwner()));
                    boolean isTargetOwner = (Objects.equals(minibossEntity.getOwner(), target));
                    boolean isMiniboss = target instanceof MinibossEntity;
                    boolean tameableE = target instanceof TameableEntity;
                    boolean isTameable = target instanceof Tameable;
                    boolean isTameableEntity = isTameable || tameableE;
                    boolean canOwnerTargetOtherOwner = (!(target instanceof Tameable tameable) || tameable.getOwner() == null) || (target instanceof Tameable miniTarget && miniTarget.getOwner() != null && minibossEntity.getOwner() instanceof PlayerEntity player && EntityRelations.actionAllowed(focusMode, intent, player, miniTarget.getOwner()));

                    if(isTargetOwner){
                        cir.setReturnValue(false);
                        return;
                    }
                    else if(!isMiniboss && !isTameableEntity){
                        cir.setReturnValue(true);
                        return;
                    }


                    cir.setReturnValue(!areOwnersTheSame && canOwnerTargetOtherOwner);
                }
                else{
                    cir.setReturnValue(true);
                }
            }
        }
          if( attacker instanceof MagusPrimeEntity minibossEntity && attacker != target ) {
                cir.setReturnValue(true);
          }
      }
    @Inject(at = @At("RETURN"), method = "actionAllowed", cancellable = true)

    private static void actionAllowedRPGMiniGemini(SpellTarget.FocusMode focusMode, SpellTarget.Intent intent, LivingEntity attacker, Entity target, CallbackInfoReturnable<Boolean> cir) {
        if(attacker instanceof GeminiEntity geminiEntity){
            if(attacker.equals(target)){
                cir.setReturnValue(false);
            }
            else
            if( geminiEntity.getPartner() != null &&  !geminiEntity.getPartner().equals(target)){
                cir.setReturnValue(true);
            }
            else if( geminiEntity.getPartner() != null &&  geminiEntity.getPartner().equals(target)) {
                cir.setReturnValue(false);
            }
            else if(geminiEntity.getPartner() == null || geminiEntity.getPartner().isDead() || geminiEntity.getPartner().isRemoved()){
                cir.setReturnValue(true);


            }

        }
    }
}
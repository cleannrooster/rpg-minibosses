package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Tameable;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.utils.TargetHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;

import static com.cleannrooster.rpg_minibosses.entity.MinibossEntity.DOWN;

@Mixin(TargetHelper.class)
public class TargetHelperMixin {
    @Inject(at = @At("HEAD"), method = "getRelation", cancellable = true)
    private static void getRelationRPG(LivingEntity attacker, Entity target, CallbackInfoReturnable<TargetHelper.Relation> relationCallbackInfo) {
        if(attacker instanceof MinibossEntity && (!(target instanceof MinibossEntity) || RPGMinibossesEntities.config.betrayal)){
            if(attacker != target)
                relationCallbackInfo.setReturnValue(TargetHelper.Relation.HOSTILE);
            else
                relationCallbackInfo.setReturnValue(TargetHelper.Relation.FRIENDLY);

        }
        if( attacker instanceof MinibossEntity minibossEntity && attacker != target ){
            if(target instanceof MinibossEntity miniTarget && (!( minibossEntity.isTamed()) || ( miniTarget.getDataTracker().get(DOWN)))) {
                relationCallbackInfo.setReturnValue(TargetHelper.Relation.FRIENDLY);

            }
            else{
                if(minibossEntity.getOwner() != null) {
                    boolean areOwnersTheSame = (target instanceof Tameable miniTarget && miniTarget.getOwner() != null && Objects.equals(minibossEntity.getOwner(), miniTarget.getOwner()));
                    boolean isTargetOwner = (Objects.equals(minibossEntity.getOwner(), target));
                    boolean isMiniboss = target instanceof MinibossEntity;
                    boolean tameableE = target instanceof TameableEntity;
                    boolean isTameable = target instanceof Tameable;
                    boolean isTameableEntity = isTameable || tameableE;
                    boolean canOwnerTargetOtherOwner = (!(target instanceof Tameable tameable) || tameable.getOwner() == null) || (target instanceof Tameable miniTarget && miniTarget.getOwner() != null && minibossEntity.getOwner() instanceof PlayerEntity player && TargetHelper.actionAllowed(TargetHelper.TargetingMode.AREA, TargetHelper.Intent.HARMFUL, player, miniTarget.getOwner()));

                    if(isTargetOwner){
                        relationCallbackInfo.setReturnValue(TargetHelper.Relation.FRIENDLY);
                        return;
                    }
                    else if(!isMiniboss && !isTameableEntity){
                        relationCallbackInfo.setReturnValue(TargetHelper.Relation.HOSTILE);
                        return;
                    }


                    relationCallbackInfo.setReturnValue(!areOwnersTheSame && canOwnerTargetOtherOwner ? TargetHelper.Relation.HOSTILE : TargetHelper.Relation.FRIENDLY);
                }
                else{
                    relationCallbackInfo.setReturnValue(TargetHelper.Relation.HOSTILE);
                }
            }
        }
        if(attacker instanceof MagusPrimeEntity){
            if(attacker != target)
                relationCallbackInfo.setReturnValue(TargetHelper.Relation.HOSTILE);
            else
                relationCallbackInfo.setReturnValue(TargetHelper.Relation.FRIENDLY);

        }


    }
}
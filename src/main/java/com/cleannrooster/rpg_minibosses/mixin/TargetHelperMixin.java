package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.spell_engine.utils.TargetHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

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
        if(attacker instanceof MagusPrimeEntity){
            if(attacker != target)
                relationCallbackInfo.setReturnValue(TargetHelper.Relation.HOSTILE);
            else
                relationCallbackInfo.setReturnValue(TargetHelper.Relation.FRIENDLY);

        }
    }
}
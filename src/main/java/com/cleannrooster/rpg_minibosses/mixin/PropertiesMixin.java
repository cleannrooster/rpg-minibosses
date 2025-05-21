package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.spell_engine.api.effect.Synchronized;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(ClientWorld.Properties.class)
public abstract class PropertiesMixin {
  /*  @Shadow
    private long timeOfDay;
    @Inject(at = @At("HEAD"), method = "getTimeOfDay", cancellable = true)
    public void getTimeOfDayRPG(CallbackInfoReturnable<Long> callbackInfoReturnable) {
        if (MinecraftClient.getInstance().player != null && Synchronized.effectsOf(MinecraftClient.getInstance().player).stream().anyMatch(effect -> effect.effect().equals(Effects.DARK_MATTER.effect))) {
                callbackInfoReturnable.setReturnValue(18000L);
        }
    }*/


}

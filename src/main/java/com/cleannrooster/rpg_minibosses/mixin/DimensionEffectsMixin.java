package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.WorldRenderer;
import net.spell_engine.api.effect.Synchronized;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = DimensionEffects.class, priority = Integer.MAX_VALUE)

public class DimensionEffectsMixin {
    @Inject(at = @At("HEAD"), method = "getSkyType", cancellable = true)
    private void getSkyType_RPGMINI(CallbackInfoReturnable<DimensionEffects.SkyType>callbackInfoReturnable) {
        if (MinecraftClient.getInstance().player != null && Synchronized.effectsOf(MinecraftClient.getInstance().player).stream().anyMatch(effect -> effect.effect().equals(Effects.DARK_MATTER.effect))) {

            callbackInfoReturnable.setReturnValue(DimensionEffects.SkyType.END);
        }
    }
}

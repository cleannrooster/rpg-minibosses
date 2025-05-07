package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import net.fabricmc.fabric.api.client.rendering.v1.DimensionRenderingRegistry;
import net.fabricmc.fabric.impl.client.rendering.DimensionRenderingRegistryImpl;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;
import net.spell_engine.api.effect.Synchronized;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;
@Mixin(value = DimensionRenderingRegistryImpl.class, priority = Integer.MAX_VALUE)
public class EndSkyMixin  {
    @Shadow
    private  static Map<RegistryKey<World>, DimensionRenderingRegistry.SkyRenderer> SKY_RENDERERS;


    @Inject(at = @At("HEAD"), method = "getSkyRenderer", cancellable = true)
    private static void getSkyRendererRPGMINIBOSSES(RegistryKey<World> key, CallbackInfoReturnable<DimensionRenderingRegistry.SkyRenderer> callbackInfo) {
        if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player != null && !Synchronized.effectsOf(MinecraftClient.getInstance().player).isEmpty() && Synchronized.effectsOf(MinecraftClient.getInstance().player).stream().anyMatch(effect -> effect.effect().equals(Effects.DARK_MATTER.effect))) {
            callbackInfo.setReturnValue(SKY_RENDERERS.get(World.END));

        }
    }

}

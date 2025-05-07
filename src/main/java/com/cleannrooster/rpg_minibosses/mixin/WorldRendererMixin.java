package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.api.effect.Synchronized;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldRenderer.class, priority = Integer.MIN_VALUE)

public abstract class WorldRendererMixin {
    @Shadow
    public abstract boolean hasBlindnessOrDarkness(Camera camera);
        @Inject(at = @At("HEAD"), method = "renderLayer", cancellable = true)
        private void renderLayerRPG(RenderLayer renderLayer, MatrixStack matrices, double cameraX, double cameraY, double cameraZ, Matrix4f positionMatrix, CallbackInfo info) {
        if (!FabricLoader.getInstance().isModLoaded("distanthorizons") &&  MinecraftClient.getInstance().player != null && !Synchronized.effectsOf(MinecraftClient.getInstance().player).isEmpty() && Synchronized.effectsOf(MinecraftClient.getInstance().player).stream().anyMatch(effect -> effect.effect().equals(Effects.DARK_MATTER.effect))){


            info.cancel();
        }
    }
    /*    @Shadow

    @Inject(at = @At("HEAD"), method = "renderSky", cancellable = true)
    public void renderSkyrpg(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo info) {
        if (  MinecraftClient.getInstance().player != null && Synchronized.effectsOf(MinecraftClient.getInstance().player).stream().anyMatch(effect -> effect.effect().equals(Effects.DARK_MATTER.effect))){
            if (!thickFog) {
                CameraSubmersionType cameraSubmersionType = camera.getSubmersionType();
                if (cameraSubmersionType != CameraSubmersionType.POWDER_SNOW && cameraSubmersionType != CameraSubmersionType.LAVA && !this.hasBlindnessOrDarkness(camera)) {

                    MatrixStack matrixStack = new MatrixStack();
                    matrixStack.multiplyPositionMatrix(matrix4f);
                    renderEndSky(matrixStack);
                    info.cancel();
                }
            }
        }
    }*/
        @Inject(at = @At("HEAD"), method = "renderEntity", cancellable = true)
    private void renderEntity_RPGMINI(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        if (MinecraftClient.getInstance().player != null && !Synchronized.effectsOf(MinecraftClient.getInstance().player).isEmpty() && Synchronized.effectsOf(MinecraftClient.getInstance().player).stream().anyMatch(effect -> effect.effect().equals(Effects.DARK_MATTER.effect))){
            if(entity instanceof LivingEntity living && !(entity instanceof PlayerEntity player || entity instanceof MagusPrimeEntity magusPrimeEntity)) {
                info.cancel();
            }
        }
    }
    @Inject(at = @At("HEAD"), method = "renderClouds", cancellable = true)
    public void renderCloudsRPG(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double cameraX, double cameraY, double cameraZ,CallbackInfo info) {
        if (MinecraftClient.getInstance().player != null && !Synchronized.effectsOf(MinecraftClient.getInstance().player).isEmpty() && Synchronized.effectsOf(MinecraftClient.getInstance().player).stream().anyMatch(effect -> effect.effect().equals(Effects.DARK_MATTER.effect))){
            info.cancel();
        }
    }

}

package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.DarkMatterView;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldRenderer.class, priority = Integer.MIN_VALUE)

public abstract class WorldRendererMixin {
    @Shadow
    public abstract void renderEndSky(MatrixStack matrices);
    @Shadow
    public abstract boolean hasBlindnessOrDarkness(Camera camera);
        @Inject(at = @At("HEAD"), method = "renderLayer", cancellable = true)
    private void renderLayer_RPGMINI(RenderLayer renderLayer, double x, double y, double z, Matrix4f matrix4f, Matrix4f positionMatrix, CallbackInfo info) {
        if (!FabricLoader.getInstance().isModLoaded("distanthorizons") &&  DarkMatterView.isActive()){
            MatrixStack matrixStack = new MatrixStack();
            matrixStack.multiplyPositionMatrix(matrix4f);

            info.cancel();
        }
    }
    @Inject(at = @At("HEAD"), method = "renderSky", cancellable = true)
    public void renderSkyrpg(Matrix4f matrix4f, Matrix4f projectionMatrix, float tickDelta, Camera camera, boolean thickFog, Runnable fogCallback, CallbackInfo info) {
        if (  DarkMatterView.isActive() || MinecraftClient.getInstance().world.getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
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
    }
        @Inject(at = @At("HEAD"), method = "renderEntity", cancellable = true)
    private void renderEntity_RPGMINI(Entity entity, double cameraX, double cameraY, double cameraZ, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, CallbackInfo info) {
        if (DarkMatterView.isActive()){
            if(entity instanceof LivingEntity living && !(entity instanceof PlayerEntity player || DarkMatterView.isExempt(entity))) {
                info.cancel();
            }
        }
    }
    @Inject(at = @At("HEAD"), method = "renderClouds", cancellable = true)
    public void renderClouds_RPGMINI(MatrixStack matrices, Matrix4f matrix4f, Matrix4f matrix4f2, float tickDelta, double cameraX, double cameraY, double cameraZ, CallbackInfo info) {
        if (DarkMatterView.isActive() || MinecraftClient.getInstance().world.getRegistryKey().equals(RPGMinibosses.DIMENSIONKEY)){
            info.cancel();
        }
    }

}

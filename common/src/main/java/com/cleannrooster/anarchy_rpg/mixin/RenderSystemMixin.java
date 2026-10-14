package com.cleannrooster.anarchy_rpg.mixin;

import com.cleannrooster.anarchy_rpg.client.entity.effect.Effects;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import static com.mojang.blaze3d.systems.RenderSystem.isOnRenderThread;
import static com.mojang.blaze3d.systems.RenderSystem.recordRenderCall;

@Mixin(RenderSystem.class)
public abstract class RenderSystemMixin {

}

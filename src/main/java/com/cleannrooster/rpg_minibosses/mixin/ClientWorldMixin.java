package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.render.DimensionEffects;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.command.TimeCommand;
import net.minecraft.util.profiler.Profiler;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.spell_engine.api.effect.Synchronized;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    /*@Shadow
    private  ClientWorld.Properties clientWorldProperties;


    @Inject(at = @At("HEAD"), method = "getDimensionEffects", cancellable = true)
    public void getDimensionEffectsRPGBosses(CallbackInfoReturnable<DimensionEffects> callbackInfoReturnable) {
        if (MinecraftClient.getInstance().player != null && Synchronized.effectsOf(MinecraftClient.getInstance().player).stream().anyMatch(effect -> effect.effect().equals(Effects.DARK_MATTER.effect))) {
            callbackInfoReturnable.setReturnValue(new DimensionEffects.End());
        }
    }*/
}

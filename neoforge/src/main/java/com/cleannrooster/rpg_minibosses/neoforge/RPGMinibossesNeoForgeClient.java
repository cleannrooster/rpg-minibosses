package com.cleannrooster.rpg_minibosses.neoforge;

import com.cleannrooster.rpg_minibosses.client.RPGMinibossesClient;
import com.cleannrooster.rpg_minibosses.client.WorldRenderHook;
import net.minecraft.client.MinecraftClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge client setup. The {@code modid} here must be the NeoForge loader id ({@code rpg_minibosses}),
 * while every shared client identifier still uses the content namespace ({@code rpg-minibosses}).
 */
@EventBusSubscriber(modid = RPGMinibossesNeoForge.LOADER_MOD_ID, value = Dist.CLIENT)
public final class RPGMinibossesNeoForgeClient {
    private RPGMinibossesNeoForgeClient() {
    }

    /** Called from the mod constructor on the physical client, before payload registration closes. */
    static void construct() {
        RPGMinibossesClient.earlyInit();
        NeoForge.EVENT_BUS.addListener(RPGMinibossesNeoForgeClient::onRenderLevelStage);
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(RPGMinibossesClient::init);
    }

    private static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) {
            return;
        }
        WorldRenderHook.fire(event.getPoseStack(), event.getCamera(),
                MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(),
                event.getPartialTick().getTickDelta(false));
    }
}

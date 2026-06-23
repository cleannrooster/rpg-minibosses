package com.cleannrooster.rpg_minibosses.neoforge;

import com.cleannrooster.rpg_minibosses.client.RPGMinibossesClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * NeoForge client setup. The {@code modid} here must be the NeoForge loader id ({@code rpg_minibosses}),
 * while every shared client identifier still uses the content namespace ({@code rpg-minibosses}).
 */
@EventBusSubscriber(modid = RPGMinibossesNeoForge.LOADER_MOD_ID, value = Dist.CLIENT)
public final class RPGMinibossesNeoForgeClient {
    private RPGMinibossesNeoForgeClient() {
    }

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(RPGMinibossesClient::init);
    }
}

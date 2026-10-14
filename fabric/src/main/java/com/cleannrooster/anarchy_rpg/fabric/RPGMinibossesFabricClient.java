package com.cleannrooster.anarchy_rpg.fabric;

import com.cleannrooster.anarchy_rpg.client.RPGMinibossesClient;
import com.cleannrooster.anarchy_rpg.client.WorldRenderHook;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;

/** Fabric client entrypoint. Delegates to the loader-independent client setup. */
public final class RPGMinibossesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RPGMinibossesClient.earlyInit();
        RPGMinibossesClient.init();
        WorldRenderEvents.AFTER_ENTITIES.register(context -> WorldRenderHook.fire(context.matrixStack(),
                context.camera(), context.consumers(), context.tickCounter().getTickDelta(false)));
    }
}

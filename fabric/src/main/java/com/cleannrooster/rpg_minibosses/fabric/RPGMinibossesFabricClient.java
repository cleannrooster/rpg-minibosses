package com.cleannrooster.rpg_minibosses.fabric;

import com.cleannrooster.rpg_minibosses.client.RPGMinibossesClient;
import net.fabricmc.api.ClientModInitializer;

/** Fabric client entrypoint. Delegates to the loader-independent client setup. */
public final class RPGMinibossesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        RPGMinibossesClient.init();
    }
}

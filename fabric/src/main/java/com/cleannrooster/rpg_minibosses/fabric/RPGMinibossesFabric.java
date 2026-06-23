package com.cleannrooster.rpg_minibosses.fabric;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric loader entrypoint. Loader mod id {@code "rpg-minibosses"} (matches the content namespace).
 *
 * <p>Fabric registries are open throughout {@code onInitialize}, so the shared registration phases
 * run directly, in dependency order: config -> stats -> sounds -> entities -> blocks -> effects ->
 * items (needs entities + blocks + config) -> item group -> deferred init (events/handlers).</p>
 */
public final class RPGMinibossesFabric implements ModInitializer {
    public static final String LOADER_MOD_ID = "rpg-minibosses";

    @Override
    public void onInitialize() {
        RPGMinibosses.initConfig();

        RPGMinibosses.registerStats();
        RPGMinibosses.registerSounds();
        RPGMinibosses.registerEntities();
        RPGMinibosses.registerBlocks();
        RPGMinibosses.registerEffects();
        RPGMinibosses.registerItems();
        RPGMinibosses.registerItemGroup();

        RPGMinibosses.init();
    }
}

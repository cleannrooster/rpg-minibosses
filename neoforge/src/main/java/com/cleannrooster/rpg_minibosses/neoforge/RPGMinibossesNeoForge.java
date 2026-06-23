package com.cleannrooster.rpg_minibosses.neoforge;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.registry.RegistryKeys;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge loader entrypoint. Loader mod id {@code "rpg_minibosses"} (underscore — NeoForge forbids
 * the hyphenated content namespace). Content namespace stays {@link RPGMinibosses#CONTENT_NAMESPACE}.
 *
 * <p>NeoForge freezes vanilla registries before mod construction, so the constructor only does
 * non-registry work (config load + event/handler registration). Registry mutation runs from
 * {@link RegisterEvent}, each shared method dispatched into its matching registry phase. Each
 * {@code event.register} consumer only fires when the firing registry matches its key, so listing
 * every phase unconditionally is correct, and the relative firing order (entity types before items,
 * blocks before block items) is the vanilla registry order.</p>
 */
@Mod(RPGMinibossesNeoForge.LOADER_MOD_ID)
public final class RPGMinibossesNeoForge {
    public static final String LOADER_MOD_ID = "rpg_minibosses";

    public RPGMinibossesNeoForge(IEventBus modBus) {
        RPGMinibosses.initConfig();

        modBus.addListener(RPGMinibossesNeoForge::onRegister);

        // Non-registry: spell handlers, gameplay events, world-gen injection, model ids.
        RPGMinibosses.init();
    }

    private static void onRegister(RegisterEvent event) {
        event.register(RegistryKeys.CUSTOM_STAT, helper -> RPGMinibosses.registerStats());
        event.register(RegistryKeys.SOUND_EVENT, helper -> RPGMinibosses.registerSounds());
        event.register(RegistryKeys.ENTITY_TYPE, helper -> RPGMinibosses.registerEntities());
        event.register(RegistryKeys.BLOCK, helper -> RPGMinibosses.registerBlocks());
        event.register(RegistryKeys.STATUS_EFFECT, helper -> RPGMinibosses.registerEffects());
        event.register(RegistryKeys.ITEM, helper -> RPGMinibosses.registerItems());
        event.register(RegistryKeys.ITEM_GROUP, helper -> RPGMinibosses.registerItemGroup());
    }
}

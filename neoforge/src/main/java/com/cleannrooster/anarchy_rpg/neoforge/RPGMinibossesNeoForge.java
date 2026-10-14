package com.cleannrooster.anarchy_rpg.neoforge;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.registry.RegistryKeys;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.registries.RegisterEvent;

/**
 * NeoForge loader entrypoint. Loader mod id {@code "anarchy_rpg"}, which matches
 * {@link RPGMinibosses#CONTENT_NAMESPACE} but is kept as its own constant: the two name different things.
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
    public static final String LOADER_MOD_ID = "anarchy_rpg";

    public RPGMinibossesNeoForge(IEventBus modBus) {
        RPGMinibosses.initConfig();

        modBus.addListener(RPGMinibossesNeoForge::onRegister);

        // Non-registry: spell handlers, gameplay events, world-gen injection, model ids.
        RPGMinibosses.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            RPGMinibossesNeoForgeClient.construct();
        }
    }

    private static void onRegister(RegisterEvent event) {
        // Phase set mirrors the artificers baseline (same author/ecosystem) that registers on NeoForge
        // successfully. ITEM_GROUP and ARMOR_MATERIAL are NOT given their own phases — they are written
        // during the ITEM phase (armor materials via Armors static-init, creative tabs in registerItems),
        // which NeoForge accepts. Splitting them out is what caused the frozen-registry crash.
        event.register(RegistryKeys.SOUND_EVENT, helper -> RPGMinibosses.registerSounds());
        event.register(RegistryKeys.ENTITY_TYPE, helper -> RPGMinibosses.registerEntities());
        event.register(RegistryKeys.BLOCK, helper -> RPGMinibosses.registerBlocks());
        event.register(RegistryKeys.STATUS_EFFECT, helper -> RPGMinibosses.registerEffects());
        event.register(RegistryKeys.FEATURE, helper -> RPGMinibosses.registerFeatures());
        event.register(RegistryKeys.ITEM, helper -> RPGMinibosses.registerItems());
    }
}

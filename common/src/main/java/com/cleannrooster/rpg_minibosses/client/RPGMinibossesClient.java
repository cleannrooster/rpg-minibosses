package com.cleannrooster.rpg_minibosses.client;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.GeminiRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossRenderer;
import com.cleannrooster.rpg_minibosses.compat.SpellEngineCompatClient;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.entity.TrapRenderer;
import com.cleannrooster.rpg_minibosses.magic.entity.MagicEntities;
import net.minecraft.client.render.entity.EmptyEntityRenderer;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;


/**
 * Loader-independent client setup. Invoked from Fabric's {@code ClientModInitializer} and NeoForge's
 * {@code FMLClientSetupEvent}. Uses Fabric client APIs that Forgified Fabric API mirrors on NeoForge.
 * Only ever loaded on the physical client.
 */
public final class RPGMinibossesClient {
    private RPGMinibossesClient() {
    }

    /**
     * Work that must happen during mod construction rather than client setup: payload receivers have to be
     * declared before the loader closes network registration.
     */
    public static void earlyInit() {
        com.cleannrooster.rpg_minibosses.client.combat.SlashEffectManager.registerReceiver();
        registerEntityRenderers();
        SpellEngineCompatClient.earlyInit();
    }

    /**
     * Must run during mod construction. On NeoForge, Architectury only queues these and hands them over when
     * {@code EntityRenderersEvent.RegisterRenderers} fires - which happens during the first resource load,
     * before client setup - so anything registered from {@link #init()} was silently dropped, leaving every
     * mob of this mod without a renderer and crashing the client the moment one came into view. The types
     * are suppliers, so they need not be registered yet.
     */
    private static void registerEntityRenderers() {
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.JUGGERNAUT_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.JUGG_MODEL, MinibossRenderer.JUGG_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.ARTILLERIST_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.MERCENARY_MODEL, MinibossRenderer.MERCENARY_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.TRICKSTER_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.ROGUE_MODEL, MinibossRenderer.ROGUE_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.FIREMAGE_MODEL, MinibossRenderer.FIREMAGE_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.TEMPLAR_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.TEMPLAR_MODEL, MinibossRenderer.TEMPLAR_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.M_ARTILLERIST_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.MERCENARY_MODEL, MinibossRenderer.MERCENARY_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.M_TRICKSTER_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.ROGUE_MODEL, MinibossRenderer.ROGUE_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.M_TEMPLAR_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.TEMPLAR_MODEL, MinibossRenderer.TEMPLAR_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.M_JUGGERNAUT_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.JUGG_MODEL, MinibossRenderer.JUGG_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.M_ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.FIREMAGE_MODEL, MinibossRenderer.FIREMAGE_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.GEMINI_ALPHA.entityType, (context) -> new GeminiRenderer(context, GeminiRenderer.FIRE_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.GEMINI_BETA.entityType, (context) -> new GeminiRenderer(context, GeminiRenderer.FROST_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.GEMINI_ALPHA_UBER.entityType, (context) -> new GeminiRenderer(context, GeminiRenderer.FIRE_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.GEMINI_BETA_UBER.entityType, (context) -> new GeminiRenderer(context, GeminiRenderer.FROST_TEXTURE));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.STORM_ANCHOR, (context) -> new net.minecraft.client.render.entity.EmptyEntityRenderer<>(context));
        EntityRendererRegistry.register(() -> RPGMinibossesEntities.TRAP, TrapRenderer::new);
        // Ability carriers draw nothing themselves yet; their visuals arrive with the effect cues.
        EntityRendererRegistry.register(() -> MagicEntities.PROJECTILE, EmptyEntityRenderer::new);
        EntityRendererRegistry.register(() -> MagicEntities.CLOUD, EmptyEntityRenderer::new);
    }

    public static void init() {
        // Attack ribbons: receives one swing packet per attack and rebuilds the arc locally from the same
        // geometry the server damaged with.
        com.cleannrooster.rpg_minibosses.client.combat.SlashEffectManager.init();
        com.cleannrooster.rpg_minibosses.client.fx.CueHandlers.init();


        ClientTickEvent.CLIENT_PRE.register((client) -> {
            if (client.crosshairTarget instanceof EntityHitResult hitResult
                    && hitResult.getEntity() instanceof MinibossEntity entity
                    && entity.getDataTracker().get(MinibossEntity.DOWN)) {
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable("text.rpg-minibosses.spare"), true);
                }
            }
        });

        SpellEngineCompatClient.init();
    }
}

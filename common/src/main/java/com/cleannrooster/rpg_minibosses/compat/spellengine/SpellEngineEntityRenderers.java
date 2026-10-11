package com.cleannrooster.rpg_minibosses.compat.spellengine;

import com.cleannrooster.rpg_minibosses.compat.SpellEngineCompatClient;
import com.cleannrooster.rpg_minibosses.compat.spellengine.client.MagusRenderer;
import com.cleannrooster.rpg_minibosses.compat.spellengine.client.OrbRenderer;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;
import net.minecraft.client.render.entity.EmptyEntityRenderer;

/**
 * The Spell Engine content's entity renderers. Run by {@link SpellEngineCompatClient#earlyInit()} during mod
 * construction, for the same reason as the core's: on NeoForge a later registration is never handed over.
 */
public final class SpellEngineEntityRenderers implements Runnable {
    @Override
    public void run() {
        EntityRendererRegistry.register(() -> SpellEngineEntities.ORB, OrbRenderer::new);
        EntityRendererRegistry.register(() -> SpellEngineEntities.MAGUS_PRIME.entityType, MagusRenderer::new);
        EntityRendererRegistry.register(() -> SpellEngineEntities.MAGUS_DOMINION_ORB, (context) -> new EmptyEntityRenderer<>(context));
    }
}

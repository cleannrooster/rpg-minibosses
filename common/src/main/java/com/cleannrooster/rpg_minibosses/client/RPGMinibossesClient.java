package com.cleannrooster.rpg_minibosses.client;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.AbberrathRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.JuggernautArmorRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.ThiefArmorRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.UniqueArmorRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.WhisperingIceRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.client.entity.effect.FeatherRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.GeminiRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MagusRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.OrbRenderer;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.entity.TrapRenderer;
import com.cleannrooster.rpg_minibosses.item.Armors;
import com.cleannrooster.rpg_minibosses.item.CompatArmors;
import com.cleannrooster.rpg_minibosses.platform.Platform;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererRegistry;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.spell_engine.api.effect.CustomModelStatusEffect;

import java.util.function.Supplier;

/**
 * Loader-independent client setup. Invoked from Fabric's {@code ClientModInitializer} and NeoForge's
 * {@code FMLClientSetupEvent}. Uses Fabric client APIs that Forgified Fabric API mirrors on NeoForge.
 * Only ever loaded on the physical client.
 */
public final class RPGMinibossesClient {
    private RPGMinibossesClient() {
    }

    public static void init() {
        // Attack ribbons: receives one swing packet per attack and rebuilds the arc locally from the same
        // geometry the server damaged with.
        com.cleannrooster.rpg_minibosses.client.combat.SlashEffectManager.init();
        EntityRendererRegistry.register(RPGMinibosses.ORBENTITY, OrbRenderer::new);

        CustomModelStatusEffect.register(Effects.FEATHER.effect, new FeatherRenderer());
        EntityRendererRegistry.register(RPGMinibossesEntities.JUGGERNAUT_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.JUGG_MODEL, MinibossRenderer.JUGG_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.ARTILLERIST_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.MERCENARY_MODEL, MinibossRenderer.MERCENARY_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.TRICKSTER_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.ROGUE_MODEL, MinibossRenderer.ROGUE_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.FIREMAGE_MODEL, MinibossRenderer.FIREMAGE_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.TEMPLAR_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.TEMPLAR_MODEL, MinibossRenderer.TEMPLAR_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.MAGuS_PRIME.entityType, MagusRenderer::new);
        EntityRendererRegistry.register(RPGMinibossesEntities.M_ARTILLERIST_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.MERCENARY_MODEL, MinibossRenderer.MERCENARY_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.M_TRICKSTER_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.ROGUE_MODEL, MinibossRenderer.ROGUE_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.M_TEMPLAR_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.TEMPLAR_MODEL, MinibossRenderer.TEMPLAR_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.M_JUGGERNAUT_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.JUGG_MODEL, MinibossRenderer.JUGG_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.M_ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) -> new MinibossRenderer(context, MinibossRenderer.FIREMAGE_MODEL, MinibossRenderer.FIREMAGE_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_ALPHA.entityType, (context) -> new GeminiRenderer(context, GeminiRenderer.FIRE_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_BETA.entityType, (context) -> new GeminiRenderer(context, GeminiRenderer.FROST_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_ALPHA_UBER.entityType, (context) -> new GeminiRenderer(context, GeminiRenderer.FIRE_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_BETA_UBER.entityType, (context) -> new GeminiRenderer(context, GeminiRenderer.FROST_TEXTURE));
        EntityRendererRegistry.register(RPGMinibossesEntities.STORM_ANCHOR, (context) -> new net.minecraft.client.render.entity.EmptyEntityRenderer<>(context));
        EntityRendererRegistry.register(RPGMinibossesEntities.MAGUS_DOMINION_ORB, (context) -> new net.minecraft.client.render.entity.EmptyEntityRenderer<>(context));
        EntityRendererRegistry.register(RPGMinibossesEntities.TRAP, TrapRenderer::new);

        ModelPredicateProviderRegistry.register(RPGMinibosses.LAVOSHORN, RPGMinibosses.id("tooting"), (stack, world, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0F : 0.0F);

        ClientTickEvents.START_CLIENT_TICK.register((client) -> {
            if (client.crosshairTarget instanceof EntityHitResult hitResult
                    && hitResult.getEntity() instanceof MinibossEntity entity
                    && entity.getDataTracker().get(MinibossEntity.DOWN)) {
                if (client.player != null) {
                    client.player.sendMessage(Text.translatable("text.rpg-minibosses.spare"), true);
                }
            }
        });

        AzArmorRendererRegistry.register(AbberrathRenderer::new, Armors.ABBERRATH);
        AzItemRendererRegistry.register(Armors.whispering_ice.item(), WhisperingIceRenderer::new);
        if (Platform.isModLoaded("extraspellattributes")) {
            registerArmorRenderer(CompatArmors.juggernautArmor, JuggernautArmorRenderer::new);
            registerArmorRenderer(CompatArmors.tricksterArmor, ThiefArmorRenderer::new);
        }

        registerArmorRenderer(Armors.despotArmor, UniqueArmorRenderer::despot);
        registerArmorRenderer(Armors.foxArmor, UniqueArmorRenderer::foxshade);
        registerArmorRenderer(Armors.kintsugiArmor, UniqueArmorRenderer::kintsugi);
        registerArmorRenderer(Armors.sanguine_red, UniqueArmorRenderer::sanguine_fire);
        registerArmorRenderer(Armors.sanguine_blue, UniqueArmorRenderer::sanguine_frost);
        registerArmorRenderer(Armors.sanguine_purple, UniqueArmorRenderer::sanguine_arcane);
    }

    private static void registerArmorRenderer(net.spell_engine.rpg_series.item.Armor.Set set, Supplier<AzArmorRenderer> armorRendererSupplier) {
        AzArmorRendererRegistry.register(armorRendererSupplier, set.head, set.chest, set.legs, set.feet);
    }

    private static void registerArmorItemRenderer(net.spell_engine.rpg_series.item.Armor.Set set, Supplier<AzItemRenderer> armorRendererSupplier) {
        AzItemRendererRegistry.register(set.chest, armorRendererSupplier);
        AzItemRendererRegistry.register(set.legs, armorRendererSupplier);
    }
}

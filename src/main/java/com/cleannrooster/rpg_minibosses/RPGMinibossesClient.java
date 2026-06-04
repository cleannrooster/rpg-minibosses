package com.cleannrooster.rpg_minibosses;


import com.cleannrooster.rpg_minibosses.client.armor.renderer.AbberrathRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.JuggernautArmorRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.ThiefArmorRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.UniqueArmorRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.WhisperingIceRenderer;
import com.cleannrooster.rpg_minibosses.item.CompatArmors;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererRegistry;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import net.fabricmc.loader.api.FabricLoader;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.client.entity.effect.FeatherRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.GeminiRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MagusRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.OrbRenderer;
import com.cleannrooster.rpg_minibosses.entity.TrapRenderer;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import com.cleannrooster.rpg_minibosses.item.Armors;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.item.ArmorItem;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.rpg_series.item.Armor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.function.Supplier;

public class RPGMinibossesClient implements ClientModInitializer {
	public static final String MOD_ID = "rpg-minibosses";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);



	@Override
	public void onInitializeClient() {
        EntityRendererRegistry.register(RPGMinibosses.ORBENTITY, OrbRenderer::new);
        //CustomModels.registerModelIds(List.of(Identifier.of(RPGMinibosses.MOD_ID,"projectile/iron_dagger")));

        CustomModelStatusEffect.register(Effects.FEATHER.effect, new FeatherRenderer());
		EntityRendererRegistry.register(RPGMinibossesEntities.JUGGERNAUT_ENTITY_ENTRY.entityType,(context) ->  new MinibossRenderer(context,MinibossRenderer.JUGG_MODEL,MinibossRenderer.JUGG_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.ARTILLERIST_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.MERCENARY_MODEL,MinibossRenderer.MERCENARY_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.TRICKSTER_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.ROGUE_MODEL,MinibossRenderer.ROGUE_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.FIREMAGE_MODEL,MinibossRenderer.FIREMAGE_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.TEMPLAR_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.TEMPLAR_MODEL,MinibossRenderer.TEMPLAR_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.MAGuS_PRIME.entityType, MagusRenderer::new);
		EntityRendererRegistry.register(RPGMinibossesEntities.M_ARTILLERIST_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.MERCENARY_MODEL,MinibossRenderer.MERCENARY_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_TRICKSTER_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.ROGUE_MODEL,MinibossRenderer.ROGUE_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_TEMPLAR_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.TEMPLAR_MODEL,MinibossRenderer.TEMPLAR_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_JUGGERNAUT_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.JUGG_MODEL,MinibossRenderer.JUGG_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer(context, MinibossRenderer.FIREMAGE_MODEL,MinibossRenderer.FIREMAGE_TEXTURE));
		EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_ALPHA.entityType, (context) ->{return new GeminiRenderer(context,GeminiRenderer.FIRE_TEXTURE);});
		EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_BETA.entityType, (context) ->{return new GeminiRenderer(context,GeminiRenderer.FROST_TEXTURE);});
		EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_ALPHA_UBER.entityType, (context) ->{return new GeminiRenderer(context,GeminiRenderer.FIRE_TEXTURE);});
		EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_BETA_UBER.entityType, (context) ->{return new GeminiRenderer(context,GeminiRenderer.FROST_TEXTURE);});
		EntityRendererRegistry.register(RPGMinibossesEntities.STORM_ANCHOR, (context) -> new net.minecraft.client.render.entity.EmptyEntityRenderer<>(context));
		EntityRendererRegistry.register(RPGMinibossesEntities.MAGUS_DOMINION_ORB, (context) -> new net.minecraft.client.render.entity.EmptyEntityRenderer<>(context));

		EntityRendererRegistry.register(RPGMinibossesEntities.TRAP, TrapRenderer::new);


		ModelPredicateProviderRegistry.register(RPGMinibosses.LAVOSHORN, Identifier.of(MOD_ID,"tooting"), (stack, world, entity, seed) -> {
			return entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0F : 0.0F;		});
		ClientTickEvents.START_CLIENT_TICK.register((client) -> {
			if(client.crosshairTarget instanceof EntityHitResult hitResult && hitResult.getEntity() instanceof MinibossEntity entity && entity.getDataTracker().get(MinibossEntity.DOWN)){
				if(client.player != null) {
					client.player.sendMessage(Text.translatable("text.rpg-minibosses.spare"), true);
				}
			}
		});

        AzArmorRendererRegistry.register(AbberrathRenderer::new, Armors.ABBERRATH);
        AzItemRendererRegistry.register(Armors.whispering_ice.item(), WhisperingIceRenderer::new);
        if (FabricLoader.getInstance().isModLoaded("extraspellattributes")) {
                registerArmorRenderer( CompatArmors.juggernautArmor,JuggernautArmorRenderer::new);


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
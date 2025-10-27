package com.cleannrooster.rpg_minibosses;


import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.client.entity.effect.FeatherRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.model.*;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.GeminiRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MagusRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.OrbRenderer;
import com.cleannrooster.rpg_minibosses.entity.TrapRenderer;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.item.ModelPredicateProviderRegistry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.render.CustomModels;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class RPGMinibossesClient implements ClientModInitializer {
	public static final String MOD_ID = "rpg-minibosses";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);



	@Override
	public void onInitializeClient() {
        EntityRendererRegistry.register(RPGMinibosses.ORBENTITY, OrbRenderer::new);
        CustomModels.registerModelIds(List.of(Identifier.of(RPGMinibosses.MOD_ID,"projectile/iron_dagger")));

        CustomModelStatusEffect.register(Effects.FEATHER.effect, new FeatherRenderer());
		EntityRendererRegistry.register(RPGMinibossesEntities.JUGGERNAUT_ENTITY_ENTRY.entityType,(context) ->  new MinibossRenderer<>(context, new JuggernautModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.ARTILLERIST_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new ArtilleristModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.TRICKSTER_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new TricksterModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new ArchmageFireModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.TEMPLAR_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new TemplarModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.MAGuS_PRIME.entityType, (context) ->  new MagusRenderer<>(context, new MagusModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_ARTILLERIST_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new ArtilleristModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_TRICKSTER_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new TricksterModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_TEMPLAR_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new TemplarModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_JUGGERNAUT_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new JuggernautModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.M_ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new ArchmageFireModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_ALPHA.entityType, (context) ->  new GeminiRenderer<>(context, new GeminiModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.GEMINI_BETA.entityType, (context) ->  new GeminiRenderer<>(context, new GeminiModel<>()));

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
	}



}
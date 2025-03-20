package com.cleannrooster.rpg_minibosses;

import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.client.entity.effect.FeatherRenderer;
import com.cleannrooster.rpg_minibosses.client.entity.model.*;
import com.cleannrooster.rpg_minibosses.client.entity.renderer.MinibossRenderer;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RPGMinibossesClient implements ClientModInitializer {
	public static final String MOD_ID = "rpg-minibosses";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);



	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(RPGMinibossesEntities.JUGGERNAUT_ENTITY_ENTRY.entityType,(context) ->  new MinibossRenderer<>(context, new JuggernautModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.ARTILLERIST_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new ArtilleristModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.TRICKSTER_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new TricksterModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.ARCHMAGE_FIRE_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new ArchmageFireModel<>()));
		EntityRendererRegistry.register(RPGMinibossesEntities.TEMPLAR_ENTITY_ENTRY.entityType, (context) ->  new MinibossRenderer<>(context, new TemplarModel<>()));

		CustomModelStatusEffect.register(Effects.FEATHER.effect, new FeatherRenderer());

	}
}
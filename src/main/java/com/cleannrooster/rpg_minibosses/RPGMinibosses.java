package com.cleannrooster.rpg_minibosses;

import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.RPGMinibossesEntities;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.util.Identifier;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.rpg_series.RPGSeriesCore;
import net.spell_engine.rpg_series.loot.LootHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;

import static net.spell_engine.rpg_series.RPGSeriesCore.lootEquipmentConfig;

public class RPGMinibosses implements ModInitializer {
	public static final String MOD_ID = "rpg-minibosses";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.
		RPGMinibossesEntities.register();

		Effects.register();
		CustomModels.registerModelIds(List.of(
				Identifier.of(MOD_ID, "projectile/feather")
		));
		CustomModels.registerModelIds(List.of(
				Identifier.of(MOD_ID, "projectile/flamewaveprojectile")
		));

		LOGGER.info("Hello Fabric world!");
	}
}
package com.cleannrooster.rpg_minibosses.worldgen;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.feature.PlacedFeature;

/**
 * Worldgen registration for the five miniboss encounters.
 *
 * <p>{@link #registerFeature()} adds the single shared {@link MinibossEncounterFeature} to the
 * (vanilla, static) {@code FEATURE} registry — called from the Fabric initializer and the NeoForge
 * {@code RegisterEvent} for {@code FEATURE}. The configured and placed features themselves are
 * data-driven (JSON under {@code data/rpg-minibosses/worldgen/...}); {@link #injectBiomes()} adds the
 * placed features to biomes via the biome tags below, using Fabric API biome modifications (backed by
 * Forgified Fabric API on NeoForge, matching how this mod already injects mob spawns).</p>
 */
public final class MinibossEncounters {
    public static final Identifier FEATURE_ID = RPGMinibosses.id("miniboss_encounter");
    public static MinibossEncounterFeature ENCOUNTER_FEATURE;

    private MinibossEncounters() {
    }

    /** FEATURE registry phase. */
    public static void registerFeature() {
        ENCOUNTER_FEATURE = Registry.register(Registries.FEATURE, FEATURE_ID, new MinibossEncounterFeature());
    }

    /** Add each placed feature to its preferred biomes (run from common deferred init). */
    public static void injectBiomes() {
        for (EncounterType type : EncounterType.values()) {
            RegistryKey<PlacedFeature> placedKey = RegistryKey.of(RegistryKeys.PLACED_FEATURE, RPGMinibosses.id(type.id()));
            TagKey<net.minecraft.world.biome.Biome> biomeTag =
                    TagKey.of(RegistryKeys.BIOME, RPGMinibosses.id("has_encounter/" + type.id()));
            BiomeModifications.addFeature(BiomeSelectors.tag(biomeTag), GenerationStep.Feature.SURFACE_STRUCTURES, placedKey);
        }
    }
}

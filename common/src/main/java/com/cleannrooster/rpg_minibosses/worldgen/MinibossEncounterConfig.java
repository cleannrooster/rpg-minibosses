package com.cleannrooster.rpg_minibosses.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.world.gen.feature.FeatureConfig;

/** Single-field config selecting which {@link EncounterType} vignette the feature should build. */
public record MinibossEncounterConfig(EncounterType encounter) implements FeatureConfig {
    public static final Codec<MinibossEncounterConfig> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            EncounterType.CODEC.fieldOf("encounter").forGetter(MinibossEncounterConfig::encounter)
    ).apply(instance, MinibossEncounterConfig::new));
}

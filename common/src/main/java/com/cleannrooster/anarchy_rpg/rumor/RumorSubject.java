package com.cleannrooster.anarchy_rpg.rumor;

import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Something real a rumor can be about, found by a {@link RumorSources} source: where it is and what to call
 * it. {@code key} identifies it in each player's memory of what they have been told; {@code player} is the
 * subject's UUID when it is a player.
 */
public record RumorSubject(RumorKind kind, String key, Text name, RegistryKey<World> dimension, Vec3d position,
                           @Nullable UUID player) {
}

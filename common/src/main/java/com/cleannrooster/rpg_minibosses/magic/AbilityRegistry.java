package com.cleannrooster.rpg_minibosses.magic;

import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Every {@link AbilitySpec} built so far, by id. Specs register themselves from their builder, so a debug
 * command (or, later, a cue packet) can resolve one from its id alone.
 */
public final class AbilityRegistry {
    private static final Map<Identifier, AbilitySpec> SPECS = new LinkedHashMap<>();

    private AbilityRegistry() {
    }

    static void register(AbilitySpec spec) {
        // Brains are constructed per mob, so the same spec id arrives many times; the latest wins, and they
        // are equal in practice.
        SPECS.put(spec.id(), spec);
    }

    public static @Nullable AbilitySpec get(Identifier id) {
        return SPECS.get(id);
    }

    public static Collection<AbilitySpec> all() {
        return Collections.unmodifiableCollection(SPECS.values());
    }
}

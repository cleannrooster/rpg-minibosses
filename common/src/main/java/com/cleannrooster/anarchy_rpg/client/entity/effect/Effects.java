package com.cleannrooster.anarchy_rpg.client.entity.effect;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;

/**
 * Status effects that exist with or without Spell Engine. Effects only reachable through Spell Engine
 * content (Forsaken Magus, unique armor set bonuses) live in
 * {@code compat.spellengine.SpellEngineEffects}; they construct {@link Entry} instances too, so they join
 * {@link #entries} and are registered by the same {@link #register()} pass.
 */
public class Effects {
    private static final ArrayList<Entry> entries = new ArrayList<>();
    public static class Entry {
        public final Identifier id;
        public final StatusEffect effect;
        public RegistryEntry<StatusEffect> registryEntry;
        public Entry(String name, StatusEffect effect) {
            this.id = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, name);
            this.effect = effect;
            entries.add(this);
        }
        public void register() {
            registryEntry = Registry.registerReference(Registries.STATUS_EFFECT, id, effect);
        }
        public Identifier modifierId() {
            return Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "effect." + id.getPath());
        }
    }

    public static final Entry PETRIFIED = new Entry("petrified",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000));

    public static void register() {
        for (var entry: entries) {
            entry.register();
        }
    }
}

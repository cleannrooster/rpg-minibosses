package com.cleannrooster.rpg_minibosses.client.entity.effect;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.spell_engine.api.effect.Synchronized;

import java.util.ArrayList;

public class Effects {
    private static final ArrayList<Entry> entries = new ArrayList<>();
    public static class Entry {
        public final Identifier id;
        public final StatusEffect effect;
        public RegistryEntry<StatusEffect> registryEntry;

        public Entry(String name, StatusEffect effect) {
            this.id = Identifier.of(RPGMinibosses.MOD_ID, name);
            this.effect = effect;
            entries.add(this);
        }

        public void register() {
            registryEntry = Registry.registerReference(Registries.STATUS_EFFECT, id, effect);
        }

        public Identifier modifierId() {
            return Identifier.of(RPGMinibosses.MOD_ID, "effect." + id.getPath());
        }
    }


    public static final Entry FEATHER = new Entry("feather",
            new Feather(StatusEffectCategory.BENEFICIAL, 0xff0000));
    public static final Entry PETRIFIED = new Entry("petrified",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000));
    public static final Entry DARK_MATTER = new Entry("dark_matter",
            new DarkMatter(StatusEffectCategory.HARMFUL, 0xff0000));

    public static final Entry SHRAPNEL = new Entry("shrapnel",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000));
    public static final Entry ARCTICARMOR = new Entry("arctic_armor",
            new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff0000));
    public static final Entry MAGUS_BARRIER = new Entry("magus_barrier",
            new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff0000));
    public static void register() {

        for (var entry: entries) {
            entry.register();
            Synchronized.configure(entry.effect, true);

        }
    }
}
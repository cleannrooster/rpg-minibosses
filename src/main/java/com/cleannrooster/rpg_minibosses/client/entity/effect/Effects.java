package com.cleannrooster.rpg_minibosses.client.entity.effect;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import net.spell_engine.api.effect.Synchronized;
import net.spell_engine.api.entity.SpellEngineAttributes;
import net.spell_engine.fx.SpellEngineParticles;
import net.spell_power.api.SpellPowerMechanics;

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
        public Entry(String name, StatusEffect effect, boolean shouldSync) {
            this.id = Identifier.of(RPGMinibosses.MOD_ID, name);
            this.effect = effect;
            entries.add(this);

            if(shouldSync) {
                Synchronized.configure(effect, true);
            }

        }

        public void register() {
            registryEntry = Registry.registerReference(Registries.STATUS_EFFECT, id, effect);
        }

        public Identifier modifierId() {
            return Identifier.of(RPGMinibosses.MOD_ID, "effect." + id.getPath());
        }
    }


    public static final Entry FEATHER = new Entry("feather",
            new Feather(StatusEffectCategory.BENEFICIAL, 0xff0000),true);
    public static final Entry PETRIFIED = new Entry("petrified",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000));
    public static final Entry DARK_MATTER = new Entry("dark_matter",
            new DarkMatter(StatusEffectCategory.HARMFUL, 0xff0000));

    public static final Entry SHRAPNEL = new Entry("shrapnel",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000));
    public static final Entry ARCTICARMOR = new Entry("arctic_armor",
            new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff0000));
    public static final Entry FOX = new Entry("foxshade",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000)
                    .addAttributeModifier(SpellEngineAttributes.EVASION_CHANCE.entry,Identifier.of(RPGMinibosses.MOD_ID,"foxshade"),-1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final Entry SAVANT = new Entry("savant",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000)
                    .addAttributeModifier(SpellPowerMechanics.CRITICAL_DAMAGE.attributeEntry,Identifier.of(RPGMinibosses.MOD_ID,"savant"),-1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final Entry DESPOT = new Entry("despot",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000)
                    .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,Identifier.of(RPGMinibosses.MOD_ID,"despot"),-0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final Entry KINTSUGI = new Entry("kintsugi",
            new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff0000)
                    .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,Identifier.of(RPGMinibosses.MOD_ID,"kintsugi"),-0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));

    public static final Entry MAGUS_BARRIER = new Entry("magus_barrier",
            new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff0000)
                    .addAttributeModifier(SpellEngineAttributes.DAMAGE_TAKEN.entry,Identifier.of(RPGMinibosses.MOD_ID,"magus_barrier"),-0.95, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static void register() {

        for (var entry: entries) {
            entry.register();

        }
    }
}
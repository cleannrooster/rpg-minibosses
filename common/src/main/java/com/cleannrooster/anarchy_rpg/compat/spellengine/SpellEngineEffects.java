package com.cleannrooster.anarchy_rpg.compat.spellengine;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.client.entity.effect.CustomEffect;
import com.cleannrooster.anarchy_rpg.client.entity.effect.Effects;
import com.cleannrooster.anarchy_rpg.compat.spellengine.effect.DarkMatter;
import com.cleannrooster.anarchy_rpg.compat.spellengine.effect.Feather;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectCategory;
import net.minecraft.util.Identifier;
import net.spell_engine.api.effect.Synchronized;
import net.spell_engine.api.entity.SpellEngineAttributes;
import net.spell_power.api.SpellPowerMechanics;

/**
 * Effects that only Spell Engine content can apply: Forsaken Magus's kit and the unique armor set
 * bonuses (applied by their spell JSONs). Each is an {@link Effects.Entry}, so it joins the core entry
 * list as long as this class is initialized before {@link Effects#register()} runs — see
 * {@link SpellEngineContent#declareEffects()}.
 */
public final class SpellEngineEffects {
    private SpellEngineEffects() {
    }

    public static final Effects.Entry FEATHER = synced(new Effects.Entry("feather",
            new Feather(StatusEffectCategory.BENEFICIAL, 0xff0000)));
    public static final Effects.Entry DARK_MATTER = new Effects.Entry("dark_matter",
            new DarkMatter(StatusEffectCategory.HARMFUL, 0xff0000));
    public static final Effects.Entry ARCTICARMOR = new Effects.Entry("arctic_armor",
            new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff0000));
    public static final Effects.Entry FOX = new Effects.Entry("foxshade",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000)
                    .addAttributeModifier(SpellEngineAttributes.EVASION_CHANCE.entry,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"foxshade"),-1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final Effects.Entry SAVANT = new Effects.Entry("savant",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000)
                    .addAttributeModifier(SpellPowerMechanics.CRITICAL_DAMAGE.attributeEntry,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"savant"),-1, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final Effects.Entry DESPOT = new Effects.Entry("despot",
            new CustomEffect(StatusEffectCategory.HARMFUL, 0xff0000)
                    .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"despot"),-0.5F, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final Effects.Entry KINTSUGI = new Effects.Entry("kintsugi",
            new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff0000)
                    .addAttributeModifier(EntityAttributes.GENERIC_ATTACK_DAMAGE,Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,"kintsugi"),-0.5, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    public static final Effects.Entry MAGUS_BARRIER = new Effects.Entry("magus_barrier",
            new CustomEffect(StatusEffectCategory.BENEFICIAL, 0xff0000));

    private static Effects.Entry synced(Effects.Entry entry) {
        Synchronized.configure(entry.effect, true);
        return entry;
    }

    /** Forces class initialization, which is what adds the entries above to the core list. */
    static void declare() {
    }
}

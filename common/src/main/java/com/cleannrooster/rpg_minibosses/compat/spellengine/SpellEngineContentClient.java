package com.cleannrooster.rpg_minibosses.compat.spellengine;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.DarkMatterView;
import com.cleannrooster.rpg_minibosses.compat.SpellEngineCompatClient;
import com.cleannrooster.rpg_minibosses.compat.spellengine.client.FeatherRenderer;
import com.cleannrooster.rpg_minibosses.compat.spellengine.client.armor.JuggernautArmorRenderer;
import com.cleannrooster.rpg_minibosses.compat.spellengine.client.armor.ThiefArmorRenderer;
import com.cleannrooster.rpg_minibosses.compat.spellengine.client.armor.UniqueArmorRenderer;
import com.cleannrooster.rpg_minibosses.compat.spellengine.client.armor.WhisperingIceRenderer;
import com.cleannrooster.rpg_minibosses.compat.spellengine.item.Armors;
import com.cleannrooster.rpg_minibosses.compat.spellengine.item.CompatArmors;
import com.cleannrooster.rpg_minibosses.compat.spellengine.magus.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.platform.Platform;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererRegistry;
import mod.azure.azurelib.common.render.item.AzItemRendererRegistry;
import dev.architectury.registry.item.ItemPropertiesRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.rpg_series.item.Armor;

import java.util.function.Supplier;

/** Client setup for the Spell Engine content. Run by {@link SpellEngineCompatClient#init()}. */
public final class SpellEngineContentClient implements Runnable {
    @Override
    public void run() {
        CustomModelStatusEffect.register(SpellEngineEffects.FEATHER.effect, new FeatherRenderer());

        ItemPropertiesRegistry.register(SpellEngineContent.LAVOSHORN, RPGMinibosses.id("tooting"), (stack, world, entity, seed) ->
                entity != null && entity.isUsingItem() && entity.getActiveItem() == stack ? 1.0F : 0.0F);

        DarkMatterView.install(new DarkMatterView.Source() {
            @Override
            public boolean isActive(PlayerEntity viewer) {
                return viewer.hasStatusEffect(SpellEngineEffects.DARK_MATTER.registryEntry);
            }

            @Override
            public boolean isExempt(Entity entity) {
                return entity instanceof MagusPrimeEntity;
            }
        });

        AzItemRendererRegistry.register(Armors.whispering_ice.item(), WhisperingIceRenderer::new);
        if (Platform.isModLoaded("extraspellattributes")) {
            registerArmorRenderer(CompatArmors.juggernautArmor, JuggernautArmorRenderer::new);
            registerArmorRenderer(CompatArmors.tricksterArmor, ThiefArmorRenderer::new);
        }

        registerArmorRenderer(Armors.despotArmor, UniqueArmorRenderer::despot);
        registerArmorRenderer(Armors.foxArmor, UniqueArmorRenderer::foxshade);
        registerArmorRenderer(Armors.kintsugiArmor, UniqueArmorRenderer::kintsugi);
        registerArmorRenderer(Armors.sanguine_red, UniqueArmorRenderer::sanguine_fire);
        registerArmorRenderer(Armors.sanguine_blue, UniqueArmorRenderer::sanguine_frost);
        registerArmorRenderer(Armors.sanguine_purple, UniqueArmorRenderer::sanguine_arcane);
    }

    private static void registerArmorRenderer(Armor.Set set, Supplier<AzArmorRenderer> armorRendererSupplier) {
        AzArmorRendererRegistry.register(armorRendererSupplier, set.head, set.chest, set.legs, set.feet);
    }
}

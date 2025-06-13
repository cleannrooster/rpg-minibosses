package com.cleannrooster.rpg_minibosses.item;

import com.cleannrooster.rpg_minibosses.client.armor.renderer.AbberrathRenderer;
import com.cleannrooster.rpg_minibosses.client.armor.renderer.WhisperingIceRenderer;
import mod.azure.azurelib.common.api.common.animatable.GeoItem;
import mod.azure.azurelib.common.internal.client.RenderProvider;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.BuiltinModelItemRenderer;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.*;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.spell_engine.api.item.armor.ConfigurableAttributes;
import net.spell_engine.api.item.weapon.SpellWeaponItem;
import net.spell_engine.api.item.weapon.StaffItem;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.List;
import java.util.function.Consumer;

public class WhisperingIceStaff extends StaffItem implements GeoItem {
    public WhisperingIceStaff(ToolMaterial material, Settings settings) {
        super(material, settings);
    }
    public SpellSchool school = SpellSchools.FROST;

    public SpellSchool getMagicSchool() {
        return school;
    }

    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);




    @Override
    public void createRenderer(Consumer<RenderProvider> consumer) {
        consumer.accept(new RenderProvider() {
            private WhisperingIceRenderer renderer = new WhisperingIceRenderer();

            @Override
            public BuiltinModelItemRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }



    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("text.rpg-minibosses.whispering_ice").formatted(Formatting.DARK_RED));
        super.appendTooltip(stack, context, tooltip, type);

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}

package com.cleannrooster.rpg_minibosses.item;

import com.cleannrooster.rpg_minibosses.client.armor.renderer.AbberrathRenderer;
import mod.azure.azurelib.common.api.common.animatable.GeoItem;
import mod.azure.azurelib.common.internal.client.RenderProvider;
import mod.azure.azurelib.common.internal.common.util.AzureLibUtil;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ArmorMaterial;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.spell_engine.api.item.armor.Armor;
import net.spell_power.api.SpellSchool;
import net.spell_power.api.SpellSchools;

import java.util.List;
import java.util.function.Consumer;

public class AbberrathArmor extends Armor.CustomItem implements GeoItem {
    public SpellSchool school = SpellSchools.FIRE;
    public AbberrathArmor(RegistryEntry<ArmorMaterial> material, ArmorItem.Type type, Item.Settings settings, SpellSchool school) {
        super(material, type, settings);
    }

    public SpellSchool getMagicSchool() {
        return school;
    }

    private final AnimatableInstanceCache cache = AzureLibUtil.createInstanceCache(this);




    @Override
    public void createRenderer(Consumer<RenderProvider> consumer) {
        consumer.accept(new RenderProvider() {
            private AbberrathRenderer renderer;

            @Override
            public BipedEntityModel<LivingEntity> getHumanoidArmorModel(LivingEntity livingEntity, ItemStack itemStack, EquipmentSlot equipmentSlot, BipedEntityModel<LivingEntity> original) {
                if (this.renderer == null) {
                    this.renderer = new AbberrathRenderer();
                }
                this.renderer.prepForRender(livingEntity, itemStack, equipmentSlot, original);
                return this.renderer;
            }
        });
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        tooltip.add(Text.translatable("text.rpg-minibosses.abberraths_hooves").formatted(Formatting.DARK_RED));

        super.appendTooltip(stack, context, tooltip, type);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {

    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

}

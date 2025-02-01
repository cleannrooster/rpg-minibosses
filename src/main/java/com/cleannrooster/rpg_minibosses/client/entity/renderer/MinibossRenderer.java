package com.cleannrooster.rpg_minibosses.client.entity.renderer;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import mod.azure.azurelib.common.api.client.renderer.DynamicGeoEntityRenderer;
import mod.azure.azurelib.common.internal.common.cache.object.BakedGeoModel;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.effect.Synchronized;
import net.spell_power.api.SpellSchools;

import java.util.List;

public class MinibossRenderer<T extends MinibossEntity, M extends BipedEntityModel<T>> extends DynamicGeoEntityRenderer<T> {

    GeoModel<T> model;
    public MinibossRenderer(EntityRendererFactory.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
        addRenderLayer(new RenderLayerItemMinibossEntity(this));

        this.model = (GeoModel<T>) model;
    }

    @Override
    public void actuallyRender(MatrixStack poseStack, T animatable, BakedGeoModel model, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        for(Synchronized.Effect effect: Synchronized.effectsOf(animatable)){
            if(CustomModelStatusEffect.rendererOf(effect.effect()) != null) {
                CustomModelStatusEffect.rendererOf(effect.effect()).renderEffect(effect.amplifier(), animatable, partialTick, poseStack, bufferSource, packedLight);
            }
        }
    }

    @Override
    protected void applyRotations(T animatable, MatrixStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        List<PlayerEntity> list =  animatable.getWorld().getTargets(PlayerEntity.class, TargetPredicate.DEFAULT,animatable,animatable.getBoundingBox().expand(12));


        super.applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick);
    }

    public Identifier getTextureLocation(T p_114891_) {
        Identifier identifier = model.getTextureResource(p_114891_);
        if(p_114891_.hasStatusEffect(Effects.PETRIFIED.registryEntry)) {
            identifier = Identifier.of("minecraft", "textures/" + "stone" + ".png");
        }

        return identifier;
    }


}
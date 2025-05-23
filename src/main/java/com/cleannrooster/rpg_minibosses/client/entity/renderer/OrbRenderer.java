package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.client.entity.model.OrbModel;
import com.cleannrooster.rpg_minibosses.entity.OrbEntity;
import com.cleannrooster.spellblades.client.entity.CycloneModel;
import mod.azure.azurelib.common.api.client.renderer.GeoEntityRenderer;
import mod.azure.azurelib.common.internal.common.cache.object.BakedGeoModel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;

public class OrbRenderer<T extends OrbEntity> extends GeoEntityRenderer<OrbEntity> {

    public OrbRenderer(EntityRendererFactory.Context context) {
        super(context, new OrbModel<>());

    }

    @Override
    public void preRender(MatrixStack poseStack, OrbEntity animatable, BakedGeoModel model, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        this.scaleHeight = 4;
        this.scaleWidth = 4;
        if(animatable.age < 40 ){

            this.scaleHeight = (float) (4*(0.025*(animatable.age+partialTick)));
            this.scaleWidth = (float) (4*(0.025*(animatable.age+partialTick)));

        }
        if(animatable.age > 200 ){

           this.scaleHeight = (float) (4-4*(0.025*(animatable.age+partialTick-200)));
            this.scaleWidth = (float) (4-4*(0.025*(animatable.age+partialTick-200)));

        }
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
    }



}
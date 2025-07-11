package com.cleannrooster.rpg_minibosses.client.entity.renderer;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.TemplarEntity;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import mod.azure.azurelib.common.api.client.renderer.DynamicGeoEntityRenderer;
import mod.azure.azurelib.common.internal.client.util.RenderUtils;
import mod.azure.azurelib.common.internal.common.cache.object.BakedGeoModel;
import mod.azure.azurelib.common.internal.common.cache.object.GeoBone;
import mod.azure.azurelib.core.object.Color;
import net.fabricmc.fabric.impl.client.indigo.renderer.helper.ColorHelper;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.effect.Synchronized;
import net.spell_power.api.SpellSchools;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

public class MinibossRenderer<T extends MinibossEntity, M extends BipedEntityModel<T>> extends DynamicGeoEntityRenderer<T> {

    GeoModel<T> model;

    public MinibossRenderer(EntityRendererFactory.Context renderManager, GeoModel<T> model) {
        super(renderManager, model);
        addRenderLayer(new RenderLayerItemMinibossEntity(this));
        addRenderLayer(new RenderBackLayerItemMinibossEntity(this));
        this.model = (GeoModel<T>) model;
    }

    @Override
    public Color getRenderColor(T animatable, float partialTick, int packedLight) {
        if(animatable.getIndicator()<20) {

            return Color.ofHSB(1F,((float)Math.sin((Math.PI*((float)animatable.getIndicator()+partialTick))/20F)),1);
        }
        return super.getRenderColor(animatable, partialTick, packedLight);
    }

    @Override
    public void renderRecursively(MatrixStack poseStack, T animatable, GeoBone bone, RenderLayer renderType, VertexConsumerProvider bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay, int colour) {
        var bool = ((bone.getName().equals("rightArm")));
        var bool2 = ((bone.getName().equals("leftArm")));
        var bool3 = animatable instanceof JuggernautEntity || animatable instanceof TemplarEntity;

        if (bone.getName().equals("head") || bool || bool2) {
            poseStack.push();
            RenderUtils.translateMatrixToBone(poseStack, bone);
            RenderUtils.translateToPivotPoint(poseStack, bone);
            RenderUtils.rotateMatrixAroundBone(poseStack, bone);
            RenderUtils.scaleMatrixForBone(poseStack, bone);

            if (!bool) {
                poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.clamp(animatable.bodyYaw- animatable.getYaw(partialTick), -180, 180)));
                poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-animatable.getPitch(partialTick)));

            } else if (!bool3 && !(animatable instanceof ArtilleristEntity && ((ArtilleristEntity) animatable).getDataTracker().get(ArtilleristEntity.CHARGING))) {

                poseStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(MathHelper.clamp( -animatable.bodyYaw+animatable.getYaw(partialTick), -180, 180)));

            }

            if (bone.isTrackingMatrices()) {
                Matrix4f poseState = new Matrix4f(poseStack.peek().getPositionMatrix());
                Matrix4f localMatrix = RenderUtils.invertAndMultiplyMatrices(poseState, this.entityRenderTranslations);

                bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
                bone.setLocalSpaceMatrix(
                        RenderUtils.translateMatrix(localMatrix, getPositionOffset(this.animatable, 1).toVector3f())
                );
                bone.setWorldSpaceMatrix(
                        RenderUtils.translateMatrix(new Matrix4f(localMatrix), this.animatable.getPos().toVector3f())
                );
            }

            RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

            this.textureOverride = getTextureOverrideForBone(bone, this.animatable, partialTick);
            Identifier texture = this.textureOverride == null
                    ? getTexture(this.animatable)
                    : this.textureOverride;
            RenderLayer renderTypeOverride = getRenderTypeOverrideForBone(
                    bone,
                    this.animatable,
                    texture,
                    bufferSource,
                    partialTick
            );

            if (texture != null && renderTypeOverride == null)
                renderTypeOverride = getRenderType(this.animatable, texture, bufferSource, partialTick);

            if (renderTypeOverride != null)
                buffer = bufferSource.getBuffer(renderTypeOverride);

            if (
                    !boneRenderOverride(
                            poseStack,
                            bone,
                            bufferSource,
                            buffer,
                            partialTick,
                            packedLight,
                            packedOverlay,
                            colour
                    )
            )
                super.renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);

            if (renderTypeOverride != null)
                buffer = bufferSource.getBuffer(
                        getRenderType(this.animatable, getTexture(this.animatable), bufferSource, partialTick)
                );

            if (!isReRender)
                applyRenderLayersForBone(
                        poseStack,
                        animatable,
                        bone,
                        renderType,
                        bufferSource,
                        buffer,
                        partialTick,
                        packedLight,
                        packedOverlay
                );

            super.renderChildBones(
                    poseStack,
                    animatable,
                    bone,
                    renderType,
                    bufferSource,
                    buffer,
                    isReRender,
                    partialTick,
                    packedLight,
                    packedOverlay,
                    colour
            );

            poseStack.pop();
        }
        else {


            super.renderRecursively(poseStack, animatable, bone, renderType, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay, colour);
        }


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
    public boolean hasLabel(T animatable) {
        return animatable.notPetrified();
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
package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzLayerRenderer;
import mod.azure.azurelib.common.render.AzRendererPipeline;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.entity.AzEntityModelRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererPipeline;
import mod.azure.azurelib.common.util.client.RenderUtils;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

import java.util.UUID;

public class MinibossModelRenderer extends AzEntityModelRenderer<MinibossEntity> {




    public MinibossModelRenderer(AzEntityRendererPipeline<MinibossEntity> minibossEntityAzRendererPipeline, AzLayerRenderer<UUID,MinibossEntity> minibossEntityAzLayerRenderer) {
        super(minibossEntityAzRendererPipeline, minibossEntityAzLayerRenderer);

    }

    public MinibossModelRenderer(AzRendererPipeline<UUID,MinibossEntity> minibossEntityAzRendererPipeline, AzLayerRenderer<UUID,MinibossEntity> minibossEntityAzLayerRenderer) {
        super((AzEntityRendererPipeline<MinibossEntity>) minibossEntityAzRendererPipeline, minibossEntityAzLayerRenderer);

    }

    @Override
    public void renderRecursively(AzRendererPipelineContext<UUID,MinibossEntity> context, AzBone bone, boolean isReRender) {
        var buffer = context.vertexConsumer();
        var bufferSource = context.multiBufferSource();
        var entity = context.animatable();
        var poseStack = context.poseStack();

        poseStack.push();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);

        if(bone.getName().contains("head")){
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.clamp(entity.bodyYaw - entity.getYaw(context.partialTick()), -180, 180)));


            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-entity.getPitch(context.partialTick())));


        }
        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.peek().getPositionMatrix());
            Matrix4f localMatrix = RenderUtils.invertAndMultiplyMatrices(
                    poseState,
                    poseStack.peek().getPositionMatrix()
            );

            bone.setModelSpaceMatrix(
                    RenderUtils.invertAndMultiplyMatrices(poseState, new Matrix4f())
            );
            bone.setLocalSpaceMatrix(
                    RenderUtils.translateMatrix(
                            localMatrix,
                            entityRendererPipeline.getRenderer().getPositionOffset(entity, 1).toVector3f()
                    )
            );
            bone.setWorldSpaceMatrix(
                    RenderUtils.translateMatrix(new Matrix4f(localMatrix), entity.getPos().toVector3f())
            );
        }

        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        context.setVertexConsumer(getOrRefreshRenderBuffer(isReRender, context, bone));

        if (
                !boneRenderOverride(
                        poseStack,
                        bone,
                        bufferSource,
                        buffer,
                        context.partialTick(),
                        context.packedLight(),
                        context.packedOverlay(),
                        context.renderColor()
                )
        )
            super.renderCubesOfBone(context, bone);

        if (!isReRender) {
            layerRenderer.applyRenderLayersForBone(context, bone);
        }

        renderChildBones(context, bone, isReRender);

        poseStack.pop();

    }
}

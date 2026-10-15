package com.cleannrooster.anarchy_rpg.client.entity.renderer;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
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
    /*
     * Procedural sitting pose, for an NPC riding anything (a boat, a horse, a minecart). Drawn here rather than
     * as a clip, so it layers over whatever is playing and needs nothing in the animation file.
     *
     * Directions follow the head-pitch code below, which tilts the head forward with a negative X rotation:
     * forward is -Z in this space, so a leg hanging from its hip swings forward with a positive X rotation.
     * The legs are children of "body" and pivot at the hip, so only they move; the whole model is dropped so the
     * hips, not the feet, rest on the seat.
     */
    private static final float SIT_LEG_PITCH = 82.0F;
    /** Outward splay per leg, so they do not sit pressed together. */
    private static final float SIT_LEG_SPLAY = 6.0F;
    /** How far the whole model drops while seated, in blocks: about the hip height above the feet, less the seat. */
    private static final float SIT_DROP = 0.45F;
    /** How far the head may turn from the body, in degrees; about vanilla's own limit. */
    private static final float HEAD_TURN_LIMIT = 75.0F;





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

        boolean seated = entity.hasVehicle();
        poseStack.push();
        if (seated && bone.getParent() == null) {
            poseStack.translate(0.0F, -SIT_DROP, 0.0F);
        }
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);

        if(bone.getName().contains("head")){
            // Wrapped before clamping: an unwrapped difference past 180 (a body turned by a boat, say) would clamp
            // to 180 and face the head backwards. Limited to about vanilla's neck reach.
            float neck = MathHelper.wrapDegrees(entity.bodyYaw - entity.getYaw(context.partialTick()));
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.clamp(neck, -HEAD_TURN_LIMIT, HEAD_TURN_LIMIT)));


            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-entity.getPitch(context.partialTick())));


        }
        if (seated && (bone.getName().equals("leftLeg") || bone.getName().equals("rightLeg"))) {
            // Splay first, then swing forward: applied the other way round the splay would only spin the leg.
            // The left leg sits at +X, so outward for it is a negative turn about Y.
            float splay = bone.getName().equals("leftLeg") ? -SIT_LEG_SPLAY : SIT_LEG_SPLAY;
            poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(splay));
            poseStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(SIT_LEG_PITCH));
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

package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.OrbEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

public class OrbRenderer extends AzEntityRenderer<OrbEntity> {

    private static final Identifier TEXTURE = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/item/orb_black.png");
    private static final Identifier GEO = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/orb.geo.json");

    public OrbRenderer(EntityRendererFactory.Context context) {
        super(
            AzEntityRendererConfig.<OrbEntity>builder(GEO, TEXTURE)
                .setAnimatorProvider(OrbAnimationProvider::new)
                .build(),
            context
        );
    }

    @Override
    public void render(OrbEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        float scale = 4.0f;

            scale = (float) (4.0 * (0.025 * (Math.min(40,entity.age) + partialTick)));

        poseStack.push();
        poseStack.scale(scale+2*entity.getSize(), (float) scale+2*entity.getSize(), (float) scale+2*entity.getSize());
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pop();
    }
}

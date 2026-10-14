package com.cleannrooster.anarchy_rpg.client.entity.renderer;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.client.entity.effect.Effects;
import com.cleannrooster.anarchy_rpg.entity.GeminiEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;

public class GeminiRenderer extends AzEntityRenderer<GeminiEntity> {

    public static final Identifier FIRE_TEXTURE = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/gemini.png");
    public static final Identifier FROST_TEXTURE = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/gemini_blue.png");
    public static final Identifier GEO = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/gemini.geo.json");

    public GeminiRenderer(EntityRendererFactory.Context context, Identifier texture) {
        super(
            AzEntityRendererConfig.<GeminiEntity>builder(GEO, texture)
                .setAnimatorProvider(GeminiAnimationProvider::new)
                .build(),
            context
        );
    }

    public Identifier getTextureLocation(GeminiEntity entity) {
        if (entity.hasStatusEffect(Effects.PETRIFIED.registryEntry)) {
            return Identifier.of("minecraft", "textures/stone.png");
        }
        return entity.school == com.cleannrooster.anarchy_rpg.magic.MagicSchool.FIRE ? FIRE_TEXTURE : FROST_TEXTURE;
    }

    @Override
    public void render(GeminiEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        poseStack.push();
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(MathHelper.clamp(entity.bodyYaw - entity.getYaw(partialTick), -180, 180)));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.pop();
    }
}

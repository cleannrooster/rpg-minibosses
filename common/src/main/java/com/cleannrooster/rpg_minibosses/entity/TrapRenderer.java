package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;

import java.util.Objects;

public class TrapRenderer extends AzEntityRenderer<TrapCleann> {

    private static final Identifier DEFAULT_LOCATION = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/traptexture.png");
    private static final Identifier GEO = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/trapmodel.geo.json");

    public TrapRenderer(EntityRendererFactory.Context context) {
        super(
            AzEntityRendererConfig.<TrapCleann>builder(GEO, TrapRenderer.DEFAULT_LOCATION)
                .setAnimatorProvider(TrapAnimator::new)
                .build(),
            context
        );
    }

    private static Identifier getTextureLocation(TrapCleann entity) {
        if (entity.age > 40 && entity.getWorld().getBlockState(entity.getBlockPos().down()).getBlock() != Blocks.AIR) {
            Sprite sprite = MinecraftClient.getInstance()
                    .getBakedModelManager()
                    .getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).getSprite(
                            MinecraftClient.getInstance().getBakedModelManager().getBlockModels()
                                    .getModel(entity.getWorld().getBlockState(entity.getBlockPos().down()))
                                    .getParticleSprite().getContents().getId());
            if (!Objects.equals(sprite.getContents().getId().getPath(), "missingno")) {
                return Identifier.of(sprite.getContents().getId().getNamespace(), "textures/" + sprite.getContents().getId().getPath() + ".png");
            }
        }
        return DEFAULT_LOCATION;
    }

    @Override
    public void render(TrapCleann entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        if (entity.age < 2) {
            return;
        }
        poseStack.scale(0.5F, 0.5F, 0.5F);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) -entity.getYaw() - 90));
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}

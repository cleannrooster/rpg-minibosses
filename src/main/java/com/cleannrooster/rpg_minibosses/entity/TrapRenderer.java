package com.cleannrooster.rpg_minibosses.entity;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import mod.azure.azurelib.renderer.GeoEntityRenderer;
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


public class TrapRenderer<T extends TrapCleann> extends GeoEntityRenderer<TrapCleann> {


    private static final Identifier DEFAULT_LOCATION = Identifier.of(RPGMinibosses.MOD_ID,"textures/mob/traptexture.png");



    public TrapRenderer(EntityRendererFactory.Context renderManager) {
        super(renderManager, new TrapModel<T>());

        //this.layerRenderers.add((GeoLayerRenderer<Reaver>) new GeoitemInHand<T,M>((IGeoRenderer<T>) this,renderManager.getItemInHandRenderer()));
    }



    @Override
    public void render(TrapCleann entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {

        if(entity.age < 2){
            return;
        }

        poseStack.scale(0.5F,0.5F,0.5F);
        poseStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) -entity.getYaw()-90));

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    public Identifier getTextureLocation(TrapCleann p_114891_) {
        Identifier identifier = DEFAULT_LOCATION;
        if(p_114891_.age > 40 && p_114891_.getWorld().getBlockState(p_114891_.getBlockPos().down()).getBlock() != Blocks.AIR)
        {
            Sprite sprite = MinecraftClient.getInstance()
                    .getBakedModelManager()
                    .getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE).getSprite(
                            MinecraftClient.getInstance().getBakedModelManager().getBlockModels().getModel(p_114891_.getWorld().getBlockState(p_114891_.getBlockPos().down())).getParticleSprite().getContents().getId());
            if(!Objects.equals(sprite.getContents().getId().getPath(), "missingno")) {
                identifier = Identifier.of(sprite.getContents().getId().getNamespace(), "textures/" + sprite.getContents().getId().getPath() + ".png");
            }
        }
        return identifier;
    }

}

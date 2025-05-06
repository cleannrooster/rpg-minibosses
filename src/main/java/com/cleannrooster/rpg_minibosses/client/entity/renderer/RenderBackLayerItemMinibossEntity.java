package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import mod.azure.azurelib.common.api.client.renderer.layer.BlockAndItemGeoLayer;
import mod.azure.azurelib.common.internal.client.renderer.GeoRenderer;
import mod.azure.azurelib.common.internal.common.cache.object.GeoBone;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;

public class RenderBackLayerItemMinibossEntity<T extends MinibossEntity> extends BlockAndItemGeoLayer<MinibossEntity> {
    public RenderBackLayerItemMinibossEntity(GeoRenderer<MinibossEntity> entityRendererIn) {
        super(entityRendererIn);
    }
    private static final String BACK_ITEM = "backItem";
    private static final String RIGHT_HAND = "itemBone";
    @Override
    protected ItemStack getStackForBone(GeoBone bone, MinibossEntity animatable) {
        // Retrieve the items in the entity's hands for the relevant bone

        return switch (bone.getName()) {
            case BACK_ITEM -> animatable.getOffHandStack();
            default -> null;
        };
    }



    // Do some quick render modifications depending on what the item is


}

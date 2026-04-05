package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.TemplarEntity;
import mod.azure.azurelib.common.model.AzBone;
import mod.azure.azurelib.common.render.AzRendererPipelineContext;
import mod.azure.azurelib.common.render.layer.AzBlockAndItemLayer;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.item.ItemStack;

import java.util.UUID;
import java.util.function.Function;

public class MinibossItemRenderer<T extends MinibossEntity> extends AzBlockAndItemLayer<UUID,MinibossEntity> {
    public MinibossItemRenderer(
    ) {
        super();

    }

    @Override
    protected ModelTransformationMode getTransformTypeForStack(AzBone bone, ItemStack stack, MinibossEntity animatable) {
        return ModelTransformationMode.THIRD_PERSON_RIGHT_HAND;
    }



    @Override
    protected void renderItemForBone(AzRendererPipelineContext<UUID,MinibossEntity> context, AzBone bone, ItemStack itemStack, MinibossEntity animatable) {
        if( (context.animatable() instanceof JuggernautEntity || context.animatable() instanceof TemplarEntity))
        {

            context.poseStack().scale(1.5f, 1.5f, 1.5f);
        }
        super.renderItemForBone(context, bone, itemStack, animatable);

    }



    @Override
    public void renderForBone(AzRendererPipelineContext context, AzBone bone) {

        super.renderForBone(context, bone);
    }

    @Override
    public ItemStack itemStackForBone(AzBone bone, MinibossEntity animatable) {
        return bone.getName().equals("itemBone") && !(animatable instanceof ArtilleristEntity artilleristEntity) ? ((MinibossEntity)animatable).getMainHandStack() : null;
    }


}

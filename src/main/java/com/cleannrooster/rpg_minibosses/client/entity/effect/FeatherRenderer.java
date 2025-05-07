package com.cleannrooster.rpg_minibosses.client.entity.effect;

import mod.azure.azurelib.util.RenderUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.ItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.spell_engine.api.render.CustomLayers;
import net.spell_engine.api.render.CustomModels;
import net.spell_engine.api.render.LightEmission;
import net.spell_engine.api.render.OrbitingEffectRenderer;

import java.util.Iterator;
import java.util.List;

import static com.cleannrooster.rpg_minibosses.RPGMinibosses.MOD_ID;

public class FeatherRenderer extends OrbitingEffectRenderer {
    private List<OrbitingEffectRenderer.Model> models;
    private float scale;
    private float horizontalOffset;
    private static final RenderLayer GLOWING_RENDER_LAYER;

    public FeatherRenderer() {
        super(List.of(new OrbitingEffectRenderer.Model(GLOWING_RENDER_LAYER, 				Identifier.of(MOD_ID, "projectile/feather")
        )), 1F, 1F);
        this.models =List.of( new OrbitingEffectRenderer.Model(GLOWING_RENDER_LAYER, 				Identifier.of(MOD_ID, "projectile/feather")));
        this.scale = 1F;
        this.horizontalOffset = 1F;
    }
    static {
        GLOWING_RENDER_LAYER = CustomLayers.spellEffect(LightEmission.GLOW, true);
    }

    private void renderModel(MatrixStack matrixStack, float scale, float verticalOffset, float horizontalOffset, float rotation, ItemRenderer itemRenderer, VertexConsumerProvider vertexConsumers, int light, LivingEntity livingEntity) {
        matrixStack.push();
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotation));
        matrixStack.translate(0.0F, verticalOffset, -horizontalOffset);
        matrixStack.scale(scale, scale, scale);
        Iterator var10 = this.models.iterator();

        while(var10.hasNext()) {
            OrbitingEffectRenderer.Model model = (OrbitingEffectRenderer.Model)var10.next();
            matrixStack.push();
            CustomModels.render(model.layer(), itemRenderer, model.modelId(), matrixStack, vertexConsumers, light, livingEntity.getId());
            matrixStack.pop();
        }

        matrixStack.pop();
    }
    public void renderEffect(int amplifier, LivingEntity livingEntity, float delta, MatrixStack matrixStack, VertexConsumerProvider vertexConsumers, int light) {
        matrixStack.push();
        float time = (float) RenderUtils.getCurrentTick() + delta;
        float initialAngle = time * 2.25F - 45.0F;
        float horizontalOffset =  this.horizontalOffset * livingEntity.getScaleFactor();

        float verticalOffset = livingEntity.getHeight() / 2.0F;
        ItemRenderer itemRenderer = MinecraftClient.getInstance().getItemRenderer();
        int stacks = amplifier + 1;
        float turnAngle = 360.0F / (float)stacks;

        for(int i = 0; i < stacks; ++i) {
            float angle = initialAngle + turnAngle * (float)i;

            this.renderModel(matrixStack, this.scale, verticalOffset+ (i % 2 == 0 ? 0.25F : -0.25F), horizontalOffset + (i % 2 == 0 ? 0.5F : 0), angle, itemRenderer, vertexConsumers, light, livingEntity);
        }

        matrixStack.pop();
    }

    public static record Model(RenderLayer layer, Identifier modelId) {
        public Model(RenderLayer layer, Identifier modelId) {
            this.layer = layer;
            this.modelId = modelId;
        }

        public RenderLayer layer() {
            return this.layer;
        }

        public Identifier modelId() {
            return this.modelId;
        }
    }
}

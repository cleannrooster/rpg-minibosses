package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.common.util.client.RenderUtils;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.effect.Synchronized;

public class MagusRenderer extends AzEntityRenderer<MagusPrimeEntity> {

    private static final Identifier DEFAULT_TEXTURE = Identifier.of(RPGMinibosses.MOD_ID, "textures/mob/magus_prime_texture.png");
    private static final Identifier CASTING_1 = Identifier.of(RPGMinibosses.MOD_ID, "textures/mob/magus_prime_texture_casting.png");
    private static final Identifier CASTING_2 = Identifier.of(RPGMinibosses.MOD_ID, "textures/mob/magus_prime_texture_casting_2.png");
    private static final Identifier PETRIFIED = Identifier.of(RPGMinibosses.MOD_ID, "textures/mob/petrified.png");
    private static final Identifier GEO = Identifier.of(RPGMinibosses.MOD_ID, "geo/magus_prime.geo.json");

    public MagusRenderer(EntityRendererFactory.Context context) {
        super(
            AzEntityRendererConfig.<MagusPrimeEntity>builder(GEO, DEFAULT_TEXTURE)
                .setAnimatorProvider(MagusPrimeAnimationProvider::new)
                .setModelRenderer(MagusModelRenderer::new)
                .build(),
            context
        );
    }


    public Identifier getTextureLocation(MagusPrimeEntity entity) {
        if (!entity.notPetrified()) {
            return PETRIFIED;
        }
        if (entity.getDataTracker().get(MagusPrimeEntity.CASTINGBOOL)) {
            return RenderUtils.getCurrentTick() % 10 <= 5 ? CASTING_1 : CASTING_2;
        }
        return DEFAULT_TEXTURE;
    }

    @Override
    public void render(MagusPrimeEntity entity, float entityYaw, float partialTick, MatrixStack poseStack, VertexConsumerProvider bufferSource, int packedLight) {
        if (entity.age <= 2) return;
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        try {
            for (Synchronized.Effect effect : Synchronized.effectsOf(entity)) {
                if (effect != null && effect.effect() != null && CustomModelStatusEffect.rendererOf(effect.effect()) != null) {
                    CustomModelStatusEffect.rendererOf(effect.effect()).renderEffect(effect.amplifier(), entity, partialTick, poseStack, bufferSource, packedLight);
                }
            }
        } catch (Exception ignored) {}
    }
}

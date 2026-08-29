package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.effect.Synchronized;

public class MagusRenderer extends AzEntityRenderer<MagusPrimeEntity> {

    private static final Identifier DEFAULT_TEXTURE = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/magus_prime_texture.png");
    /** Mouth open. */
    private static final Identifier CASTING_1 = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/magus_prime_texture_casting.png");
    /** Mouth open wide. */
    private static final Identifier CASTING_2 = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/magus_prime_texture_casting_2.png");
    private static final Identifier PETRIFIED = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/petrified.png");
    private static final Identifier GEO = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/magus_prime.geo.json");

    /**
     * The incantation. Mouth shapes are sequenced rather than alternated so the result reads as
     * speech instead of a flap: the wide frames land as stressed syllables and the closed frames as
     * the gaps between words. Kept deliberately short so it loops unnoticeably across a long cast.
     */
    private static final Identifier[] TALK_CYCLE = {
            CASTING_1, CASTING_2, CASTING_1, DEFAULT_TEXTURE,
            CASTING_2, CASTING_1, CASTING_2, CASTING_2,
            CASTING_1, DEFAULT_TEXTURE, CASTING_1, CASTING_2,
    };

    /** Ticks each mouth shape is held. Three is fast enough to read as talking, slow enough to see. */
    private static final int TALK_FRAME_TICKS = 3;

    public MagusRenderer(EntityRendererFactory.Context context) {
        super(
            // AzureLib resolves the texture through this config function every frame. It has no
            // renderer-side texture hook to override — a `getTextureLocation` method on this class
            // overrides nothing, compiles silently, and is never called, which is how the talking
            // stopped happening in the first place. Put per-entity texture logic HERE.
            AzEntityRendererConfig.<MagusPrimeEntity>builder(entity -> GEO, MagusRenderer::textureFor)
                .setAnimatorProvider(MagusPrimeAnimationProvider::new)
                .setModelRenderer(MagusModelRenderer::new)
                .build(),
            context
        );
    }

    /**
     * Petrified wins over everything; otherwise Magus mouths his incantation for exactly as long as
     * {@code CASTINGBOOL} is set, which the entity holds across the long casts only — the channel
     * and the heavy projectile/nova families. Quick casts are over in a few ticks and are left
     * mouth-closed on purpose, so an open mouth always means something big is coming.
     */
    private static Identifier textureFor(MagusPrimeEntity entity) {
        if (!entity.notPetrified()) {
            return PETRIFIED;
        }
        if (entity.getDataTracker().get(MagusPrimeEntity.CASTINGBOOL)) {
            // Driven off entity age rather than a global clock so the cycle starts from wherever the
            // entity is in its own life and pauses with it.
            return TALK_CYCLE[Math.floorMod(entity.age / TALK_FRAME_TICKS, TALK_CYCLE.length)];
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
                    CustomModelStatusEffect.rendererOf(effect.effect()).renderEffect(entity.getWorld().getTime(), effect.amplifier(), entity, partialTick, poseStack, bufferSource, packedLight);
                }
            }
        } catch (Exception ignored) {}
    }
}

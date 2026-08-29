package com.cleannrooster.rpg_minibosses.client.entity.renderer;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.ArchmageFireEntity;
import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.TemplarEntity;

import mod.azure.azurelib.common.render.entity.AzEntityRenderer;
import mod.azure.azurelib.common.render.entity.AzEntityRendererConfig;
import mod.azure.azurelib.common.render.layer.AzBlockAndItemLayer;
import mod.azure.azurelib.core.object.Color;
import net.fabricmc.fabric.impl.client.indigo.renderer.helper.ColorHelper;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.TargetPredicate;
import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.spell_engine.api.effect.CustomModelStatusEffect;
import net.spell_engine.api.effect.Synchronized;
import net.spell_power.api.SpellSchools;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.List;

public class MinibossRenderer extends AzEntityRenderer<MinibossEntity> {
    AzBlockAndItemLayer renderer = new AzBlockAndItemLayer<>();
    protected MinibossRenderer(AzEntityRendererConfig<MinibossEntity> config, EntityRendererFactory.Context context) {
        super(config, context);

    }
    public static final Identifier TEMPLAR_MODEL = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "geo/templarmob.geo.json"
    );
    public static final Identifier TEMPLAR_TEXTURE = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/templar.png"
    );
    public static final Identifier ROGUE_MODEL = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "geo/thiefmob.json"
    );
    public static final Identifier ROGUE_TEXTURE = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/thieftexture.png"
    );
    public static final Identifier JUGG_MODEL = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "geo/juggmob.json"
    );
    public static final Identifier JUGG_TEXTURE = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/juggtexture.png"
    );
    public static final Identifier FIREMAGE_MODEL = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "geo/archmagefire.json"
    );
    public static final Identifier FIREMAGE_TEXTURE = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/archmagetexturefire.png"
    );
    public static final Identifier MERCENARY_MODEL = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "geo/artmob.json"
    );
    public static final Identifier MERCENARY_TEXTURE = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/artillerist.png"
    );

    // ── Fire Mage incantation ────────────────────────────────────────────────
    //
    // Same treatment as Magus: the mouth is sequenced rather than alternated, so the wide frames
    // read as stressed syllables and the base texture's closed mouth as the gap between words.
    // These two files do not exist yet — until they are added the mage simply stays mouth-closed,
    // see talkTexturesPresent().
    public static final Identifier FIREMAGE_TALK_OPEN = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/archmagetexturefire_casting.png"
    );
    public static final Identifier FIREMAGE_TALK_WIDE = Identifier.of(
            RPGMinibosses.CONTENT_NAMESPACE, "textures/mob/archmagetexturefire_casting_2.png"
    );

    /** {@code null} means "use the mob's own texture", i.e. mouth closed. */
    private static final Identifier[] TALK_CYCLE = {
            FIREMAGE_TALK_OPEN, FIREMAGE_TALK_WIDE, FIREMAGE_TALK_OPEN, null,
            FIREMAGE_TALK_WIDE, FIREMAGE_TALK_OPEN, FIREMAGE_TALK_WIDE, FIREMAGE_TALK_WIDE,
            FIREMAGE_TALK_OPEN, null, FIREMAGE_TALK_OPEN, FIREMAGE_TALK_WIDE,
    };

    private static final int TALK_FRAME_TICKS = 3;

    /**
     * The mouth textures are art that does not exist in the repo yet. Rather than render the mage
     * with a missing-texture checkerboard every time it casts, fall back to its normal skin until
     * both files are present. Only consulted while a fire mage is actually mid-incantation, so this
     * is not a per-frame cost for the other four minibosses.
     */
    private static boolean talkTexturesPresent() {
        var resources = net.minecraft.client.MinecraftClient.getInstance().getResourceManager();
        return resources.getResource(FIREMAGE_TALK_OPEN).isPresent()
                && resources.getResource(FIREMAGE_TALK_WIDE).isPresent();
    }

    /**
     * AzureLib resolves the texture through this config function every frame — there is no
     * renderer-side hook to override, so per-entity texture logic belongs here.
     */
    private static Identifier textureFor(MinibossEntity entity, Identifier base) {
        if (entity instanceof ArchmageFireEntity mage && mage.isTalking() && talkTexturesPresent()) {
            Identifier frame = TALK_CYCLE[Math.floorMod(entity.age / TALK_FRAME_TICKS, TALK_CYCLE.length)];
            return frame == null ? base : frame;
        }
        return base;
    }

    public MinibossRenderer(EntityRendererFactory.Context context, Identifier model, Identifier texture) {
        super(
                AzEntityRendererConfig.<MinibossEntity>builder(
                                entity -> model, entity -> textureFor(entity, texture))
                        .setModelRenderer(MinibossModelRenderer::new)
                        .setAnimatorProvider(MinibossAnimationProvider::new) // Custom animator

                        .setDeathMaxRotation(180F) // Custom death rotation
                        .setShadowRadius(1.0F) // Sets a shadow radius
                        .setShadowRadius(exampleEntity -> 1.0F) // Sets a shadow radius with context

                        .setRenderType(RenderLayer.getEntityTranslucent(texture)) // Sets RenderType
                        // The render layer carries the texture, so it has to follow the same swap —
                        // changing only the texture provider would leave the mouth frames unused.
                        .setRenderType(entity -> RenderLayer.getEntityTranslucent(textureFor(entity, texture)))

                        .addRenderLayer(new MinibossItemRenderer<>()) // Add render layers
                      //  .setModelRenderer(ExampleCustomEntityModelRenderer::new) // Sets the Model Renderer of your render to the ExampleCustomEntityModelRenderer
                        //.setPipelineContext(ExampleEntityRendererPipelineContext::new) // Sets the Pipeline Context to the ExampleEntityRendererPipelineContext
                        .setPrerenderEntry(context2 -> {
                            // Insert code you want to run here

                            return context2;
                        }) // Pre-render hook
                        .setRenderEntry(context3 -> {


                            // Insert code you want to run here
                            return context3;
                        }) // Render hook
                        .setPostRenderEntry(context2 -> {
                            // Insert code you want to run here

                            return context2;
                        }) // Post-render hook
                        .setAlpha(exampleEntity -> 1.0F) // Alpha with context
                        .setAlpha(1.0F) // Alpha with just a value
                        .setScale(1.0F, 1.0F) // Scale for width and height
                        .setScale(1.0F) // Scale for width and height being the same
                        .setScale(exampleEntity -> 1.0F) // Scale for width and height being the same with context
                        .setScale(exampleEntity -> 1.0F, exampleEntity -> 1.0F) // Scale for width and height with context
                        .build(),
                context
        );
    }
}
package com.cleannrooster.rpg_minibosses.client.entity.renderer;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.client.entity.effect.Effects;
import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.TemplarEntity;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import mod.azure.azurelib.common.api.client.renderer.DynamicGeoEntityRenderer;
import mod.azure.azurelib.common.internal.client.util.RenderUtils;
import mod.azure.azurelib.common.internal.common.cache.object.BakedGeoModel;
import mod.azure.azurelib.common.internal.common.cache.object.GeoBone;
import mod.azure.azurelib.core.object.Color;
import mod.azure.azurelib.rewrite.render.entity.AzEntityRenderer;
import mod.azure.azurelib.rewrite.render.entity.AzEntityRendererConfig;
import net.fabricmc.fabric.impl.client.indigo.renderer.helper.ColorHelper;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.BipedEntityModel;
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

    protected MinibossRenderer(AzEntityRendererConfig<MinibossEntity> config, EntityRendererFactory.Context context) {
        super(config, context);
    }
    public static final Identifier TEMPLAR_MODEL = Identifier.of(
            RPGMinibosses.MOD_ID, "geo/templarmob.geo.json"
    );
    public static final Identifier TEMPLAR_TEXTURE = Identifier.of(
            RPGMinibosses.MOD_ID, "textures/mob/templar.png"
    );
    public static final Identifier ROGUE_MODEL = Identifier.of(
            RPGMinibosses.MOD_ID, "geo/thiefmob.json"
    );
    public static final Identifier ROGUE_TEXTURE = Identifier.of(
            RPGMinibosses.MOD_ID, "textures/mob/thieftexture.png"
    );
    public static final Identifier JUGG_MODEL = Identifier.of(
            RPGMinibosses.MOD_ID, "geo/juggmob.json"
    );
    public static final Identifier JUGG_TEXTURE = Identifier.of(
            RPGMinibosses.MOD_ID, "textures/mob/juggtexture.png"
    );
    public static final Identifier FIREMAGE_MODEL = Identifier.of(
            RPGMinibosses.MOD_ID, "geo/archmagefire.json"
    );
    public static final Identifier FIREMAGE_TEXTURE = Identifier.of(
            RPGMinibosses.MOD_ID, "textures/mob/archmagetexturefire.png"
    );
    public static final Identifier MERCENARY_MODEL = Identifier.of(
            RPGMinibosses.MOD_ID, "geo/artmob.json"
    );
    public static final Identifier MERCENARY_TEXTURE = Identifier.of(
            RPGMinibosses.MOD_ID, "textures/mob/artillerist.png"
    );



    public MinibossRenderer(EntityRendererFactory.Context context, Identifier model, Identifier texture) {
        super(
                AzEntityRendererConfig.<MinibossEntity>builder(model  ,texture)
                        .setModelRenderer(MinibossModelRenderer::new)
                        .setAnimatorProvider(MinibossAnimationProvider::new) // Custom animator
                        .setDeathMaxRotation(180F) // Custom death rotation
                        .setShadowRadius(1.0F) // Sets a shadow radius
                        .setShadowRadius(exampleEntity -> 1.0F) // Sets a shadow radius with context

                        .setRenderType(RenderLayer.getEntityTranslucent(texture)) // Sets RenderType
                        .setRenderType(exampleEntity -> RenderLayer.getEntityTranslucent(texture)) // Sets RenderType with context
                       // .addRenderLayer(new CustomEntityRenderLayer()) // Add render layers
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
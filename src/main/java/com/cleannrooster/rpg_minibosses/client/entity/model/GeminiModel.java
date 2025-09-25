package com.cleannrooster.rpg_minibosses.client.entity.model;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.GeminiEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellSchools;

public class GeminiModel<T extends GeminiEntity> extends GeoModel<T> implements ModelWithArms {
    private static final Identifier DEFAULT_LOCATION = Identifier.of(RPGMinibosses.MOD_ID,"textures/mob/gemini.png");
    private static final Identifier BLUE = Identifier.of(RPGMinibosses.MOD_ID,"textures/mob/gemini_blue.png");

    @Override
    public Identifier getModelResource(T reaver) {

        return Identifier.of(RPGMinibosses.MOD_ID,"geo/gemini.geo.json");
    }
    @Override
    public Identifier getTextureResource(T reaver) {



        return reaver.school == SpellSchools.FIRE ? DEFAULT_LOCATION : BLUE;
    }

    @Override
    public Identifier getAnimationResource(T reaver) {
        return Identifier.of(RPGMinibosses.MOD_ID,"animations/gemini.animations.json");
    }

    public void setArmAngle(Arm humanoidArm, MatrixStack poseStack) {
        this.translateAndRotate(poseStack);
    }
    public void translateAndRotate(MatrixStack arg) {
        arg.translate((double)(1), (double)(0 / 16.0F), (double)(0 / 16.0F));
        arg.scale(2, 2, 2);



    }
}

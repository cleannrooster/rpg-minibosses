package com.cleannrooster.rpg_minibosses.client.entity.model;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.TricksterEntity;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellSchools;

public class TricksterModel<T extends MinibossEntity> extends GeoModel<T> implements ModelWithArms {
    private static final Identifier DEFAULT_LOCATION = Identifier.of(RPGMinibosses.MOD_ID,"textures/mob/thieftexture.png");

    @Override
    public Identifier getModelResource(T reaver) {

        return Identifier.of(RPGMinibosses.MOD_ID,"geo/thiefmob.json");
    }
    @Override
    public Identifier getTextureResource(T reaver) {

        Identifier identifier = DEFAULT_LOCATION;
        if(!reaver.notPetrified()) {
            identifier = Identifier.of(RPGMinibosses.MOD_ID, "textures/mob/petrified.png");
        }

        return identifier;    }

    @Override
    public Identifier getAnimationResource(T reaver) {
        return Identifier.of(RPGMinibosses.MOD_ID,"animations/mobs.animations.json");
    }

    public void setArmAngle(Arm humanoidArm, MatrixStack poseStack) {
        this.translateAndRotate(poseStack);
    }
    public void translateAndRotate(MatrixStack arg) {
        arg.translate((double)(1), (double)(0 / 16.0F), (double)(0 / 16.0F));
        arg.scale(2, 2, 2);



    }
}

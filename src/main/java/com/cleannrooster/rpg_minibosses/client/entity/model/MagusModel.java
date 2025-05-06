package com.cleannrooster.rpg_minibosses.client.entity.model;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.internal.client.util.RenderUtils;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Identifier;
import net.spell_engine.entity.SpellProjectile;

public class MagusModel<T extends MagusPrimeEntity> extends GeoModel<T> implements ModelWithArms {
    private static final Identifier DEFAULT_LOCATION = Identifier.of(RPGMinibosses.MOD_ID,"textures/mob/magus_prime_texture.png");
    private static final Identifier CASTING_1 = Identifier.of(RPGMinibosses.MOD_ID,"textures/mob/magus_prime_texture_casting.png");

    private static final Identifier CASTING_2 = Identifier.of(RPGMinibosses.MOD_ID,"textures/mob/magus_prime_texture_casting_2.png");

    @Override
    public Identifier getModelResource(T reaver) {

        return Identifier.of(RPGMinibosses.MOD_ID,"geo/magus_prime.geo.json");
    }



    @Override
    public Identifier getTextureResource(T reaver) {

        Identifier identifier = DEFAULT_LOCATION;
        if(reaver.getDataTracker().get(MagusPrimeEntity.CASTINGBOOL)){
            if(RenderUtils.getCurrentTick() % 10 <= 5){
                return CASTING_1;
            }
            else{
                return CASTING_2;
            }
        }
        if(!reaver.notPetrified()) {
            identifier = Identifier.of(RPGMinibosses.MOD_ID, "textures/mob/petrified.png");
        }

        return identifier;    }

    @Override
    public Identifier getAnimationResource(T reaver) {
        return Identifier.of(RPGMinibosses.MOD_ID,"animations/magus.animations.json");
    }

    public void setArmAngle(Arm humanoidArm, MatrixStack poseStack) {
        this.translateAndRotate(poseStack);
    }
    public void translateAndRotate(MatrixStack arg) {
        arg.translate((double)(1), (double)(0 / 16.0F), (double)(0 / 16.0F));
        arg.scale(2, 2, 2);



    }
}

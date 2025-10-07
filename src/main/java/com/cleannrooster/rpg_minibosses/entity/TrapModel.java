package com.cleannrooster.rpg_minibosses.entity;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import mod.azure.azurelib.model.GeoModel;
import net.minecraft.util.Identifier;


public class TrapModel<T extends TrapCleann> extends mod.azure.azurelib.model.GeoModel<TrapCleann> {

    @Override
    public Identifier getModelResource(TrapCleann reaver) {
        return Identifier.of(RPGMinibosses.MOD_ID,"geo/trapmodel.geo.json");
    }
    @Override
    public Identifier getTextureResource(TrapCleann reaver) {
        return Identifier.of(RPGMinibosses.MOD_ID,"textures/mob/traptexture.png");
    }

    @Override
    public Identifier getAnimationResource(TrapCleann reaver) {
        return Identifier.of(RPGMinibosses.MOD_ID,"animations/trapmodel.animation.json") ;
    }


}

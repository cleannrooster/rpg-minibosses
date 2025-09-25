package com.cleannrooster.rpg_minibosses.client.armor.model;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.item.ThiefArmor;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import net.minecraft.util.Identifier;

public class ThiefModel extends GeoModel<ThiefArmor> {

    public Identifier getModelResource(ThiefArmor animatable) {

        return Identifier.of(RPGMinibosses.MOD_ID,"geo/thiefmodel.geo.json");
    }

    @Override
    public Identifier getTextureResource(ThiefArmor animatable) {

        return Identifier.of(RPGMinibosses.MOD_ID,"textures/armor/thieftexture.png");
    }

    @Override
    public Identifier getAnimationResource(ThiefArmor animatable) {
        return null;
    }
}

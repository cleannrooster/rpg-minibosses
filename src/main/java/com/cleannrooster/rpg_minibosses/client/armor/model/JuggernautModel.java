package com.cleannrooster.rpg_minibosses.client.armor.model;


import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.item.JuggernautArmor;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import net.minecraft.util.Identifier;

public class JuggernautModel extends GeoModel<JuggernautArmor> {

    public Identifier getModelResource(JuggernautArmor animatable) {

        return Identifier.of(RPGMinibosses.MOD_ID,"geo/juggmodel.geo.json");
    }

    @Override
    public Identifier getTextureResource(JuggernautArmor animatable) {

        return Identifier.of(RPGMinibosses.MOD_ID,"textures/armor/juggtexture.png");
    }

    @Override
    public Identifier getAnimationResource(JuggernautArmor animatable) {
        return null;
    }
}

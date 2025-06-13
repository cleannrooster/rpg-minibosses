package com.cleannrooster.rpg_minibosses.client.armor.model;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.item.AbberrathArmor;
import com.cleannrooster.rpg_minibosses.item.WhisperingIceStaff;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import net.minecraft.util.Identifier;

public class WhisperingIceModel extends GeoModel<WhisperingIceStaff> {


    @Override
    public Identifier getModelResource(WhisperingIceStaff animatable) {

        return Identifier.of(RPGMinibosses.MOD_ID,"geo/whispering_ice.geo.json");
    }

    @Override
    public Identifier getTextureResource(WhisperingIceStaff animatable) {

        return Identifier.of(RPGMinibosses.MOD_ID,"textures/item/whispering.png");
    }

    @Override
    public Identifier getAnimationResource(WhisperingIceStaff animatable) {
        return null;
    }
}

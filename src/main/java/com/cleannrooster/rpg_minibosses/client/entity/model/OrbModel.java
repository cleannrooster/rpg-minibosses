package com.cleannrooster.rpg_minibosses.client.entity.model;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.OrbEntity;
import com.cleannrooster.spellblades.entity.CycloneEntity;
import com.cleannrooster.spellblades.items.Orb;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import net.minecraft.util.Identifier;

public class OrbModel<T extends OrbEntity> extends GeoModel<OrbEntity> {

    @Override
    public Identifier getModelResource(OrbEntity reaver) {

        return Identifier.of(RPGMinibosses.MOD_ID,"geo/orb.geo.json");
    }
    @Override
    public Identifier getTextureResource(OrbEntity reaver) {

        return Identifier.of(RPGMinibosses.MOD_ID, "textures/item/orb_black.png");

    }

    @Override
    public Identifier getAnimationResource(OrbEntity reaver) {
        return Identifier.of(RPGMinibosses.MOD_ID,"animations/orb.animation.json");
    }

}

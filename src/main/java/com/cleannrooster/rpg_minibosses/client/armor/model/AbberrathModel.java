package com.cleannrooster.rpg_minibosses.client.armor.model;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.item.AbberrathArmor;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.util.Identifier;
import net.spell_power.api.SpellSchools;

public class AbberrathModel extends GeoModel<AbberrathArmor> {


    @Override
    public Identifier getModelResource(AbberrathArmor animatable) {

        return Identifier.of(RPGMinibosses.MOD_ID,"geo/abberraths_hooves.json");
    }

    @Override
    public Identifier getTextureResource(AbberrathArmor animatable) {

        return Identifier.of(RPGMinibosses.MOD_ID,"textures/armor/abberrath.png");
    }

    @Override
    public Identifier getAnimationResource(AbberrathArmor animatable) {
        return null;
    }
}

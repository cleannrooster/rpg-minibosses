package com.cleannrooster.rpg_minibosses.client.armor.renderer;


import com.cleannrooster.rpg_minibosses.client.armor.model.ThiefModel;
import com.cleannrooster.rpg_minibosses.item.ThiefArmor;
import mod.azure.azurelib.common.api.client.renderer.GeoArmorRenderer;

public class ThiefArmorRenderer extends GeoArmorRenderer<ThiefArmor> {

    public ThiefArmorRenderer() {
        super(new ThiefModel());

    }
}

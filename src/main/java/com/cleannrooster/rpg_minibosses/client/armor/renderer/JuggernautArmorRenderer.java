package com.cleannrooster.rpg_minibosses.client.armor.renderer;


import com.cleannrooster.rpg_minibosses.client.armor.model.JuggernautModel;
import com.cleannrooster.rpg_minibosses.item.JuggernautArmor;
import mod.azure.azurelib.common.api.client.renderer.GeoArmorRenderer;

public class JuggernautArmorRenderer extends GeoArmorRenderer<JuggernautArmor> {

    public JuggernautArmorRenderer() {
        super(new JuggernautModel());

    }
}

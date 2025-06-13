package com.cleannrooster.rpg_minibosses.client.armor.renderer;

import com.cleannrooster.rpg_minibosses.client.armor.model.AbberrathModel;
import com.cleannrooster.rpg_minibosses.client.armor.model.WhisperingIceModel;
import com.cleannrooster.rpg_minibosses.item.AbberrathArmor;
import com.cleannrooster.rpg_minibosses.item.WhisperingIceStaff;
import mod.azure.azurelib.common.api.client.renderer.GeoArmorRenderer;
import mod.azure.azurelib.common.api.client.renderer.GeoItemRenderer;

public class WhisperingIceRenderer extends GeoItemRenderer<WhisperingIceStaff> {

    public WhisperingIceRenderer() {
        super(new WhisperingIceModel());

    }

}

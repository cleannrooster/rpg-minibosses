package com.cleannrooster.rpg_minibosses.client.armor.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import net.minecraft.util.Identifier;

public class WhisperingIceRenderer extends AzItemRenderer {

    private static final Identifier GEO = Identifier.of(RPGMinibosses.MOD_ID, "geo/whispering_ice.geo.json");
    private static final Identifier TEXTURE = Identifier.of(RPGMinibosses.MOD_ID, "textures/item/whispering.png");

    public WhisperingIceRenderer() {
        super(AzItemRendererConfig.builder(GEO, TEXTURE).build());
    }
}

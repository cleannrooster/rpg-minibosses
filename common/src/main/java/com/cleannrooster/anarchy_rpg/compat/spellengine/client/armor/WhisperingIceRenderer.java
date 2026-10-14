package com.cleannrooster.anarchy_rpg.compat.spellengine.client.armor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import mod.azure.azurelib.common.render.item.AzItemRenderer;
import mod.azure.azurelib.common.render.item.AzItemRendererConfig;
import net.minecraft.util.Identifier;

public class WhisperingIceRenderer extends AzItemRenderer {

    private static final Identifier GEO = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/whispering_ice.geo.json");
    private static final Identifier TEXTURE = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/item/whispering.png");

    public WhisperingIceRenderer() {
        super(AzItemRendererConfig.builder(GEO, TEXTURE).build());
    }
}

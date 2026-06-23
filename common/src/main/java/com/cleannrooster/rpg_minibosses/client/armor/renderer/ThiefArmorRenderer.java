package com.cleannrooster.rpg_minibosses.client.armor.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererConfig;
import net.minecraft.util.Identifier;

public class ThiefArmorRenderer extends AzArmorRenderer {

    private static final Identifier GEO = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/thiefmodel.geo.json");
    private static final Identifier TEXTURE = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/armor/thieftexture.png");

    public ThiefArmorRenderer() {
        super(AzArmorRendererConfig.builder(GEO, TEXTURE).build());
    }
}

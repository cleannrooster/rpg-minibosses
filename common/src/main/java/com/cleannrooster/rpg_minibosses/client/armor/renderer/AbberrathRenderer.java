package com.cleannrooster.rpg_minibosses.client.armor.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererConfig;
import net.minecraft.util.Identifier;

public class AbberrathRenderer extends AzArmorRenderer {

    private static final Identifier GEO = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/abberraths_hooves.json");
    private static final Identifier TEXTURE = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/armor/abberrath.png");

    public AbberrathRenderer() {
        super(AzArmorRendererConfig.builder(GEO, TEXTURE).build());
    }
}

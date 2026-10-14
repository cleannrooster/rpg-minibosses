package com.cleannrooster.anarchy_rpg.compat.spellengine.client.armor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
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

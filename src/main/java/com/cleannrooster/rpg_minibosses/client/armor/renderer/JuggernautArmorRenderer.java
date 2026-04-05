package com.cleannrooster.rpg_minibosses.client.armor.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.item.JuggernautArmor;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererConfig;
import net.minecraft.util.Identifier;

public class JuggernautArmorRenderer extends AzArmorRenderer{

    private static final Identifier GEO = Identifier.of(RPGMinibosses.MOD_ID, "geo/juggmodel.geo.json");
    private static final Identifier TEXTURE = Identifier.of(RPGMinibosses.MOD_ID, "textures/armor/juggtexture.png");

    public JuggernautArmorRenderer() {
        super(AzArmorRendererConfig.<JuggernautArmor>builder(GEO, TEXTURE).build());
    }

}

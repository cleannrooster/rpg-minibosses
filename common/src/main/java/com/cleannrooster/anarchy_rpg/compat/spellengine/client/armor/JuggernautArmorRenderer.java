package com.cleannrooster.anarchy_rpg.compat.spellengine.client.armor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.compat.spellengine.item.JuggernautArmor;
import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererConfig;
import net.minecraft.util.Identifier;

public class JuggernautArmorRenderer extends AzArmorRenderer{

    private static final Identifier GEO = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/juggmodel.geo.json");
    private static final Identifier TEXTURE = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/armor/juggtexture.png");

    public JuggernautArmorRenderer() {
        super(AzArmorRendererConfig.<JuggernautArmor>builder(GEO, TEXTURE).build());
    }

}

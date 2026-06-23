package com.cleannrooster.rpg_minibosses.client.armor.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;

import mod.azure.azurelib.common.render.armor.AzArmorRenderer;
import mod.azure.azurelib.common.render.armor.AzArmorRendererConfig;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;

public class UniqueArmorRenderer extends AzArmorRenderer {

    public UniqueArmorRenderer(String modelName, String textureName) {
        super(AzArmorRendererConfig.builder(
                        Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "geo/" + modelName + ".geo.json"),
                        Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "textures/armor/" + textureName + ".png"))
                .build()
        );
    }
    public static UniqueArmorRenderer despot() {
        return new UniqueArmorRenderer("kaomsheart", "kaomsheart");
    }
    public static UniqueArmorRenderer foxshade() {
        return new UniqueArmorRenderer("foxshade", "foxshade");
    }
    public static UniqueArmorRenderer kintsugi() {
        return new UniqueArmorRenderer("kintsugi", "kintsugi");
    }
    public static UniqueArmorRenderer sanguine_fire() {
        return new UniqueArmorRenderer("sanguine", "sanguine_red");
    }
    public static UniqueArmorRenderer sanguine_frost() {
        return new UniqueArmorRenderer("sanguine", "sanguine_blue");
    }
    public static UniqueArmorRenderer sanguine_arcane() {
        return new UniqueArmorRenderer("sanguine", "sanguine_purple");
    }

}

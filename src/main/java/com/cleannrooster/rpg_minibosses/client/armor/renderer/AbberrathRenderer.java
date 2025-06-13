package com.cleannrooster.rpg_minibosses.client.armor.renderer;

import com.cleannrooster.rpg_minibosses.client.armor.model.AbberrathModel;
import com.cleannrooster.rpg_minibosses.item.AbberrathArmor;
import mod.azure.azurelib.common.api.client.renderer.GeoArmorRenderer;
import mod.azure.azurelib.common.internal.client.util.RenderUtils;
import mod.azure.azurelib.common.internal.common.cache.object.GeoBone;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.EquipmentSlot;
import org.jetbrains.annotations.Nullable;

public class AbberrathRenderer extends GeoArmorRenderer<AbberrathArmor> {

    public AbberrathRenderer() {
        super(new AbberrathModel());

    }

}

package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.OrbEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class OrbAnimationProvider extends AzEntityAnimator<OrbEntity> {

    private static final Identifier ANIMATIONS = Identifier.of(RPGMinibosses.MOD_ID, "animations/orb.animation.json");

    public static final AzCommand SPIN_COMMAND = AzCommand.create("fly", "idle", AzPlayBehaviors.LOOP);

    @Override
    public void registerControllers(AzAnimationControllerContainer<OrbEntity> container) {
        container.add(
            AzAnimationController.<OrbEntity>builder(this, "fly").setTransitionLength(0).build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(OrbEntity animatable) {
        return ANIMATIONS;
    }
}

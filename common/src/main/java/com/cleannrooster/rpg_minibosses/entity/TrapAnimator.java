package com.cleannrooster.rpg_minibosses.entity;

import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import static com.cleannrooster.rpg_minibosses.RPGMinibosses.CONTENT_NAMESPACE;

public class TrapAnimator extends AzEntityAnimator<TrapCleann> {

    private static final Identifier ANIMATIONS = Identifier.of(CONTENT_NAMESPACE, "animations/trapmodel.animation.json");

    @Override
    public void registerControllers(AzAnimationControllerContainer<TrapCleann> animationControllerContainer) {
        animationControllerContainer.add(
            AzAnimationController.<TrapCleann>builder(this, "drop")
                .setTransitionLength(3)
                .build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(TrapCleann animatable) {
        return ANIMATIONS;
    }
}

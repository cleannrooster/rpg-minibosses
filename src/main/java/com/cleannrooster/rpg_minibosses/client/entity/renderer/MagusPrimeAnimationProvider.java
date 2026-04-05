package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.MagusPrimeEntity;

import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class MagusPrimeAnimationProvider extends AzEntityAnimator<MagusPrimeEntity> {

    private static final Identifier ANIMATIONS = Identifier.of(RPGMinibosses.MOD_ID, "animations/magus.animations.json");

    public static final AzCommand IDLE_COMMAND = AzCommand.create("walk", "animation.magus.idle", AzPlayBehaviors.LOOP);
    public static final AzCommand WALK_COMMAND = AzCommand.create("walk", "animation.magus.walk_1", AzPlayBehaviors.LOOP);
    public static final AzCommand INTRO_COMMAND = AzCommand.create("dash", "animation.magus.intro", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand CASTING = AzCommand.create("attacks", "animation.magus.casting", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand CASTINGM = AzCommand.create("attacks", "animation.magus.casting2", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand DASH = AzCommand.create("dash", "animation.magus.dashforward", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand CASTQUICKM = AzCommand.create("attacks", "animation.magus.cast.quick", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand CASTQUICK = AzCommand.create("attacks", "animation.magus.cast.quick2", AzPlayBehaviors.PLAY_ONCE);

    @Override
    public void registerControllers(AzAnimationControllerContainer<MagusPrimeEntity> container) {
        container.add(
            AzAnimationController.<MagusPrimeEntity>builder(this, "walk").setTransitionLength(0).build(),
                AzAnimationController.<MagusPrimeEntity>builder(this, "attacks").setTransitionLength(0).build(),
                AzAnimationController.<MagusPrimeEntity>builder(this, "dash").setTransitionLength(0).build()


        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(MagusPrimeEntity animatable) {
        return ANIMATIONS;
    }
}

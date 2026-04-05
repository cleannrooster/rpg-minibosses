package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.GeminiEntity;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

public class GeminiAnimationProvider extends AzEntityAnimator<GeminiEntity> {

    private static final Identifier ANIMATIONS = Identifier.of(RPGMinibosses.MOD_ID, "animations/gemini.animations.json");

    public static final AzCommand IDLE_COMMAND = AzCommand.create("idle", "animation.awakener.idle", AzPlayBehaviors.LOOP);
    public static final AzCommand BEAM_COMMAND = AzCommand.create("beam", "animation.awakener.beam_1", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand BEAM_LARGE_COMMAND = AzCommand.create("beam_large", "animation.awakener.beam_large", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand METEOR_CHANNEL_COMMAND = AzCommand.create("meteor_channel", "animation.awakener.meteor_channel", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand METEOR_CHARGE_COMMAND = AzCommand.create("meteor_charge", "animation.awakener.meteor_charge", AzPlayBehaviors.PLAY_ONCE);

    @Override
    public void registerControllers(AzAnimationControllerContainer<GeminiEntity> container) {
        container.add(
            AzAnimationController.<GeminiEntity>builder(this, "idle").setTransitionLength(0).build(),
            AzAnimationController.<GeminiEntity>builder(this, "meteor_channel").setTransitionLength(0).build(),
            AzAnimationController.<GeminiEntity>builder(this, "meteor_charge").setTransitionLength(0).build(),
            AzAnimationController.<GeminiEntity>builder(this, "beam").setTransitionLength(0).build(),
            AzAnimationController.<GeminiEntity>builder(this, "beam_large").setTransitionLength(0).build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(GeminiEntity animatable) {
        return ANIMATIONS;
    }
}

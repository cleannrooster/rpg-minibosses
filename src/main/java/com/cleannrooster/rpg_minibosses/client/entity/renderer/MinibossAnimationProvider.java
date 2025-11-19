package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.TemplarEntity;
import mod.azure.azurelib.rewrite.animation.AzAnimatorConfig;
import mod.azure.azurelib.rewrite.animation.controller.AzAnimationController;
import mod.azure.azurelib.rewrite.animation.controller.AzAnimationControllerBuilder;
import mod.azure.azurelib.rewrite.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.rewrite.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.rewrite.animation.easing.AzEasingTypes;
import mod.azure.azurelib.rewrite.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.rewrite.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class MinibossAnimationProvider  extends AzEntityAnimator<MinibossEntity> {
    private static final Identifier ANIMATIONS = Identifier.of(RPGMinibosses.MOD_ID,
            "animations/mobs.animations.json"
    );

    public MinibossAnimationProvider() {
        super(AzAnimatorConfig.defaultConfig());
        builders = List.of(
                AzAnimationController.builder(this, "base_controller").setTransitionLength(2).setEasingType(AzEasingTypes.EASE_IN_OUT_QUAD),
                AzAnimationController.builder(this, "dash").setTransitionLength(2).setEasingType(AzEasingTypes.EASE_IN_OUT_QUAD),

                AzAnimationController.builder(this, "attacks").setTransitionLength(2).setEasingType(AzEasingTypes.EASE_IN_OUT_QUAD));

    }
    public List<AzAnimationControllerBuilder> builders;

    @Override
    public void registerControllers(AzAnimationControllerContainer<MinibossEntity> animationControllerContainer) {
        for(AzAnimationControllerBuilder controller : builders) {
            animationControllerContainer.add(
                controller.build()
            );
        }
    }

    @Override
    public @NotNull Identifier getAnimationLocation(MinibossEntity animatable) {
        return ANIMATIONS;
    }


    public record MinibossAnimationDispatcher(MinibossEntity entity) {


        private static final AzCommand IDLE_COMMAND = AzCommand.create(
                    "base_controller",
                    "idle",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand MERC_IDLE_AGGRO = AzCommand.create(
                    "base_controller",
                    "animation.merc.idle",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand IDLE_AGGRO = AzCommand.create(
                    "base_controller",
                    "animation.unknown.idle",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand IDLE_2h_COMMAND = AzCommand.create(
                    "base_controller",
                    "animation.unknown.idle_2h",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand WALK_COMMAND = AzCommand.create(
                    "base_controller",
                    "walking",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand WALK_AGGRO = AzCommand.create(
                    "base_controller",
                    "animation.unknown.walk",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand WALK_AGGRO_TEMPLAR = AzCommand.create(
                    "base_controller",
                    "animation.mob.walk_templar",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand WALK_2h_COMMAND = AzCommand.create(
                    "base_controller",
                    "animation.unknown.walk_2h",
                    AzPlayBehaviors.LOOP
            );

            private static final AzCommand WALK_BACKWARDS_COMMAND = AzCommand.create(
                    "base_controller",
                    "walking_backwards",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand RUN_COMMAND = AzCommand.create(
                    "base_controller",
                    "running",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand HEAVY_RUN = AzCommand.create(
                    "base_controller",
                    "animation.merc.heavy_run",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand RUN_2H = AzCommand.create(
                    "base_controller",
                    "animation.templar.heavy_run",
                    AzPlayBehaviors.LOOP
            );
            private static final AzCommand DOWN = AzCommand.create(
                    "base_controller",
                    "animation.generic.down",
                    AzPlayBehaviors.HOLD_ON_LAST_FRAME
            );
            private static final AzCommand SWING = AzCommand.create(
                    "attacks",
                    "animation.mob.swing1",
                    AzPlayBehaviors.PLAY_ONCE
            );
            private static final AzCommand SWING2 = AzCommand.create(
                    "attacks",
                    "animation.mob.swing2",
                    AzPlayBehaviors.PLAY_ONCE
            );
            private static final AzCommand PREPARE = AzCommand.create(
                    "base_controller",
                    "animation.mob.prepare",
                    AzPlayBehaviors.PLAY_ONCE
            );
        private static final AzCommand dashRight = AzCommand.create(
                "dash",
                "animation.valkyrie.dashright",
                AzPlayBehaviors.PLAY_ONCE
        );
        private static final AzCommand dashLeft = AzCommand.create(
                "dash",
                "animation.valkyrie.dashLeft",
                AzPlayBehaviors.PLAY_ONCE
        );
        private static final AzCommand throw1 = AzCommand.create(
                "attacks",
                "animation.mob.throw1",
                AzPlayBehaviors.PLAY_ONCE
        );
        private static final AzCommand throw2 = AzCommand.create(
                "attacks",
                "animation.mob.throw2",
                AzPlayBehaviors.PLAY_ONCE
        );
        private static final AzCommand pommelstrike = AzCommand.create(
                "attacks",
                "animation.mob.trickster.pommelstrike",
                AzPlayBehaviors.PLAY_ONCE
        );
        private static final AzCommand roll = AzCommand.create(
                "dash",
                "animation.mob.trickster.roll",
                AzPlayBehaviors.PLAY_ONCE
        );

            public void idle() {
                IDLE_COMMAND.sendForEntity(entity);
            }

        public void idleAggro() {
                if (entity instanceof ArtilleristEntity) {
                    MERC_IDLE_AGGRO.sendForEntity(entity);
                } else {
                    IDLE_AGGRO.sendForEntity(entity);
                }
            }

            public void walk() {
                WALK_COMMAND.sendForEntity(entity);
            }

        public void walkAggro() {
                if (entity instanceof TemplarEntity) {
                    WALK_AGGRO_TEMPLAR.sendForEntity(entity);
                } else {
                    WALK_AGGRO.sendForEntity(entity);
                }
            }

        public void run() {
                if (entity instanceof ArtilleristEntity) {
                    HEAVY_RUN.sendForEntity(entity);
                } else if (entity.isTwoHand()) {
                    RUN_2H.sendForEntity(entity);
                } else {
                    RUN_COMMAND.sendForEntity(entity);
                }
            }

        public void throw1() {
           throw1.sendForEntity(entity);
        }
        public void throw2() {
            throw2.sendForEntity(entity);

        }
        public void dashleft() {
            dashLeft.sendForEntity(entity);

        }
        public void dashright() {
            dashRight.sendForEntity(entity);

        }
        public void roll() {
            roll.sendForEntity(entity);

        }
        public void setPommelstrike() {
            pommelstrike.sendForEntity(entity);

        }
        public void setSwing() {
            SWING.sendForEntity(entity);

        }
        public void setSwing2() {
            SWING2.sendForEntity(entity);

        }
        public void setPrepare() {
            PREPARE.sendForEntity(entity);

        }
}

}

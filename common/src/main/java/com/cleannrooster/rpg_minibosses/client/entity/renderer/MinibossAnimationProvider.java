package com.cleannrooster.rpg_minibosses.client.entity.renderer;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.entity.ArchmageFireEntity;
import com.cleannrooster.rpg_minibosses.entity.ArtilleristEntity;
import com.cleannrooster.rpg_minibosses.entity.JuggernautEntity;
import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import com.cleannrooster.rpg_minibosses.entity.TemplarEntity;
import com.cleannrooster.rpg_minibosses.entity.TricksterEntity;

import mod.azure.azurelib.common.animation.AzAnimatorConfig;
import mod.azure.azurelib.common.animation.controller.AzAnimationController;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerBuilder;
import mod.azure.azurelib.common.animation.controller.AzAnimationControllerContainer;
import mod.azure.azurelib.common.animation.dispatch.command.AzCommand;
import mod.azure.azurelib.common.animation.impl.AzEntityAnimator;
import mod.azure.azurelib.common.animation.play_behavior.AzPlayBehaviors;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MinibossAnimationProvider  extends AzEntityAnimator<MinibossEntity> {

    /**
     * The v2 combat animation resource.
     *
     * <p>The legacy {@code animations/mobs.animations.json} is intentionally retained in the repository,
     * untouched, for reference and posterity — nothing points at it any more. Every clip it contained was
     * copied forward into this file by {@code tools/gen_v2_animations.py}, so old animation names still
     * resolve, and the authored v2 combat clips were added alongside them. Do not delete the legacy file.
     */
    private static final Identifier ANIMATIONS = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE,
            "animations/mobs_v2.animations.json"
    );

    public MinibossAnimationProvider() {
        super(AzAnimatorConfig.defaultConfig());
        builders = List.of(
                // Controller order is layering order: later controllers win the bones they animate, so an
                // attack always reads over the locomotion cycle underneath it.
                AzAnimationController.builder(this, "base_controller").setTransitionLength(5),
                AzAnimationController.builder(this, "dash").setTransitionLength(2),

                AzAnimationController.builder(this, "attacks").setTransitionLength(2));

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

        /** Built commands are immutable and reused; building one per tick would allocate for nothing. */
        private static final Map<String, AzCommand> CACHE = new ConcurrentHashMap<>();

        // ── v2 clip families ──────────────────────────────────────────────────
        //
        // Each mob has its own set on the shared biped rig. The keys are the per-mob prefixes; the
        // locomotion suffixes below are the directional set the client selects between.

        public static final String JUGG = "animation.v2.jugg.";
        public static final String TEMPLAR = "animation.v2.templar.";
        public static final String TRICKSTER = "animation.v2.trickster.";
        public static final String MERC = "animation.v2.merc.";
        public static final String MAGE = "animation.v2.mage.";

        public static final String STANCE = "stance";
        public static final String ADVANCE = "advance";
        public static final String LATERAL_LEFT = "lateral_left";
        public static final String LATERAL_RIGHT = "lateral_right";
        public static final String BACKSTEP = "backstep";

        /** Prefix for whichever of the five this entity is; falls back to the Trickster's one-handed set. */
        public static String family(MinibossEntity entity) {
            if (entity instanceof JuggernautEntity) return JUGG;
            if (entity instanceof TemplarEntity) return TEMPLAR;
            if (entity instanceof TricksterEntity) return TRICKSTER;
            if (entity instanceof ArtilleristEntity) return MERC;
            if (entity instanceof ArchmageFireEntity) return MAGE;
            return TRICKSTER;
        }

        // ── Generic dispatch ──────────────────────────────────────────────────

        /**
         * Play a clip on a controller. This is the entry point the combat action runner uses, so an
         * attack definition can name its clip as data rather than needing a bespoke method here.
         */
        public void play(String controller, String animation, boolean loop, float speed) {
            command(controller, animation, loop, speed).sendForEntity(entity);
        }

        /** Play a v2 clip belonging to this entity's own family. */
        public void playOwn(String controller, String suffix, boolean loop, float speed) {
            play(controller, family(entity) + suffix, loop, speed);
        }

        private static AzCommand command(String controller, String animation, boolean loop, float speed) {
            var key = controller + '|' + animation + '|' + loop + '|' + speed;
            return CACHE.computeIfAbsent(key, ignored -> AzCommand.create(
                    controller,
                    animation,
                    loop ? AzPlayBehaviors.LOOP : AzPlayBehaviors.PLAY_ONCE,
                    0, speed, 0, 0, 0, false));
        }

        // ── Locomotion (client-driven, selected from real displacement) ────────

        /** Combat idle: the mob is holding a stance, not standing around. */
        public void stance() {
            playOwn("base_controller", STANCE, true, 1.0f);
        }

        public void advance(float speed) {
            playOwn("base_controller", ADVANCE, true, speed);
        }

        public void lateral(int side, float speed) {
            playOwn("base_controller", side >= 0 ? LATERAL_LEFT : LATERAL_RIGHT, true, speed);
        }

        public void backstep(float speed) {
            playOwn("base_controller", BACKSTEP, true, speed);
        }

        // ── Legacy out-of-combat locomotion ────────────────────────────────────
        //
        // Retained verbatim: these are the pre-overhaul clips, still used when a miniboss is wandering,
        // following an owner, or downed. The overhaul only replaces what happens once it has a target.

        private static final AzCommand IDLE_COMMAND = AzCommand.create(
                "base_controller", "idle", AzPlayBehaviors.LOOP);
        private static final AzCommand MERC_IDLE_AGGRO = AzCommand.create(
                "base_controller", "animation.merc.idle", AzPlayBehaviors.LOOP);
        private static final AzCommand IDLE_AGGRO = AzCommand.create(
                "base_controller", "animation.unknown.idle", AzPlayBehaviors.LOOP);
        private static final AzCommand IDLE_2h_COMMAND = AzCommand.create(
                "base_controller", "animation.unknown.idle_2h", AzPlayBehaviors.LOOP);
        private static final AzCommand DOWN = AzCommand.create(
                "base_controller", "animation.generic.down", AzPlayBehaviors.HOLD_ON_LAST_FRAME);

        private static AzCommand WALK_COMMAND(float speed) {
            return AzCommand.create("base_controller", "walking",
                    AzPlayBehaviors.LOOP, 0, speed, 0, 0, 0, false);
        }

        private static AzCommand WALK_2h_COMMAND(float speed) {
            return AzCommand.create("base_controller", "animation.unknown.walk_2h",
                    AzPlayBehaviors.LOOP, 0, speed, 0, 0, 0, false);
        }

        private static AzCommand RUN_COMMAND(float speed) {
            return AzCommand.create("base_controller", "running",
                    AzPlayBehaviors.LOOP, 0, speed, 0, 0, 0, false);
        }

        public void idle() {
            if (entity instanceof ArtilleristEntity) {
                MERC_IDLE_AGGRO.sendForEntity(entity);
            } else if (entity.isTwoHand()) {
                IDLE_2h_COMMAND.sendForEntity(entity);
            } else {
                IDLE_COMMAND.sendForEntity(entity);
            }
        }

        public void idleAggro() {
            stance();
        }

        public void walk(float speed) {
            if (entity.isTwoHand()) {
                WALK_2h_COMMAND(speed).sendForEntity(entity);
            } else {
                WALK_COMMAND(speed).sendForEntity(entity);
            }
        }

        public void walkAggro(float speed) {
            advance(speed);
        }

        public void run(float speed) {
            RUN_COMMAND(speed).sendForEntity(entity);
        }

        public void setIdleAggro() {
            IDLE_AGGRO.sendForEntity(entity);
        }

        public void setIdle2h() {
            IDLE_2h_COMMAND.sendForEntity(entity);
        }

        public void setDown() {
            DOWN.sendForEntity(entity);
        }

        // ── Legacy attack dispatch ────────────────────────────────────────────
        //
        // Still referenced by entity code outside the five overhauled brains (Gemini's allies, the downed
        // state, the older goals kept for compatibility). The clips they name live on in the v2 resource.

        private static final AzCommand SWING = AzCommand.create(
                "attacks", "animation.mob.swing1", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand SWING2 = AzCommand.create(
                "attacks", "animation.mob.swing2", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand PREPARE = AzCommand.create(
                "attacks", "animation.mob.prepare", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand WAVE_1h = AzCommand.create(
                "attacks", "animation.mob.wave", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand walkwave = AzCommand.create(
                "attacks", "animation.mob.walkwave", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand WAVE = AzCommand.create(
                "attacks", "animation.mob.wizard.staffwave", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand SLAM = AzCommand.create(
                "attacks", "animation.mob.heavy.slam", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand LEAP = AzCommand.create(
                "attacks", "animation.mob.jugg.leapslam", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand SPIN = AzCommand.create(
                "attacks", "animation.mob.spin_2h", AzPlayBehaviors.PLAY_ONCE, 0, 1.05F, 0, 0, 0, false);
        private static final AzCommand dashRight = AzCommand.create(
                "dash", "animation.valkyrie.dashright", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand dashLeft = AzCommand.create(
                "dash", "animation.valkyrie.dashleft", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand throw1 = AzCommand.create(
                "attacks", "animation.mob.throw1", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand staff = AzCommand.create(
                "attacks", "animation.valkyrie.staff", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand throw2 = AzCommand.create(
                "attacks", "animation.mob.throw2", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand pommelstrike = AzCommand.create(
                "attacks", "animation.mob.trickster.pommelstrike", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand roll = AzCommand.create(
                "dash", "animation.mob.trickster.roll", AzPlayBehaviors.PLAY_ONCE);
        private static final AzCommand SHOOT_HEAVY = AzCommand.create(
                "attacks", "animation.merc.shoot_heavy", AzPlayBehaviors.PLAY_ONCE, 2, 1.05F, 0, 0, 0, false);
        private static final AzCommand SHOOT_HEAVY_MANY = AzCommand.create(
                "attacks", "animation.merc.shoot_heavy_many", AzPlayBehaviors.PLAY_ONCE, 0, 1, 0, 0, 0, false);

        public void throw1() { throw1.sendForEntity(entity); }

        public void throw2() { throw2.sendForEntity(entity); }

        public void dashleft() { dashLeft.sendForEntity(entity); }

        public void dashright() { dashRight.sendForEntity(entity); }

        public void roll() { roll.sendForEntity(entity); }

        public void setPommelstrike() { pommelstrike.sendForEntity(entity); }

        public void setSwing() { SWING.sendForEntity(entity); }

        public void setSwing2() { SWING2.sendForEntity(entity); }

        public void setPrepare() { PREPARE.sendForEntity(entity); }

        public void setMercIdleAggro() { MERC_IDLE_AGGRO.sendForEntity(entity); }

        public void setSpin() { SPIN.sendForEntity(entity); }

        public void setWave() { WAVE.sendForEntity(entity); }

        public void setLeap() { LEAP.sendForEntity(entity); }

        public void setSlam() { SLAM.sendForEntity(entity); }

        public void setWAVE_1h() { WAVE_1h.sendForEntity(entity); }

        public void setWalkwave() { walkwave.sendForEntity(entity); }

        public void setShootHeavy() { SHOOT_HEAVY.sendForEntity(entity); }

        public void setShootHeavyMany() { SHOOT_HEAVY_MANY.sendForEntity(entity); }

        public void setStaff() { staff.sendForEntity(entity); }
    }

}

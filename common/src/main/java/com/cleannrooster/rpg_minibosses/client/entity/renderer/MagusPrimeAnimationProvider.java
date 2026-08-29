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

    /**
     * The v2 clip resource for Magus.
     *
     * <p>{@code animations/magus.animations.json} is intentionally left on disk, untouched, as
     * reference material — every clip it held was copied forward into this file by
     * {@code tools/gen_gemini_magus_v2_animations.py}, so the legacy names still resolve, and the
     * authored v2 clips sit alongside them. Do not delete the old file.
     */
    private static final Identifier ANIMATIONS = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "animations/magus_v2.animations.json");

    // ── Locomotion and legacy clips ──────────────────────────────────────────
    public static final AzCommand IDLE_COMMAND = AzCommand.create("walk", "animation.magus.idle", AzPlayBehaviors.LOOP);
    public static final AzCommand WALK_COMMAND = AzCommand.create("walk", "animation.magus.walk_1", AzPlayBehaviors.LOOP);
    public static final AzCommand INTRO_COMMAND = AzCommand.create("dash", "animation.magus.intro", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand DASH = AzCommand.create("dash", "animation.magus.dashforward", AzPlayBehaviors.PLAY_ONCE);
    // Legacy cast clips. Nothing dispatches these any more — the families below replaced them —
    // but they stay resolvable, and the clips themselves stay in the legacy resource untouched.
    public static final AzCommand CASTING = AzCommand.create("attacks", "animation.magus.casting", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand CASTINGM = AzCommand.create("attacks", "animation.magus.casting2", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand CASTQUICKM = AzCommand.create("attacks", "animation.magus.cast.quick", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand CASTQUICK = AzCommand.create("attacks", "animation.magus.cast.quick2", AzPlayBehaviors.PLAY_ONCE);

    // ── v2 casting families ──────────────────────────────────────────────────
    //
    // The point of these is grammar, not variety: one recognisable body shape per class of spell,
    // with the key pose landing on the tick the spell actually resolves.
    //
    //   QUICK_PROJECTILE  right hand alone, 5-tick release, recovered by 14 — minimal commitment
    //   QUICK_NOVA        both hands break outward from the chest on the same 5-tick beat
    //   HEAVY_PROJECTILE  right hand crosses to the staff, 40-tick wind-up, 8-tick settle
    //   NOVA_LONG         gathers to centre of mass for 40 ticks, then breaks radially
    //   SHOCKWAVE         staff raised then driven down and forward — ground-directed intent
    //   CHANNEL           held overhead the whole cast, aura swelling — the catastrophic tell
    //   PHASE_TRANSITION  contract, hold, throw the guard wide open
    //
    // Recovery is baked into the tail of each heavy clip rather than living in a separate clip:
    // the release lands on tick 40 and the following 8 ticks return the staff to neutral, and the
    // action lock is held for those 8 ticks so nothing snaps straight back into locomotion.
    public static final AzCommand QUICK_PROJECTILE_COMMAND = AzCommand.create("attacks", "animation.magus.v2.cast.quick", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand NOVA_QUICK_COMMAND = AzCommand.create("attacks", "animation.magus.v2.cast.quick_nova", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand HEAVY_PROJECTILE_COMMAND = AzCommand.create("attacks", "animation.magus.v2.cast.heavy", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand NOVA_LONG_COMMAND = AzCommand.create("attacks", "animation.magus.v2.cast.nova", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand SHOCKWAVE_COMMAND = AzCommand.create("attacks", "animation.magus.v2.cast.shockwave", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand CHANNEL_COMMAND = AzCommand.create("attacks", "animation.magus.v2.cast.channel", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand PHASE_TRANSITION_COMMAND = AzCommand.create("dash", "animation.magus.v2.phase_transition", AzPlayBehaviors.PLAY_ONCE);

    // ── Barrier reactions ────────────────────────────────────────────────────
    //
    // Upper body only, on their own controller, so a barrier event reads over whatever cast is in
    // flight without wiping the cast's stance out from under it.
    public static final AzCommand BARRIER_COMMAND = AzCommand.create("reaction", "animation.magus.v2.barrier_shift", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand BARRIER_ABSORB_COMMAND = AzCommand.create("reaction", "animation.magus.v2.barrier_absorb", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand BARRIER_BREAK_COMMAND = AzCommand.create("reaction", "animation.magus.v2.barrier_break", AzPlayBehaviors.PLAY_ONCE);

    @Override
    public void registerControllers(AzAnimationControllerContainer<MagusPrimeEntity> container) {
        // Controller order is layering order: later controllers win the bones they animate.
        // Locomotion sits underneath, casts read over it, and a barrier reaction reads over both.
        container.add(
            AzAnimationController.<MagusPrimeEntity>builder(this, "walk").setTransitionLength(4).build(),
            AzAnimationController.<MagusPrimeEntity>builder(this, "attacks").setTransitionLength(2).build(),
            AzAnimationController.<MagusPrimeEntity>builder(this, "dash").setTransitionLength(2).build(),
            AzAnimationController.<MagusPrimeEntity>builder(this, "reaction").setTransitionLength(1).build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(MagusPrimeEntity animatable) {
        return ANIMATIONS;
    }
}

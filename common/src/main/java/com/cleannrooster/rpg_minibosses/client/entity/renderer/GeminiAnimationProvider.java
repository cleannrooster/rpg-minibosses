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

    /**
     * The v2 clip resource for the twins.
     *
     * <p>{@code animations/gemini.animations.json} (and its {@code gemini.animation.json} duplicate)
     * are intentionally left on disk, untouched, as reference material — every clip they held was
     * copied forward into this file by {@code tools/gen_gemini_magus_v2_animations.py}, so the legacy
     * names still resolve, and the authored v2 clips sit alongside them. Do not delete the old files.
     */
    private static final Identifier ANIMATIONS = Identifier.of(RPGMinibosses.CONTENT_NAMESPACE, "animations/gemini_v2.animations.json");

    // ── Legacy clips ─────────────────────────────────────────────────────────
    // Kept resolvable and kept here so nothing that still names them breaks. Live combat now
    // dispatches through the visual-grammar families below.
    public static final AzCommand IDLE_COMMAND = AzCommand.create("idle", "animation.awakener.idle", AzPlayBehaviors.LOOP);
    public static final AzCommand BEAM_COMMAND = AzCommand.create("beam", "animation.awakener.beam_1", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand BEAM_LARGE_COMMAND = AzCommand.create("beam_large", "animation.awakener.beam_large", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand METEOR_CHANNEL_COMMAND = AzCommand.create("meteor_channel", "animation.awakener.meteor_channel", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand METEOR_CHARGE_COMMAND = AzCommand.create("meteor_charge", "animation.awakener.meteor_charge", AzPlayBehaviors.PLAY_ONCE);

    // ── v2 visual grammar ────────────────────────────────────────────────────
    //
    // One body shape per class of threat, so a player can name the incoming attack from the
    // silhouette alone:
    //
    //   BEAM_SNAP     compact single-arm jab      quick projectile pressure   release on tick 10
    //   BEAM_CHANNEL  squares up, then sustains   beam / channel              releases on ticks 20/28/36
    //   COMET_CALL    overhead gather and throw   meteor / comet              releases on ticks 40/60/80/100
    //   CLOUD_SPREAD  arms wide and low, pushing  cloud / area denial         drops on ticks 40/60/80/100
    //
    // Each carries a mirrored twin: the fire Alpha leads with the right hand, the frost Beta with
    // the left, so the pair reads as related without looking like the same mob twice.
    private static final AzCommand BEAM_SNAP_FIRE = snap("");
    private static final AzCommand BEAM_SNAP_FROST = snap("_frost");
    private static final AzCommand BEAM_CHANNEL_FIRE = channel("");
    private static final AzCommand BEAM_CHANNEL_FROST = channel("_frost");
    private static final AzCommand COMET_FIRE = comet("");
    private static final AzCommand COMET_FROST = comet("_frost");
    private static final AzCommand CLOUD_FIRE = cloud("");
    private static final AzCommand CLOUD_FROST = cloud("_frost");

    /** Short handoff cues: the incoming PRIMARY opens up, the outgoing one closes down. */
    public static final AzCommand ROLE_ASCEND_COMMAND = AzCommand.create("role", "animation.awakener.v2.role_ascend", AzPlayBehaviors.PLAY_ONCE);
    public static final AzCommand ROLE_SETTLE_COMMAND = AzCommand.create("role", "animation.awakener.v2.role_settle", AzPlayBehaviors.PLAY_ONCE);

    private static AzCommand snap(String variant) {
        return AzCommand.create("beam", "animation.awakener.v2.beam_snap" + variant, AzPlayBehaviors.PLAY_ONCE);
    }

    private static AzCommand channel(String variant) {
        return AzCommand.create("beam_large", "animation.awakener.v2.beam_channel" + variant, AzPlayBehaviors.PLAY_ONCE);
    }

    private static AzCommand comet(String variant) {
        return AzCommand.create("meteor_channel", "animation.awakener.v2.comet_call" + variant, AzPlayBehaviors.PLAY_ONCE);
    }

    private static AzCommand cloud(String variant) {
        return AzCommand.create("meteor_charge", "animation.awakener.v2.cloud_spread" + variant, AzPlayBehaviors.PLAY_ONCE);
    }

    public static AzCommand beamSnap(boolean frost) {
        return frost ? BEAM_SNAP_FROST : BEAM_SNAP_FIRE;
    }

    public static AzCommand beamChannel(boolean frost) {
        return frost ? BEAM_CHANNEL_FROST : BEAM_CHANNEL_FIRE;
    }

    public static AzCommand cometCall(boolean frost) {
        return frost ? COMET_FROST : COMET_FIRE;
    }

    public static AzCommand cloudSpread(boolean frost) {
        return frost ? CLOUD_FROST : CLOUD_FIRE;
    }

    @Override
    public void registerControllers(AzAnimationControllerContainer<GeminiEntity> container) {
        // Controller order is layering order: later controllers win the bones they animate, so a
        // cast always reads over the hover underneath it, and a role handoff reads over everything.
        // The short transitions replace the old hard cuts between poses.
        // The handoff sits just above the hover and below the casts: a role swap should read over
        // "suspended, waiting", but must never sit on top of a cast telegraph.
        container.add(
            AzAnimationController.<GeminiEntity>builder(this, "idle").setTransitionLength(3).build(),
            AzAnimationController.<GeminiEntity>builder(this, "role").setTransitionLength(2).build(),
            AzAnimationController.<GeminiEntity>builder(this, "meteor_channel").setTransitionLength(2).build(),
            AzAnimationController.<GeminiEntity>builder(this, "meteor_charge").setTransitionLength(2).build(),
            AzAnimationController.<GeminiEntity>builder(this, "beam").setTransitionLength(2).build(),
            AzAnimationController.<GeminiEntity>builder(this, "beam_large").setTransitionLength(2).build()
        );
    }

    @Override
    public @NotNull Identifier getAnimationLocation(GeminiEntity animatable) {
        return ANIMATIONS;
    }
}

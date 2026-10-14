package com.cleannrooster.anarchy_rpg.magic.fx;

import dev.eann.atlasvfx.cue.FxCueArgs;
import dev.eann.atlasvfx.cue.FxCues;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import dev.eann.atlasvfx.telegraph.TelegraphTiming;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * The server's side of boss effects: cues and telegraphs go out through AtlasVFX's cue layer
 * ({@link dev.eann.atlasvfx.cue.FxEmitter}, which declares its own payloads), sounds are played here. Safe on
 * a dedicated server.
 */
public final class FxEmitter {

    private FxEmitter() {
    }

    public static void emit(ServerWorld world, Identifier cue, FxCueArgs args) {
        dev.eann.atlasvfx.cue.FxEmitter.emit(world, cue, args);
    }

    /** Warn of {@code area} (already placed and turned), landing {@code windupTicks} from now. */
    public static void telegraph(ServerWorld world, TelegraphArea area, int windupTicks, boolean helpful) {
        telegraph(world, area, windupTicks, helpful, 0);
    }

    /** As above, drawn in {@code colour}; 0 for the usual look (red, or green when {@code helpful}). */
    public static void telegraph(ServerWorld world, TelegraphArea area, int windupTicks, boolean helpful, int colour) {
        var timing = TelegraphTiming.starting(world.getTime(), windupTicks,
                dev.eann.atlasvfx.cue.FxEmitter.TELEGRAPH_FADE_TICKS);
        dev.eann.atlasvfx.cue.FxEmitter.telegraph(world, area, timing, helpful ? FxCues.HELPFUL : FxCues.DANGER, colour);
    }

    public static void sound(ServerWorld world, Vec3d at, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.HOSTILE, volume, pitch);
    }
}

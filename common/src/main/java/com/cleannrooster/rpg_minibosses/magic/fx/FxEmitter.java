package com.cleannrooster.rpg_minibosses.magic.fx;

import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import dev.eann.atlasvfx.telegraph.TelegraphArea;
import dev.eann.atlasvfx.telegraph.TelegraphTiming;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

/**
 * The server's side of the cue layer: tells nearby clients to draw a cue or a telegraph, and plays
 * sounds. Nothing here touches AtlasVFX's client classes — only its server-safe telegraph geometry —
 * so it is safe on a dedicated server.
 *
 * <p>Kept behind this one class (and {@code client.fx.CueHandlers} on the other side) so the layer can
 * later move into AtlasVFX without its callers changing.</p>
 */
public final class FxEmitter {
    /** Players further than this from a cue never hear of it. */
    private static final double RANGE = 96.0;

    private FxEmitter() {
    }

    /** Declare the payload types on a dedicated server; a client declares them with its receivers. */
    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(FxCuePayload.ID, FxCuePayload.CODEC);
            NetworkManager.registerS2CPayloadType(TelegraphPayload.ID, TelegraphPayload.CODEC);
        }
    }

    public static void emit(ServerWorld world, Identifier cue, CueArgs args) {
        send(world, args.at(), new FxCuePayload(cue, args));
    }

    /** Warn of {@code area} (already placed and turned), landing {@code windupTicks} from now. */
    public static void telegraph(ServerWorld world, TelegraphArea area, int windupTicks, boolean helpful) {
        var timing = TelegraphTiming.starting(world.getTime(), windupTicks, 8);
        send(world, area.origin(), new TelegraphPayload(area, timing, helpful));
    }

    public static void sound(ServerWorld world, Vec3d at, SoundEvent sound, float volume, float pitch) {
        world.playSound(null, at.x, at.y, at.z, sound, SoundCategory.HOSTILE, volume, pitch);
    }

    private static void send(ServerWorld world, Vec3d at, CustomPayload payload) {
        double rangeSq = RANGE * RANGE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(at) <= rangeSq && NetworkManager.canPlayerReceive(player, payload.getId())) {
                NetworkManager.sendToPlayer(player, payload);
            }
        }
    }
}

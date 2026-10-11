package com.cleannrooster.rpg_minibosses.client.fx;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import com.cleannrooster.rpg_minibosses.magic.entity.BossCloud;
import com.cleannrooster.rpg_minibosses.magic.entity.BossProjectile;
import com.cleannrooster.rpg_minibosses.magic.fx.CueArgs;
import com.cleannrooster.rpg_minibosses.magic.fx.FxCuePayload;
import com.cleannrooster.rpg_minibosses.magic.fx.FxCues;
import com.cleannrooster.rpg_minibosses.magic.fx.TelegraphPayload;
import dev.architectury.networking.NetworkManager;
import dev.eann.atlasvfx.FxDensity;
import dev.eann.atlasvfx.FxShapes;
import dev.eann.atlasvfx.FxWorld;
import dev.eann.atlasvfx.telegraph.Telegraph;
import dev.eann.atlasvfx.telegraph.TelegraphStyle;
import dev.eann.atlasvfx.telegraph.TelegraphStyles;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.HashMap;
import java.util.Map;

/**
 * The client's side of the cue layer: turns cue and telegraph packets into AtlasVFX shapes, and draws the
 * ability carriers in flight. A boss with a bespoke look registers its own cue here and emits it with
 * {@code FxEmitter} like any built-in one.
 */
public final class CueHandlers {
    /** Draws one cue. Runs on the client thread. */
    @FunctionalInterface
    public interface Handler {
        void play(FxWorld world, CueArgs args);
    }

    /** This mod's share of AtlasVFX's density; the game's particle setting already scales it. */
    public static final FxDensity DENSITY = FxDensity.FULL;

    private static final TelegraphStyle HELPFUL = TelegraphStyles.of(0x66FF66);
    private static final Map<Identifier, Handler> HANDLERS = new HashMap<>();

    private CueHandlers() {
    }

    public static void register(Identifier cue, Handler handler) {
        HANDLERS.put(cue, handler);
    }

    /** Declare the payloads and their receivers. Must run during mod construction (see {@code earlyInit}). */
    public static void registerReceivers() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, FxCuePayload.ID, FxCuePayload.CODEC,
                (payload, context) -> context.queue(() -> play(payload)));
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, TelegraphPayload.ID, TelegraphPayload.CODEC,
                (payload, context) -> context.queue(() -> telegraph(payload)));
    }

    /** Built-in cues and the carriers' in-flight visuals. Called from client init. */
    public static void init() {
        register(FxCues.PULSE, (world, a) -> world.play(FxShapes.pulse(style(a), a.at(), a.size(), a.tier()), DENSITY));
        register(FxCues.NOVA, (world, a) -> world.play(FxShapes.nova(style(a), a.at(), a.size(), a.tier(), a.seed()), DENSITY));
        register(FxCues.EXPLOSION, (world, a) -> world.play(FxShapes.explosion(style(a), a.at(), a.size(), a.tier(), a.seed()), DENSITY));
        register(FxCues.BLAST, (world, a) -> world.play(FxShapes.blast(style(a), a.at(), a.size(), a.tier(), a.seed(), false), DENSITY));
        register(FxCues.BLAST_FALLING, (world, a) -> world.play(FxShapes.blast(style(a), a.at(), a.size(), a.tier(), a.seed(), true), DENSITY));
        register(FxCues.BEAM, (world, a) -> world.play(FxShapes.beam(style(a), a.at(), a.to(), a.size()), DENSITY));
        register(FxCues.ARC, (world, a) -> world.play(FxShapes.arc(style(a), a.at(), a.to(), a.tier(), a.seed()), DENSITY));
        register(FxCues.CONE, (world, a) -> world.play(FxShapes.cone(style(a), a.at(), a.to(), a.angle(), a.size(), a.tier(), a.seed()), DENSITY));
        register(FxCues.BURST, (world, a) -> FxShapes.burst(world, style(a), a.at(), a.size(), 12, a.seed(), DENSITY));
        register(FxCues.WISP, (world, a) -> FxShapes.wisp(world, style(a), a.at(), a.size(), 16, DENSITY.effective()));
        register(FxCues.SMOKE, (world, a) -> FxShapes.smoke(world, a.at(), a.size(), 24, DENSITY.effective()));
        register(FxCues.RAIN, (world, a) -> FxShapes.rain(world, style(a), a.at(), a.size(), 16, DENSITY.effective()));

        BossProjectile.clientTick = CueHandlers::projectileTick;
        BossCloud.clientTick = CueHandlers::cloudTick;
    }

    private static void play(FxCuePayload payload) {
        var handler = HANDLERS.get(payload.cue());
        if (handler == null) {
            RPGMinibosses.LOGGER.debug("[{}] no client handler for cue {}", RPGMinibosses.CONTENT_NAMESPACE, payload.cue());
            return;
        }
        handler.play(FxWorld.get(), payload.args());
    }

    private static void telegraph(TelegraphPayload payload) {
        var style = payload.helpful() ? HELPFUL : TelegraphStyles.DANGER;
        FxWorld.get().play(Telegraph.fixed(payload.area(), payload.timing(), style), DENSITY);
    }

    /** A projectile's body and wake: a bright mote where it is, sparks shed behind. */
    private static void projectileTick(BossProjectile projectile) {
        var style = SchoolStyles.of(projectile.school());
        var world = FxWorld.get();
        var at = projectile.getPos().add(0.0, projectile.getHeight() * 0.5, 0.0);
        world.spawn(style.accent(), at, Vec3d.ZERO, 2.5f);
        world.spray(style.debris(), at, 0.3f, Math.max(1, Math.round(2 * DENSITY.effective())));
    }

    /** A cloud's lingering motes, between its damaging pulses. */
    private static void cloudTick(BossCloud cloud) {
        if (cloud.age % 4 != 0) {
            return;
        }
        var style = SchoolStyles.of(cloud.school());
        FxShapes.wisp(FxWorld.get(), style, cloud.getPos().add(0.0, 0.3, 0.0), cloud.radius(), 6, DENSITY.effective());
    }

    private static dev.eann.atlasvfx.FxStyle style(CueArgs args) {
        return SchoolStyles.of(args.school());
    }
}

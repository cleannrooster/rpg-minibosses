package com.cleannrooster.rpg_minibosses.client.fx;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import dev.eann.atlasvfx.trail.TrailStyles;
import dev.eann.atlasvfx.trail.Trail;
import dev.eann.atlasvfx.trail.SweepFrameSource;
import dev.eann.atlasvfx.trail.SweepFrame;
import dev.eann.atlasvfx.trail.ArcSweep;
import com.cleannrooster.rpg_minibosses.magic.CastTarget;
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
        register(FxCues.SLASH, (world, a) -> world.play(Trail.slash(
                SweepFrameSource.fixed(SweepFrame.of(a.at(), CastTarget.yawOf(a.to()), 0.0f)), a.size(), a.angle(),
                a.tier() == 1, ArcSweep.Plane.HORIZONTAL, 5, 6, TrailStyles.slash(a.school().color()), a.seed()), DENSITY));
        register(FxCues.SMOKE, (world, a) -> FxShapes.smoke(world, a.at(), a.size(), 24, DENSITY.effective()));
        register(FxCues.RAIN, (world, a) -> FxShapes.rain(world, style(a), a.at(), a.size(), 16, DENSITY.effective()));
        register(FxCues.GUIDE_LINE, (world, a) -> world.add(GuideShapes.line(style(a), a.at(), a.to(), a.size()), DENSITY));
        register(FxCues.GUIDE_RING, (world, a) -> world.add(GuideShapes.ring(style(a), a.at(), a.to(), a.size()), DENSITY));
        register(FxCues.HOLY_STRIKE, CueHandlers::holyStrike);

        BossProjectile.clientTick = CueHandlers::projectileTick;
        BossCloud.clientTick = CueHandlers::cloudTick;
        GroundFx.register();
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

    /** A cloud's ground overlay ({@link GroundFx}), and its lingering motes between its damaging pulses. */
    private static void cloudTick(BossCloud cloud) {
        GroundFx.track(cloud);
        if (cloud.age % 4 != 0) {
            return;
        }
        var style = SchoolStyles.of(cloud.school());
        FxShapes.wisp(FxWorld.get(), style, cloud.getPos().add(0.0, 0.3, 0.0), cloud.radius(), 6, DENSITY.effective());
    }

    /** Bolts in a holy strike, and how high above the ground they come out of the sky. */
    private static final int HOLY_BOLTS = 5;
    private static final double HOLY_BOLT_HEIGHT = 9.0;

    /** Holy lightning: a shaft down onto the spot, forked bolts out of the sky across the area, a flash. */
    private static void holyStrike(FxWorld world, CueArgs a) {
        var holy = SchoolStyles.holy();
        var random = new java.util.Random(a.seed());
        world.play(FxShapes.blast(holy, a.at(), a.size(), 1, a.seed(), true), DENSITY);
        world.play(FxShapes.explosion(holy, a.at(), a.size(), 1, a.seed()), DENSITY);
        // One bolt straight down the middle, the rest scattered over the area; each starts a little off to the
        // side of where it lands, so they slant like real strikes rather than standing as straight posts.
        for (int i = 0; i < HOLY_BOLTS; i++) {
            double r = i == 0 ? 0.0 : a.size() * Math.sqrt(random.nextDouble());
            double around = random.nextDouble() * Math.PI * 2.0;
            var ground = a.at().add(Math.cos(around) * r, 0.1, Math.sin(around) * r);
            var sky = ground.add((random.nextDouble() - 0.5) * 3.0, HOLY_BOLT_HEIGHT * (0.8 + 0.4 * random.nextDouble()),
                    (random.nextDouble() - 0.5) * 3.0);
            world.play(FxShapes.arc(holy, sky, ground, i == 0 ? 2 : 1, random.nextLong()), DENSITY);
        }
    }

    private static dev.eann.atlasvfx.FxStyle style(CueArgs args) {
        return SchoolStyles.of(args.school());
    }
}

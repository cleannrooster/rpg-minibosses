package com.cleannrooster.rpg_minibosses.client.fx;

import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import com.cleannrooster.rpg_minibosses.magic.entity.BossCloud;
import com.cleannrooster.rpg_minibosses.magic.entity.BossProjectile;
import com.cleannrooster.rpg_minibosses.magic.fx.BossCues;
import dev.eann.atlasvfx.FxDensity;
import dev.eann.atlasvfx.FxShapes;
import dev.eann.atlasvfx.FxStyle;
import dev.eann.atlasvfx.FxWorld;
import dev.eann.atlasvfx.cue.FxCueArgs;
import dev.eann.atlasvfx.cue.FxCueHandlers;
import dev.eann.atlasvfx.cue.FxCueStyles;
import net.minecraft.util.math.Vec3d;

/**
 * The client's side of boss effects. AtlasVFX's cue layer receives the packets and draws its built-in cues;
 * this registers what is this mod's own: a look per school (under {@link MagicSchool#styleId()}), the bespoke
 * cues in {@link BossCues}, and the ability carriers' visuals in flight.
 */
public final class CueHandlers {
    /** This mod's share of AtlasVFX's density; the game's particle setting already scales it. */
    public static final FxDensity DENSITY = FxDensity.FULL;

    private CueHandlers() {
    }

    /** School looks, bespoke cues and the carriers' in-flight visuals. Called from client init. */
    public static void init() {
        for (MagicSchool school : MagicSchool.values()) {
            FxCueStyles.register(school.styleId(), SchoolStyles.of(school));
        }
        FxCueHandlers.register(BossCues.GUIDE_LINE, (world, a) -> world.add(GuideShapes.line(style(a), a.at(), a.to(), a.size()), DENSITY));
        FxCueHandlers.register(BossCues.GUIDE_RING, (world, a) -> world.add(GuideShapes.ring(style(a), a.at(), a.to(), a.size()), DENSITY));
        FxCueHandlers.register(BossCues.HOLY_STRIKE, CueHandlers::holyStrike);

        BossProjectile.clientTick = CueHandlers::projectileTick;
        BossCloud.clientTick = CueHandlers::cloudTick;
        GroundFx.register();
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
    private static void holyStrike(FxWorld world, FxCueArgs a) {
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

    private static FxStyle style(FxCueArgs args) {
        return FxCueStyles.style(args.style());
    }
}

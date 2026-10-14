package com.cleannrooster.anarchy_rpg.client.fx;

import dev.eann.atlasvfx.FxLife;
import dev.eann.atlasvfx.FxPainter;
import dev.eann.atlasvfx.FxShape;
import dev.eann.atlasvfx.FxStyle;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;

/**
 * Warning marks, drawn so they cannot be mistaken for the attack itself: faint, in the school's soft glow only
 * - no white-hot core, no end flares, no sparkles - and each one fades in and out over its short life, so a
 * guide re-sent every few ticks breathes rather than burns. AtlasVFX's own beam is fixed at full strength,
 * which is what the warning used to borrow.
 */
final class GuideShapes {
    /** How long one flash of a guide lives; a little longer than the interval it is re-sent at, so they overlap. */
    private static final int TICKS = 6;
    private static final float LINE_ALPHA = 0.22f;
    private static final float RING_ALPHA = 0.28f;
    private static final float RING_WIDTH = 0.08f;

    private GuideShapes() {
    }

    static FxShape line(FxStyle style, Vec3d from, Vec3d to, float width) {
        return new Line(style, from, to, width);
    }

    /** A ring of {@code radius} around {@code at}, lying across {@code axis}. */
    static FxShape ring(FxStyle style, Vec3d at, Vec3d axis, float radius) {
        var normal = axis.lengthSquared() < 1.0e-6 ? new Vec3d(0.0, 1.0, 0.0) : axis.normalize();
        // The painter's ring lies in its own x/y plane: turn its normal (+z) onto the axis.
        var plane = new Quaternionf().rotationTo(0.0f, 0.0f, 1.0f, (float) normal.x, (float) normal.y, (float) normal.z);
        return new Ring(style, at, plane, radius);
    }

    /** 0 at birth and death, 1 in the middle: a soft swell rather than a flash. */
    private static float swell(FxLife life, float tickDelta) {
        return MathHelper.sin(life.progress(tickDelta, TICKS) * MathHelper.PI);
    }

    private record Line(FxStyle style, Vec3d from, Vec3d to, float width) implements FxShape.Timed {
        @Override
        public int ticks() {
            return TICKS;
        }

        @Override
        public void draw(FxPainter painter, @Nullable ClientWorld world, FxLife life, float tickDelta) {
            painter.beam(this.from, this.to, Math.max(0.03f, this.width), this.style.glow(), LINE_ALPHA * swell(life, tickDelta));
        }
    }

    private record Ring(FxStyle style, Vec3d at, Quaternionf plane, float radius) implements FxShape.Timed {
        @Override
        public int ticks() {
            return TICKS;
        }

        @Override
        public void draw(FxPainter painter, @Nullable ClientWorld world, FxLife life, float tickDelta) {
            painter.ring(this.at, this.plane, this.radius, RING_WIDTH, this.style.glow(), RING_ALPHA * swell(life, tickDelta));
        }
    }
}

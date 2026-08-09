package com.cleannrooster.rpg_minibosses.client.combat;

import com.cleannrooster.rpg_minibosses.entity.combat.SlashProfile;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackFrame;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackGeometry;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackShape;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackVolume;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Draws an attack's swept surface as a textured ribbon.
 *
 * <p>The mesh is generated entirely from {@link AttackGeometry} — the same function the server queries
 * when deciding whether you were hit. A horizontal sweep therefore renders as a horizontal crescent of
 * exactly its real reach and arc, a rising cut as a rising arc, a charge as a lane the length of its
 * actual trail. Nothing here is tuned per mob; a new attack gets a correct visual by existing.
 *
 * <p>The ribbon is animated <em>through</em> the swing rather than shown whole: the leading edge advances
 * with the server's swing progress and the tail follows a fixed distance behind, so the arc is drawn being
 * cut.
 *
 * <p>Ported from the Divine Encounters combat system.
 */
public final class SlashRenderer {

    /** Emissive slashes are drawn fullbright so they read at night and in caves. */
    private static final int FULL_BRIGHT = LightmapTextureManager.pack(15, 15);

    private SlashRenderer() {
    }

    static void render(SlashEffect effect, MatrixStack matrices, Camera camera,
                       VertexConsumerProvider consumers, float partialTick) {
        var action = effect.action();
        var profile = action.ribbon();
        var volume = action.volume();
        if (profile == null || volume == null || !profile.isVisible()
                || volume.shape().family() == AttackShape.Family.RADIAL) {
            return;
        }
        var alpha = profile.alpha() * effect.fade(partialTick);
        if (alpha <= 0.01f) {
            return;
        }

        var grid = buildGrid(effect, volume, profile, partialTick);
        if (grid == null) {
            return;
        }

        var layer = profile.emissive()
                ? RenderLayer.getEntityTranslucentEmissive(profile.texture())
                : RenderLayer.getEntityTranslucent(profile.texture());
        var consumer = consumers.getBuffer(layer);

        var cameraPos = camera.getPos();
        var normal = AttackGeometry.planeNormal(volume, effect.frame());
        // Clamped to the attack's real half-extent, so however solid the blade is made to look it can
        // never occupy space the damage volume does not.
        var thickness = Math.min(profile.thickness(),
                (float) AttackGeometry.halfThickness(volume, effect.frame()));

        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // The bloom goes down first, underneath the real arc.
        //
        // This is the only geometry drawn outside the damage volume, and it is drawn in a deliberately
        // different register so it cannot be mistaken for the volume: a fraction of the alpha, fading to
        // nothing at its outer limit, and with no thickness of its own. The bright, solid ribbon laid over
        // it is still exactly what hits you. Drawing it first also means it can never occlude the honest
        // shape — the arc's own edge stays the brightest thing on screen, which is what a player actually
        // reads reach from.
        if (profile.hasOverreach()) {
            var bloom = buildOverreach(effect, volume, profile, partialTick);
            if (bloom != null) {
                emit(consumer, matrices.peek(), bloom, profile,
                        alpha * SlashProfile.OVERREACH_ALPHA, normal);
            }
        }

        if (thickness > 0.001f) {
            // Two parallel sheets read as a slab with real presence. One sheet displaced back and forth
            // just looks like crumpled paper.
            emit(consumer, matrices.peek(), offsetGrid(grid, normal, thickness), profile, alpha, normal);
            emit(consumer, matrices.peek(), offsetGrid(grid, normal, -thickness), profile, alpha,
                    normal.multiply(-1.0));
        } else {
            emit(consumer, matrices.peek(), grid, profile, alpha, normal);
        }
        matrices.pop();
    }

    /**
     * The ribbon as a grid of world-space vertices plus a per-row brightness ramp.
     *
     * @param points   {@code [row][column]}; rows run along the long axis, columns across it
     * @param rowAlpha per-row opacity — the trailing end of a swing fades out behind the leading edge
     * @param colAlpha per-column opacity, or null for fully opaque across the blade. Only the outer bloom
     *                 uses it, to dissolve as it projects past the real reach.
     */
    private record Ribbon(Vec3d[][] points, float[] rowAlpha, float @Nullable [] colAlpha) {
        Ribbon(Vec3d[][] points, float[] rowAlpha) {
            this(points, rowAlpha, null);
        }

        float alphaAt(int row, int column) {
            var across = this.colAlpha == null ? 1.0f : this.colAlpha[column];
            return this.rowAlpha[row] * across;
        }
    }

    private static @Nullable Ribbon buildGrid(SlashEffect effect, AttackVolume volume,
                                              SlashProfile profile, float partialTick) {
        return switch (volume.shape().family()) {
            case ARC -> buildArc(effect, volume, profile, partialTick);
            case LANE, PATH -> buildLane(effect, volume, profile, partialTick);
            case RADIAL -> null;
        };
    }

    /**
     * A crescent: rows step through the swept angles, columns step from the inner radius out to the tip.
     * Leaving the innermost fraction empty is what turns a pie wedge into a blade.
     */
    private static @Nullable Ribbon buildArc(SlashEffect effect, AttackVolume volume,
                                             SlashProfile profile, float partialTick) {
        var frame = effect.frame();
        var lead = effect.leadingEdge(partialTick);
        var tail = effect.trailingEdge(partialTick);
        if (lead - tail < 1.0e-4f) {
            return null;
        }
        // Cones show their whole span from the first frame; swept arcs reveal it progressively.
        if (!volume.shape().isSwept()) {
            tail = 0.0f;
            lead = 1.0f;
        }

        var rows = Math.max(2, profile.sweepSegments());
        var cols = Math.max(1, profile.bladeSegments());
        var points = new Vec3d[rows + 1][cols + 1];
        var rowAlpha = new float[rows + 1];

        for (var r = 0; r <= rows; r++) {
            var rowFraction = (float) r / rows;
            var s = MathHelper.lerp(rowFraction, tail, lead);
            // Brightest at the leading edge, dissolving toward the tail of the trail.
            rowAlpha[r] = rowFraction * rowFraction * (3.0f - 2.0f * rowFraction);
            for (var c = 0; c <= cols; c++) {
                var t = MathHelper.lerp((float) c / cols, profile.innerFraction(), 1.0f);
                points[r][c] = AttackGeometry.surfacePoint(volume, frame, s, t);
            }
        }
        return new Ribbon(points, rowAlpha);
    }

    /**
     * A thrust or charge trail: rows step along the lane, columns across its width. The lane only extends
     * as far as the swing has progressed, so it visibly reaches out.
     */
    private static @Nullable Ribbon buildLane(SlashEffect effect, AttackVolume volume,
                                              SlashProfile profile, float partialTick) {
        var frame = effect.frame();
        var extension = effect.leadingEdge(partialTick);
        if (extension <= 1.0e-4f) {
            return null;
        }

        var rows = Math.max(2, profile.bladeSegments());
        var across = AttackGeometry.planeAxis(volume, frame)
                .multiply(AttackGeometry.widthOf(volume, frame) * 0.5);
        var points = new Vec3d[rows + 1][2];
        var rowAlpha = new float[rows + 1];

        for (var r = 0; r <= rows; r++) {
            var t = (float) r / rows;
            var centre = AttackGeometry.surfacePoint(volume, frame, extension, t);
            // Taper toward the root so the lane reads as a spearhead rather than a plank.
            var taper = 0.35f + 0.65f * t;
            points[r][0] = centre.subtract(across.multiply(taper));
            points[r][1] = centre.add(across.multiply(taper));
            rowAlpha[r] = 0.3f + 0.7f * t;
        }
        return new Ribbon(points, rowAlpha);
    }

    /** Radial subdivisions of the bloom. Few: it is a gradient, not a shape. */
    private static final int OVERREACH_SEGMENTS = 3;

    /**
     * The outer bloom: a band projecting past the attack's real reach, fading out as it goes.
     *
     * <p>It shares the arc's swept angles exactly, so it is the same shape pointing the same way — only
     * longer. That is what keeps it reading as force being <em>projected along the strike</em> rather than
     * as a decal pasted over it. It starts at the tip ({@code t = 1}, the real limit) and runs outward, so
     * it never overlaps or brightens the damaging region.
     *
     * <p>Returns null for shapes with no swept arc; a lane's reach is already its whole visual.
     */
    private static @Nullable Ribbon buildOverreach(SlashEffect effect, AttackVolume volume,
                                                   SlashProfile profile, float partialTick) {
        if (volume.shape().family() != AttackShape.Family.ARC) {
            return null;
        }
        var frame = effect.frame();
        var lead = effect.leadingEdge(partialTick);
        var tail = effect.trailingEdge(partialTick);
        if (lead - tail < 1.0e-4f) {
            return null;
        }
        if (!volume.shape().isSwept()) {
            tail = 0.0f;
            lead = 1.0f;
        }

        var rows = Math.max(2, profile.sweepSegments() / 2);
        var cols = OVERREACH_SEGMENTS;
        var points = new Vec3d[rows + 1][cols + 1];
        var rowAlpha = new float[rows + 1];
        var colAlpha = new float[cols + 1];

        // Full strength where it meets the blade tip, nothing at its outer limit. Squared, so it falls
        // away quickly rather than lingering as a halo.
        for (var c = 0; c <= cols; c++) {
            var outward = (float) c / cols;
            colAlpha[c] = (1.0f - outward) * (1.0f - outward);
        }

        for (var r = 0; r <= rows; r++) {
            var rowFraction = (float) r / rows;
            var s = MathHelper.lerp(rowFraction, tail, lead);
            rowAlpha[r] = rowFraction * rowFraction * (3.0f - 2.0f * rowFraction);
            for (var c = 0; c <= cols; c++) {
                // t runs from 1 (the real tip) outward to 1 + overreach.
                var t = 1.0f + profile.overreach() * ((float) c / cols);
                points[r][c] = overreachPoint(volume, frame, s, t);
            }
        }
        return new Ribbon(points, rowAlpha, colAlpha);
    }

    /**
     * The arc's surface extended past {@code t = 1}.
     *
     * <p>{@link AttackGeometry#surfacePoint} deliberately clamps {@code t} to the real reach — that clamp
     * is what guarantees particles can never spawn outside the damage volume, and it is not something to
     * relax. So the bloom computes its own radius here instead, using the same angle and the same plane,
     * and reaches past the limit without ever asking the geometry to lie about where the limit is.
     */
    private static Vec3d overreachPoint(AttackVolume volume, AttackFrame frame, float s, float t) {
        var angle = Math.toRadians(AttackGeometry.sweepAngleDegrees(volume, frame, s));
        var plane = AttackGeometry.planeAxis(volume, frame);
        var direction = frame.forward().multiply(Math.cos(angle)).add(plane.multiply(Math.sin(angle)));
        var radius = (volume.innerRadius()
                + (volume.range() - volume.innerRadius()) * t) * frame.scale();
        return frame.origin().add(direction.multiply(radius));
    }

    /** A copy of the ribbon displaced bodily along the swing plane's normal — one face of the slab. */
    private static Ribbon offsetGrid(Ribbon ribbon, Vec3d normal, float distance) {
        var source = ribbon.points();
        var shifted = new Vec3d[source.length][];
        var offset = normal.multiply(distance);
        for (var r = 0; r < source.length; r++) {
            shifted[r] = new Vec3d[source[r].length];
            for (var c = 0; c < source[r].length; c++) {
                shifted[r][c] = source[r][c].add(offset);
            }
        }
        return new Ribbon(shifted, ribbon.rowAlpha());
    }

    private static void emit(VertexConsumer consumer, MatrixStack.Entry pose, Ribbon ribbon,
                             SlashProfile profile, float alpha, Vec3d normal) {
        var points = ribbon.points();
        var rows = points.length - 1;
        var cols = points[0].length - 1;

        for (var r = 0; r < rows; r++) {
            var u0 = (float) r / rows;
            var u1 = (float) (r + 1) / rows;
            for (var c = 0; c < cols; c++) {
                var v0 = (float) c / cols;
                var v1 = (float) (c + 1) / cols;
                var p00 = points[r][c];
                var p10 = points[r + 1][c];
                var p11 = points[r + 1][c + 1];
                var p01 = points[r][c + 1];

                // Per-corner rather than per-row, so a ribbon that fades across the blade as well as along
                // the swing — which is exactly what the outer bloom does — gets a smooth gradient in both
                // directions instead of banding.
                var a00 = alpha * ribbon.alphaAt(r, c);
                var a01 = alpha * ribbon.alphaAt(r, c + 1);
                var a11 = alpha * ribbon.alphaAt(r + 1, c + 1);
                var a10 = alpha * ribbon.alphaAt(r + 1, c);

                quad(consumer, pose, profile, normal,
                        p00, u0, v0, a00, p01, u0, v1, a01, p11, u1, v1, a11, p10, u1, v0, a10);
                // Reverse winding: the slash has to be visible from whichever side the player is on.
                quad(consumer, pose, profile, normal.multiply(-1.0),
                        p10, u1, v0, a10, p11, u1, v1, a11, p01, u0, v1, a01, p00, u0, v0, a00);
            }
        }
    }

    private static void quad(VertexConsumer consumer, MatrixStack.Entry pose, SlashProfile profile,
                             Vec3d normal,
                             Vec3d a, float au, float av, float aa,
                             Vec3d b, float bu, float bv, float ba,
                             Vec3d c, float cu, float cv, float ca,
                             Vec3d d, float du, float dv, float da) {
        vertex(consumer, pose, profile, normal, a, au, av, aa);
        vertex(consumer, pose, profile, normal, b, bu, bv, ba);
        vertex(consumer, pose, profile, normal, c, cu, cv, ca);
        vertex(consumer, pose, profile, normal, d, du, dv, da);
    }

    private static void vertex(VertexConsumer consumer, MatrixStack.Entry pose, SlashProfile profile,
                               Vec3d normal, Vec3d position, float u, float v, float alpha) {
        consumer.vertex(pose, (float) position.x, (float) position.y, (float) position.z)
                .color(profile.red(), profile.green(), profile.blue(), MathHelper.clamp(alpha, 0.0f, 1.0f))
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(FULL_BRIGHT)
                .normal(pose, (float) normal.x, (float) normal.y, (float) normal.z);
    }
}

package com.cleannrooster.rpg_minibosses.client.fx;

import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import com.cleannrooster.rpg_minibosses.magic.entity.BossCloud;
import dev.eann.atlasvfx.AtlasRef;
import dev.eann.atlasvfx.FxLayers;
import dev.eann.atlasvfx.FxLight;
import dev.eann.atlasvfx.FxPainter;
import dev.eann.atlasvfx.FxPass;
import dev.eann.atlasvfx.FxSprite;
import dev.eann.atlasvfx.WorldRenderHook;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * A lingering cloud's ground, drawn as the thing it is: each covered column shows a real block face lying
 * on the floor - magma whose cracks breathe light under fire, frosted ice that cracks as it ends, water
 * crackling with arcs under lightning - with light and life over it: flames standing up, glints on ice,
 * a bright front running out as it spreads. The same look as Mo' Mana's ground effects.
 *
 * <p>The outline is feathered: every corner is shared by up to four cells and fades by how many of them
 * the patch covers, so the edge thins out instead of stopping at a square. The covered cells are the
 * flood's ({@link GroundPatch}), worked out once per cloud.</p>
 */
public final class GroundFx {
    /** Light and life lie this far above the surface they decorate, so they never fight it. */
    private static final float OVERLAY_LIFT = 0.006f;
    private static final Quaternionf FLAT = new Quaternionf().rotationX((float) (Math.PI / 2.0));
    /** Ticks per flood step before the overlay reaches a cell; ticks it spends washing in; and fading out. */
    private static final double ARRIVAL_TICKS = 1.0;
    private static final double SPREAD_TICKS = 5.0;
    private static final double FADE_TICKS = 18.0;
    /** How opaque the surface settles at, and how bright its arriving edge is. */
    private static final float SETTLED = 0.8f;
    private static final float SPREAD = 1.0f;

    private static final FxSprite WATER = FxSprite.blocks("water_still");
    private static final FxSprite MAGMA = FxSprite.blocks("magma");
    private static final FxSprite SOUL_SOIL = FxSprite.blocks("soul_soil");
    private static final FxSprite AMETHYST = FxSprite.blocks("amethyst_block");
    private static final FxSprite MOSS = FxSprite.blocks("moss_block");
    private static final FxSprite FIRE = FxSprite.blocks("fire_0", "fire_1");
    private static final FxSprite SOUL_FIRE = FxSprite.blocks("soul_fire_0", "soul_fire_1");
    private static final FxSprite ICE_STAGES = FxSprite.blocks("frosted_ice_0", "frosted_ice_1", "frosted_ice_2", "frosted_ice_3");
    private static final FxSprite GLITTER = FxSprite.particleRun("glitter", 8);
    private static final FxSprite SPARK = FxSprite.particleRun("spark", 8);

    /** How one school's ground looks. */
    private record Look(FxSprite base, int baseTint, @Nullable FxSprite glow, int glowTint, float glowAlpha,
                        float glowBeat, @Nullable FxSprite glint, int glintTint, int glintEvery, int hot,
                        @Nullable FxSprite flames, boolean arcs, boolean cracks) {
    }

    private static Look look(MagicSchool school) {
        return switch (school) {
            // Magma: dark, its cracks breathing light, and flames standing up out of it.
            case FIRE -> new Look(MAGMA, 0x8A6A60, MAGMA, 0xFFFFFF, 0.85f, 0.12f, null, 0, 0, 0xFFD08A,
                    FIRE, false, false);
            // Soul soil, burning blue.
            case SOUL -> new Look(SOUL_SOIL, 0x8A8A9A, null, 0, 0.0f, 0.0f, null, 0, 0, 0x9FF5FF,
                    SOUL_FIRE, false, false);
            // Frosted ice, cracking as it nears its end, with frost glittering on it.
            case FROST -> new Look(ICE_STAGES, 0xDDF4FF, null, 0, 0.0f, 0.0f, GLITTER, 0xE8FAFF, 3, 0xFFFFFF,
                    null, false, true);
            // Water with something alive in it: a glow that will not sit still, sparks, arcs.
            case LIGHTNING -> new Look(WATER, 0x3F76E4, WATER, 0xFFF066, 0.4f, 0.35f, SPARK, 0xFFF8C0, 4,
                    0xFFFFD0, null, true, false);
            // Amethyst, glowing and glittering.
            case ARCANE -> new Look(AMETHYST, 0xC890FF, AMETHYST, 0xFF66FF, 0.35f, 0.2f, GLITTER, 0xFFCCFF, 4,
                    0xFFDDFF, null, false, false);
            case HEALING -> new Look(MOSS, 0x8FD07A, null, 0, 0.0f, 0.0f, GLITTER, 0xCCFFCC, 4, 0xEEFFEE,
                    null, false, false);
            case GENERIC, PHYSICAL_MELEE, PHYSICAL_RANGED -> new Look(WATER, school.color(), null, 0, 0.0f, 0.0f,
                    GLITTER, 0xFFFFFF, 5, 0xFFFFFF, null, false, false);
        };
    }

    /** One cell as it stands this frame, worked out once and drawn in every pass. */
    private record CellView(int x, int z, float y, float alpha, float a00, float a01, float a11, float a10,
                            int light, int hash, double sinceArrival, double untilExpiry) {
    }

    /** A cloud being drawn, and the ground it covers. */
    private record Field(BossCloud cloud, GroundPatch patch) {
    }

    /** Live clouds by entity id, added as they tick and dropped once gone. */
    private static final Map<Integer, Field> FIELDS = new HashMap<>();

    private GroundFx() {
    }

    public static void register() {
        WorldRenderHook.AFTER_TRANSLUCENT.register(GroundFx::render);
    }

    /** Keeps a cloud on the list to draw. From its client tick. */
    static void track(BossCloud cloud) {
        var field = FIELDS.get(cloud.getId());
        if (field == null || field.cloud() != cloud) {
            FIELDS.put(cloud.getId(), new Field(cloud, GroundPatch.flood(cloud.getWorld(), cloud.getPos(), cloud.radius())));
        }
    }

    /**
     * Draws every cloud's ground. Each layer goes down for every cloud before the next starts, so each kind
     * of drawing goes into its buffer in one run.
     */
    private static void render(WorldRenderHook.Context context) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (FIELDS.isEmpty()) {
            return;
        }
        FIELDS.values().removeIf(field -> world == null || field.cloud().isRemoved() || field.cloud().getWorld() != world);
        var consumers = context.consumers();
        var matrices = context.matrices();
        if (world == null || consumers == null || matrices == null || FIELDS.isEmpty()) {
            return;
        }
        Vec3d camera = context.camera().getPos();
        float tickDelta = context.tickDelta();

        List<Field> drawn = new ArrayList<>();
        List<Double> ages = new ArrayList<>();
        List<List<CellView>> views = new ArrayList<>();
        for (Field field : FIELDS.values()) {
            if (field.cloud().durationTicks() <= 0) {
                continue; // not synced yet
            }
            double age = world.getTime() - field.cloud().bornTick() + tickDelta;
            var cells = cells(world, field, age);
            if (!cells.isEmpty()) {
                drawn.add(field);
                ages.add(age);
                views.add(cells);
            }
        }
        if (drawn.isEmpty()) {
            return;
        }
        MatrixStack.Entry entry = matrices.peek();
        Matrix4f matrix = entry.getPositionMatrix();

        // The surfaces themselves, lit by the world.
        RenderLayer litLayer = RenderLayer.getEntityTranslucentCull(AtlasRef.BLOCK.texture());
        VertexConsumer lit = consumers.getBuffer(litLayer);
        for (int i = 0; i < drawn.size(); i++) {
            var look = look(drawn.get(i).cloud().school());
            for (CellView cell : views.get(i)) {
                Sprite sprite = look.cracks()
                        ? look.base().frame(crackStage(cell.untilExpiry()), 0, true)
                        : look.base().frame(0.0f, cell.hash(), false);
                if (sprite != null) {
                    flatLit(lit, entry, camera, cell, sprite, look.baseTint());
                }
            }
        }
        // Each layer is drawn as soon as it is down, while the world's projection is still in force.
        FxPainter.drawNow(consumers, litLayer);

        // Light from the ground itself: glowing cracks, a charged wash, flames.
        VertexConsumer addBlock = consumers.getBuffer(FxLayers.emissive(AtlasRef.BLOCK));
        for (int i = 0; i < drawn.size(); i++) {
            var look = look(drawn.get(i).cloud().school());
            double age = ages.get(i);
            for (CellView cell : views.get(i)) {
                if (look.glow() != null) {
                    Sprite sprite = look.glow().frame(0.0f, cell.hash(), false);
                    float beat = 0.7f + 0.3f * MathHelper.sin((float) (age * look.glowBeat() + (cell.hash() & 63) * 0.1f));
                    if (look.arcs()) {
                        // Charged ground flickers rather than breathes.
                        beat *= 0.75f + 0.25f * hashUnit(cell.hash() + (int) age * 7919);
                    }
                    if (sprite != null) {
                        flatAdd(addBlock, matrix, camera, cell, sprite, look.glowTint(), look.glowAlpha() * beat);
                    }
                }
                if (look.flames() != null && cell.hash() % 3 == 0) {
                    Sprite sprite = look.flames().frame(0.0f, cell.hash() >> 3, false);
                    if (sprite != null) {
                        flame(addBlock, matrix, camera, cell, sprite, age);
                    }
                }
            }
        }
        FxPainter.drawNow(consumers, FxLayers.emissive(AtlasRef.BLOCK));

        // Light caught on the surface: glints on ice, sparks on charged water.
        VertexConsumer addParticle = consumers.getBuffer(FxLayers.emissive(AtlasRef.PARTICLE));
        for (int i = 0; i < drawn.size(); i++) {
            var look = look(drawn.get(i).cloud().school());
            if (look.glint() == null) {
                continue;
            }
            double age = ages.get(i);
            for (CellView cell : views.get(i)) {
                if (cell.hash() % look.glintEvery() != 0) {
                    continue;
                }
                float phase = (cell.hash() >> 4 & 255) / 255.0f * MathHelper.TAU;
                float twinkle = (float) Math.pow(Math.max(0.0f, MathHelper.sin((float) (age * 0.12f) + phase)), 3.0);
                if (twinkle <= 0.01f) {
                    continue;
                }
                float life = (float) ((age * 0.04 + phase) % 1.0);
                Sprite sprite = look.glint().frame(life, 0, true);
                if (sprite != null) {
                    glint(addParticle, matrix, camera, cell, sprite, look.glintTint(), twinkle * cell.alpha() * 1.4f);
                }
            }
        }
        FxPainter.drawNow(consumers, FxLayers.emissive(AtlasRef.PARTICLE));

        // The bright front as the patch spreads, and arcs across charged ground.
        var painter = FxPainter.world(consumers, matrix, new Quaternionf(context.camera().getRotation()), camera);
        painter.begin(FxPass.GLOW);
        for (int i = 0; i < drawn.size(); i++) {
            var field = drawn.get(i);
            var look = look(field.cloud().school());
            for (CellView cell : views.get(i)) {
                if (cell.sinceArrival() < 6.0) {
                    float front = 1.0f - (float) (cell.sinceArrival() / 6.0);
                    painter.glow(new Vec3d(cell.x() + 0.5, cell.y() + OVERLAY_LIFT * 2.0, cell.z() + 0.5), FLAT,
                            0.85f, look.hot(), 0.55f * front);
                }
            }
            if (look.arcs()) {
                arcs(painter, world, field, views.get(i), look);
            }
        }
        painter.finish();
    }

    // ---- per cell ------------------------------------------------------------------------------------

    private static List<CellView> cells(ClientWorld world, Field field, double age) {
        var patch = field.patch();
        int duration = field.cloud().durationTicks();
        int salt = field.cloud().school().ordinal();
        List<CellView> out = new ArrayList<>(patch.cells().size());
        for (GroundPatch.Cell cell : patch.cells()) {
            double sinceArrival = age - cell.distance() * ARRIVAL_TICKS;
            float alpha = cellAlpha(sinceArrival, duration - age);
            if (alpha <= 0.01f) {
                continue;
            }
            int x = cell.x();
            int z = cell.z();
            int light = WorldRenderer.getLightmapCoordinates(world, BlockPos.ofFloored(x, cell.y() + 0.5, z));
            out.add(new CellView(x, z, cell.y(), alpha,
                    alpha * corner(patch, x, z, salt), alpha * corner(patch, x, z + 1, salt),
                    alpha * corner(patch, x + 1, z + 1, salt), alpha * corner(patch, x + 1, z, salt),
                    light, hash(x, z, salt), sinceArrival, duration - age));
        }
        return out;
    }

    /** A brighter edge as the overlay arrives, easing into the settled look, then fading at the end. */
    static float cellAlpha(double sinceArrival, double untilExpiry) {
        if (sinceArrival < 0.0 || untilExpiry <= 0.0) {
            return 0.0f;
        }
        double base = SETTLED;
        if (sinceArrival < SPREAD_TICKS) {
            double progress = sinceArrival / SPREAD_TICKS;
            double eased = 1.0 - (1.0 - progress) * (1.0 - progress);
            base = SPREAD + (SETTLED - SPREAD) * eased;
            base *= Math.min(1.0, progress * 3.0);
        }
        if (untilExpiry < FADE_TICKS) {
            base *= untilExpiry / FADE_TICKS;
        }
        return (float) base;
    }

    /**
     * How strongly a corner shows: a fixed per-corner shade, so neighbouring cells share it and the seams
     * vanish, thinned by how many of the four cells around it the patch covers - so the shore fades out.
     */
    private static float corner(GroundPatch patch, int x, int z, int salt) {
        int covered = 0;
        if (patch.covers(x - 1, z - 1)) covered++;
        if (patch.covers(x, z - 1)) covered++;
        if (patch.covers(x - 1, z)) covered++;
        if (patch.covers(x, z)) covered++;
        float share = covered / 4.0f;
        float shade = 0.72f + 0.28f * ((hash(x, z, salt + 31) & 0xFF) / 255.0f);
        return shade * share * share;
    }

    /** Which frosted-ice frame shows: whole while it has a while left, cracking through its last three seconds. */
    static float crackStage(double untilExpiry) {
        if (untilExpiry > 60.0) {
            return 0.0f;
        }
        float cracked = 1.0f - (float) Math.max(0.0, untilExpiry) / 60.0f;
        return MathHelper.clamp(0.25f + cracked * 0.75f, 0.0f, 0.999f);
    }

    // ---- geometry ------------------------------------------------------------------------------------

    /** A cell's surface, lit by the world, both ways round for the culling layer. */
    private static void flatLit(VertexConsumer out, MatrixStack.Entry entry, Vec3d camera, CellView cell,
                                Sprite sprite, int rgb) {
        float x0 = (float) (cell.x() - camera.x);
        float x1 = x0 + 1.0f;
        float z0 = (float) (cell.z() - camera.z);
        float z1 = z0 + 1.0f;
        float y = (float) (cell.y() - camera.y);
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        litVertex(out, entry, x0, y, z0, u0, v0, rgb, cell.a00(), cell.light());
        litVertex(out, entry, x0, y, z1, u0, v1, rgb, cell.a01(), cell.light());
        litVertex(out, entry, x1, y, z1, u1, v1, rgb, cell.a11(), cell.light());
        litVertex(out, entry, x1, y, z0, u1, v0, rgb, cell.a10(), cell.light());

        litVertex(out, entry, x1, y, z0, u1, v0, rgb, cell.a10(), cell.light());
        litVertex(out, entry, x1, y, z1, u1, v1, rgb, cell.a11(), cell.light());
        litVertex(out, entry, x0, y, z1, u0, v1, rgb, cell.a01(), cell.light());
        litVertex(out, entry, x0, y, z0, u0, v0, rgb, cell.a00(), cell.light());
    }

    private static void litVertex(VertexConsumer out, MatrixStack.Entry entry, float x, float y, float z,
                                  float u, float v, int rgb, float alpha, int light) {
        out.vertex(entry, x, y, z)
                .color(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, (int) (MathHelper.clamp(alpha, 0.0f, 1.0f) * 255.0f))
                .texture(u, v)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 1.0f, 0.0f);
    }

    /** A cell's surface again, as light, just above itself. The layer does not cull. */
    private static void flatAdd(VertexConsumer out, Matrix4f matrix, Vec3d camera, CellView cell, Sprite sprite,
                                int rgb, float strength) {
        float x0 = (float) (cell.x() - camera.x);
        float x1 = x0 + 1.0f;
        float z0 = (float) (cell.z() - camera.z);
        float z1 = z0 + 1.0f;
        float y = (float) (cell.y() + OVERLAY_LIFT - camera.y);
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        addVertex(out, matrix, x0, y, z0, u0, v0, rgb, cell.a00() * strength);
        addVertex(out, matrix, x0, y, z1, u0, v1, rgb, cell.a01() * strength);
        addVertex(out, matrix, x1, y, z1, u1, v1, rgb, cell.a11() * strength);
        addVertex(out, matrix, x1, y, z0, u1, v0, rgb, cell.a10() * strength);
    }

    /** Two crossed panes of fire standing up out of a cell, flickering in height. */
    private static void flame(VertexConsumer out, Matrix4f matrix, Vec3d camera, CellView cell, Sprite sprite,
                              double age) {
        float jitterX = (hashUnit(cell.hash() * 31) - 0.5f) * 0.4f;
        float jitterZ = (hashUnit(cell.hash() * 17) - 0.5f) * 0.4f;
        float cx = (float) (cell.x() + 0.5 + jitterX - camera.x);
        float cz = (float) (cell.z() + 0.5 + jitterZ - camera.z);
        float y0 = (float) (cell.y() - camera.y);
        float flicker = 1.0f + 0.15f * MathHelper.sin((float) (age * 0.55) + (cell.hash() & 31));
        float height = (0.45f + 0.3f * hashUnit(cell.hash() * 13)) * flicker * Math.min(1.0f, cell.alpha() * 1.6f);
        float y1 = y0 + height;
        float half = 0.3f;
        float alpha = 0.85f * Math.min(1.0f, cell.alpha() * 1.4f);
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        for (int pane = 0; pane < 2; pane++) {
            float angle = (pane == 0 ? 0.785f : 2.356f) + jitterX;
            float dx = MathHelper.cos(angle) * half;
            float dz = MathHelper.sin(angle) * half;
            addVertex(out, matrix, cx - dx, y0, cz - dz, u0, v1, 0xFFFFFF, alpha);
            addVertex(out, matrix, cx - dx, y1, cz - dz, u0, v0, 0xFFFFFF, alpha * 0.6f);
            addVertex(out, matrix, cx + dx, y1, cz + dz, u1, v0, 0xFFFFFF, alpha * 0.6f);
            addVertex(out, matrix, cx + dx, y0, cz + dz, u1, v1, 0xFFFFFF, alpha);
        }
    }

    /** A small glint lying on the surface somewhere in a cell, turned at its own angle. */
    private static void glint(VertexConsumer out, Matrix4f matrix, Vec3d camera, CellView cell, Sprite sprite,
                              int rgb, float alpha) {
        float px = (float) (cell.x() + 0.2 + 0.6 * hashUnit(cell.hash() * 7) - camera.x);
        float pz = (float) (cell.z() + 0.2 + 0.6 * hashUnit(cell.hash() * 11) - camera.z);
        float y = (float) (cell.y() + OVERLAY_LIFT * 3.0 - camera.y);
        float half = 0.14f + 0.08f * hashUnit(cell.hash() * 5);
        float angle = hashUnit(cell.hash() * 3) * MathHelper.TAU;
        float c = MathHelper.cos(angle) * half;
        float s = MathHelper.sin(angle) * half;
        float u0 = sprite.getMinU();
        float u1 = sprite.getMaxU();
        float v0 = sprite.getMinV();
        float v1 = sprite.getMaxV();
        addVertex(out, matrix, px - c + s, y, pz - s - c, u0, v0, rgb, alpha);
        addVertex(out, matrix, px - c - s, y, pz - s + c, u0, v1, rgb, alpha);
        addVertex(out, matrix, px + c - s, y, pz + s + c, u1, v1, rgb, alpha);
        addVertex(out, matrix, px + c + s, y, pz + s - c, u1, v0, rgb, alpha);
    }

    private static void addVertex(VertexConsumer out, Matrix4f matrix, float x, float y, float z, float u, float v,
                                  int rgb, float alpha) {
        out.vertex(matrix, x, y, z).texture(u, v)
                .color((rgb >> 16 & 0xFF) / 255.0f, (rgb >> 8 & 0xFF) / 255.0f, (rgb & 0xFF) / 255.0f,
                        MathHelper.clamp(alpha, 0.0f, 1.0f))
                .light(FxLight.FULL_BRIGHT);
    }

    /** Arcs crawling across charged ground: a few cracks of light between nearby cells, chosen afresh every tick. */
    private static void arcs(FxPainter painter, ClientWorld world, Field field, List<CellView> cells, Look look) {
        if (cells.size() < 2) {
            return;
        }
        var random = new Random(field.cloud().bornTick() * 31L + world.getTime() * 7919L + field.cloud().getId());
        int count = Math.min(4, 1 + cells.size() / 8);
        for (int i = 0; i < count; i++) {
            CellView a = cells.get(random.nextInt(cells.size()));
            CellView b = cells.get(random.nextInt(cells.size()));
            int dx = b.x() - a.x();
            int dz = b.z() - a.z();
            if (a == b || dx * dx + dz * dz > 16) {
                continue;
            }
            float strength = Math.min(a.alpha(), b.alpha()) / SETTLED;
            Vec3d from = new Vec3d(a.x() + random.nextDouble(), a.y() + 0.05, a.z() + random.nextDouble());
            Vec3d to = new Vec3d(b.x() + random.nextDouble(), b.y() + 0.05, b.z() + random.nextDouble());
            Vec3d previous = from;
            int steps = 4 + random.nextInt(3);
            for (int s = 1; s <= steps; s++) {
                Vec3d next = s == steps ? to : from.lerp(to, s / (double) steps)
                        .add((random.nextDouble() - 0.5) * 0.3, random.nextDouble() * 0.12, (random.nextDouble() - 0.5) * 0.3);
                painter.beam(previous, next, 0.12f, look.glowTint(), 0.8f * strength);
                painter.beam(previous, next, 0.04f, look.hot(), strength);
                previous = next;
            }
            painter.glow(to, 0.2f, look.hot(), 0.7f * strength);
        }
    }

    // ---- hashing ---------------------------------------------------------------------------------------

    private static int hash(int x, int z, int salt) {
        int mixed = x * 374761393 + z * 668265263 + salt * 1274126177;
        mixed = (mixed ^ (mixed >>> 13)) * 1103515245;
        return (mixed ^ (mixed >>> 16)) & Integer.MAX_VALUE;
    }

    private static float hashUnit(int value) {
        int mixed = value * 0x9E3779B9;
        mixed ^= mixed >>> 15;
        return (mixed & 0xFFFF) / 65535.0f;
    }
}

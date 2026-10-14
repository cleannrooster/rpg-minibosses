package com.cleannrooster.anarchy_rpg.client.faction;

import com.cleannrooster.anarchy_rpg.client.WorldRenderHook;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.faction.PackedFlag;
import com.cleannrooster.anarchy_rpg.hub.HubMarkersPayload;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BannerBlockEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModelLayers;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.model.ModelLoader;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;

/**
 * Draws faction flags floating above adventurers and above hubs, but only up close: within
 * {@link #ADVENTURER_RANGE} blocks of an adventurer and {@link #HUB_RANGE} of a hub.
 *
 * <p>The flag is vanilla's own banner flag (the {@code banner} model layer and its pattern textures, via
 * {@link BannerBlockEntityRenderer#renderCanvas}), turned to face the camera, so it matches placed banners
 * and follows resource packs.
 */
public final class FactionFlagRenderer {
    private static final double ADVENTURER_RANGE = 16.0;
    private static final double HUB_RANGE = 48.0;
    /** Model units are pixels; the flag is 20 x 40 of them. These scale it to world size. */
    private static final float ADVENTURER_SCALE = 0.25F;
    private static final float HUB_SCALE = 0.9F;
    private static final float FLAG_HEIGHT_PX = 40.0F;
    private static final float PX = 1.0F / 16.0F;
    /** Gap between an adventurer's head (and its name tag) and the bottom of its flag. */
    private static final double ADVENTURER_GAP = 0.85;

    private static volatile List<HubMarkersPayload.Marker> hubs = List.of();
    private static ModelPart flag;
    private static EntityModelLoader flagLoader;

    private FactionFlagRenderer() {
    }

    /** During mod construction, with the other payload receivers. */
    public static void registerReceiver() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, HubMarkersPayload.ID, HubMarkersPayload.CODEC,
                (payload, context) -> context.queue(() -> hubs = List.copyOf(payload.markers())));
    }

    public static void register() {
        WorldRenderHook.register(FactionFlagRenderer::render);
    }

    private static void render(MatrixStack matrices, Camera camera, VertexConsumerProvider consumers, float partialTick) {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld world = client.world;
        if (world == null || client.options.hudHidden) {
            return;
        }
        ModelPart canvas = flagModel(client);
        if (canvas == null) {
            return;
        }
        Vec3d cameraPos = camera.getPos();

        double adventurerRangeSq = ADVENTURER_RANGE * ADVENTURER_RANGE;
        for (Entity entity : world.getEntities()) {
            if (!(entity instanceof MinibossEntity npc) || !npc.isAlive() || npc.isInvisible() || npc.isDowned()) {
                continue;
            }
            int packed = npc.getPackedFactionFlag();
            if (packed == PackedFlag.NONE || npc.squaredDistanceTo(cameraPos) > adventurerRangeSq) {
                continue;
            }
            double x = MathHelper.lerp(partialTick, npc.prevX, npc.getX());
            double y = MathHelper.lerp(partialTick, npc.prevY, npc.getY()) + npc.getHeight() + ADVENTURER_GAP;
            double z = MathHelper.lerp(partialTick, npc.prevZ, npc.getZ());
            drawFlag(matrices, consumers, camera, canvas, new Vec3d(x, y, z), ADVENTURER_SCALE, packed, world);
        }

        double hubRangeSq = HUB_RANGE * HUB_RANGE;
        for (HubMarkersPayload.Marker marker : hubs) {
            Vec3d base = Vec3d.ofBottomCenter(marker.pos());
            if (marker.flag() != PackedFlag.NONE && base.squaredDistanceTo(cameraPos) <= hubRangeSq) {
                drawFlag(matrices, consumers, camera, canvas, base, HUB_SCALE, marker.flag(), world);
            }
        }

        if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
            immediate.draw();
        }
    }

    /** One camera-facing flag whose bottom edge sits at {@code bottom}. */
    private static void drawFlag(MatrixStack matrices, VertexConsumerProvider consumers, Camera camera, ModelPart canvas,
                                 Vec3d bottom, float scale, int packed, ClientWorld world) {
        Vec3d cameraPos = camera.getPos();
        matrices.push();
        matrices.translate(bottom.x - cameraPos.x, bottom.y - cameraPos.y + FLAG_HEIGHT_PX * PX * scale, bottom.z - cameraPos.z);
        matrices.multiply(camera.getRotation());
        // Model y points down the flag, so flip y; the flag then hangs from the translated point.
        matrices.scale(scale, -scale, -scale);
        BannerBlockEntityRenderer.renderCanvas(matrices, consumers, LightmapTextureManager.MAX_LIGHT_COORDINATE,
                OverlayTexture.DEFAULT_UV, canvas, ModelLoader.BANNER_BASE, true, PackedFlag.base(packed),
                PackedFlag.patterns(packed, world.getRegistryManager()));
        matrices.pop();
    }

    /** The banner flag part, rebuilt if resources were reloaded. Centered on x, hanging from y = 0. */
    private static ModelPart flagModel(MinecraftClient client) {
        EntityModelLoader loader = client.getEntityModelLoader();
        if (flag == null || flagLoader != loader) {
            flag = loader.getModelPart(EntityModelLayers.BANNER).getChild("flag");
            flagLoader = loader;
        }
        flag.pivotX = 0.0F;
        flag.pivotY = 0.0F;
        flag.pivotZ = 0.0F;
        flag.pitch = 0.0F;
        return flag;
    }

    /** Forget hub markers on leaving a world. */
    public static void clear() {
        hubs = List.of();
    }
}

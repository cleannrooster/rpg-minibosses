package com.cleannrooster.rpg_minibosses.mixin;

import com.cleannrooster.rpg_minibosses.entity.GeminiEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.crash.CrashException;
import net.minecraft.util.crash.CrashReport;
import net.minecraft.util.crash.CrashReportSection;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.spell_power.api.SpellSchools;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin<E extends Entity>{
    Entity entity2;
    @Inject(at = @At("HEAD"), method = "render", cancellable = true)
    public void renderMixGemini(E entity, double d, double e, double f, float g, float h, MatrixStack poseStack, VertexConsumerProvider multiBufferSource, int i, CallbackInfo info) {
        EntityRenderDispatcher renderer = (EntityRenderDispatcher) (Object) this;
        EntityRenderer entityRenderer = renderer.getRenderer(entity);
        if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().player != null && MinecraftClient.getInstance().options.getPerspective().equals(Perspective.FIRST_PERSON) && MinecraftClient.getInstance().player.equals(entity)) {
            return;
        }
        if(MinecraftClient.getInstance() != null && !(MinecraftClient.getInstance().player != null && (MinecraftClient.getInstance().currentScreen instanceof CreativeInventoryScreen || MinecraftClient.getInstance().currentScreen instanceof InventoryScreen) && entity == MinecraftClient.getInstance().player) ) {
            try {
                double d1 = MathHelper.lerp((double)h, entity.lastRenderY, entity.getY());

                Vec3d vec3 = entityRenderer.getPositionOffset(entity, h);
                double j = d + vec3.getX();
                double k = - (e)+(-62.5+d1)*2-3F/8F;
                double l = f + vec3.getZ();
                double y =  (e)+vec3.getY();
                if(entity instanceof GeminiEntity geminiEntity && geminiEntity.getDataTracker().get(GeminiEntity.IS_CLONES)){

                    Vec3d distance = geminiEntity.getLerpedPos(h).subtract(MinecraftClient.getInstance().player.getLerpedPos(h));
                    Vec3d distanceleft = distance.rotateY((float) Math.toRadians(90));
                    Vec3d distanceright = distance.rotateY((float) Math.toRadians(-90));

                    poseStack.push();

                    poseStack.translate(j-2*distance.getX(),y,l-2*distance.getZ());
                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(180));

                    entityRenderer.render(entity, g, h, poseStack, multiBufferSource, i);
                    poseStack.pop();
                    poseStack.push();
                    poseStack.translate(j-distanceleft.getX()-distance.getX(),y,l-distanceleft.getZ()-distance.getZ());
                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(-90));

                    entityRenderer.render(entity, g, h, poseStack, multiBufferSource, i);
                    poseStack.pop();
                    poseStack.push();
                    poseStack.translate(j-distanceright.getX()-distance.getX(),y,l-distanceright.getZ()-distance.getZ());
                    poseStack.multiply(RotationAxis.POSITIVE_Y.rotation(90));

                    entityRenderer.render(entity, g, h, poseStack, multiBufferSource, i);
                    poseStack.pop();

                }
            } catch (Throwable var24) {
                CrashReport crashReport = CrashReport.create(var24, "Rendering entity in world");
                CrashReportSection crashReportCategory = crashReport.addElement("Entity being rendered");
                entity.populateCrashReport(crashReportCategory);
                CrashReportSection crashReportCategory2 = crashReport.addElement("Renderer details");
                crashReportCategory2.add("Assigned renderer", entityRenderer);
                crashReportCategory2.add("Location", CrashReportSection.createPositionString(entity.getWorld(), d, e, f));
                crashReportCategory2.add("Rotation", g);
                crashReportCategory2.add("Delta", h);
                throw new CrashException(crashReport);
            }
        }
    }

}
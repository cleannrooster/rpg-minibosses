package com.cleannrooster.anarchy_rpg.client.combat;

import com.cleannrooster.anarchy_rpg.client.WorldRenderHook;
import net.minecraft.util.math.MathHelper;
import dev.eann.atlasvfx.trail.TrailWindow;
import dev.eann.atlasvfx.trail.TrailStyles;
import dev.eann.atlasvfx.trail.Trail;
import dev.eann.atlasvfx.FxWorld;
import com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackVolume;
import com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackShape;
import com.cleannrooster.anarchy_rpg.client.fx.CueHandlers;
import com.cleannrooster.anarchy_rpg.entity.combat.AttackRegistry;
import com.cleannrooster.anarchy_rpg.entity.combat.net.SwingPayload;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.networking.NetworkManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumerProvider;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client-side pool of live attack visuals.
 *
 * <p>Nothing here knows about any particular mob: an entity that fires an attack with a volume and a
 * ribbon profile gets its visual for free. The pool ages effects out on the client tick and draws them
 * after entities, so the arc sits in the world rather than over the UI.
 */
public final class SlashEffectManager {

    private static final List<SlashEffect> ACTIVE = new ArrayList<>();

    private SlashEffectManager() {
    }

    /**
     * Declare the swing payload and its receiver. Must run before the loader closes payload registration,
     * so it is called from each loader's client entrypoint during mod construction, not from client setup.
     */
    public static void registerReceiver() {
        NetworkManager.registerReceiver(NetworkManager.Side.S2C, SwingPayload.ID, SwingPayload.CODEC,
                (payload, context) -> context.queue(() -> spawn(payload)));
    }

    /** Wire up the tick hook and the world-render hook. Called from client init. */
    public static void init() {
        ClientTickEvent.CLIENT_POST.register(client -> tick());
        WorldRenderHook.register((matrices, camera, consumers, partialTick) -> {
            if (ACTIVE.isEmpty() || consumers == null) {
                return;
            }
            for (var effect : ACTIVE) {
                SlashRenderer.render(effect, matrices, camera, consumers, partialTick);
            }
            if (consumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw();
            }
        });
    }

    private static void spawn(SwingPayload payload) {
        var action = AttackRegistry.lookupForClient(payload.attackId());
        if (action == null || action.volume() == null) {
            return;
        }
        var ribbon = action.ribbon();
        if (drawsAsTrail(action.volume()) && ribbon != null) {
            // A sweep is drawn by AtlasVFX on the attack's own damage surface; its colour, length and fade
            // come from the ribbon profile it replaces.
            int colour = rgb(ribbon.red(), ribbon.green(), ribbon.blue());
            var trail = new Trail(new AttackSurface(action.volume(), payload.frame()), TrailStyles.slash(colour),
                    TrailWindow.swing(action.activeTicks(), ribbon.trail(), ribbon.fadeOutTicks()),
                    payload.attackerId() * 31L + payload.attackId().hashCode());
            FxWorld.get().play(trail, CueHandlers.DENSITY);
            return;
        }
        ACTIVE.add(new SlashEffect(action, payload.frame(), payload.attackerId()));
    }

    /**
     * Swept arcs are drawn as AtlasVFX trails; thrust lanes, charge paths and one-tick shapes keep the
     * ribbon until AtlasVFX can draw them.
     */
    public static boolean drawsAsTrail(AttackVolume volume) {
        return volume.shape().isSwept() && volume.shape().family() == AttackShape.Family.ARC;
    }

    private static int rgb(float red, float green, float blue) {
        return (Math.round(MathHelper.clamp(red, 0.0f, 1.0f) * 255.0f) << 16)
                | (Math.round(MathHelper.clamp(green, 0.0f, 1.0f) * 255.0f) << 8)
                | Math.round(MathHelper.clamp(blue, 0.0f, 1.0f) * 255.0f);
    }

    private static void tick() {
        if (ACTIVE.isEmpty()) {
            return;
        }
        var level = MinecraftClient.getInstance().world;
        if (level == null) {
            ACTIVE.clear();
            return;
        }
        for (Iterator<SlashEffect> it = ACTIVE.iterator(); it.hasNext(); ) {
            var effect = it.next();
            var volume = effect.action().volume();
            // A travelling attack's volume follows its owner, so the visual has to follow it too.
            if (volume != null && volume.shape().followsAttacker()) {
                var attacker = level.getEntityById(effect.attackerId());
                if (attacker != null) {
                    effect.setFrame(effect.frame().withOrigin(
                            attacker.getPos().add(0.0, volume.offsetVertical(), 0.0)));
                }
            }
            effect.tick();
            if (effect.isExpired()) {
                it.remove();
            }
        }
    }

    /** Drop everything — on level change or disconnect. */
    public static void clear() {
        ACTIVE.clear();
    }
}

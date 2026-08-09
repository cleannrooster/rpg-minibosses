package com.cleannrooster.rpg_minibosses.client.combat;

import com.cleannrooster.rpg_minibosses.entity.combat.AttackRegistry;
import com.cleannrooster.rpg_minibosses.entity.combat.net.SwingPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
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

    /** Wire up the packet receiver, the tick hook and the world-render hook. Called from client init. */
    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(SwingPayload.ID, (payload, context) ->
                context.client().execute(() -> spawn(payload)));
        ClientTickEvents.END_CLIENT_TICK.register(client -> tick());
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            if (ACTIVE.isEmpty() || context.consumers() == null) {
                return;
            }
            var partialTick = context.tickCounter().getTickDelta(false);
            for (var effect : ACTIVE) {
                SlashRenderer.render(effect, context.matrixStack(), context.camera(),
                        context.consumers(), partialTick);
            }
            if (context.consumers() instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw();
            }
        });
    }

    private static void spawn(SwingPayload payload) {
        var action = AttackRegistry.lookupForClient(payload.attackId());
        if (action == null || action.volume() == null) {
            return;
        }
        ACTIVE.add(new SlashEffect(action, payload.frame(), payload.attackerId()));
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

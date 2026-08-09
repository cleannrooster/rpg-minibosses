package com.cleannrooster.rpg_minibosses.entity.combat.net;

import com.cleannrooster.rpg_minibosses.entity.combat.CombatAction;
import com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackFrame;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

/**
 * Server-to-client dispatch for attack visuals.
 *
 * <p>One packet per swing, sent on the tick the damaging window opens, so the ribbon and the hitbox start
 * together. Uses the Fabric networking API, which Forgified Fabric API mirrors on NeoForge — the same
 * arrangement the rest of this mod's networking already relies on.
 */
public final class AttackNetwork {

    /**
     * Players further than this from the attacker never see the swing, so a long fight does not spam the
     * whole dimension. Comfortably beyond any attack's reach plus normal render distance.
     */
    private static final double BROADCAST_RANGE = 96.0;

    private AttackNetwork() {
    }

    /** Declare the payload type. Called from common init on both sides. */
    public static void register() {
        PayloadTypeRegistry.playS2C().register(SwingPayload.ID, SwingPayload.CODEC);
    }

    /** Tell every nearby client to draw this swing. */
    public static void broadcastSwing(LivingEntity attacker, CombatAction action, AttackFrame frame) {
        if (!(attacker.getWorld() instanceof ServerWorld world)) {
            return;
        }
        var payload = SwingPayload.of(action.id(), attacker.getId(), frame);
        var rangeSq = BROADCAST_RANGE * BROADCAST_RANGE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.squaredDistanceTo(attacker) <= rangeSq
                    && ServerPlayNetworking.canSend(player, SwingPayload.ID)) {
                ServerPlayNetworking.send(player, payload);
            }
        }
    }
}

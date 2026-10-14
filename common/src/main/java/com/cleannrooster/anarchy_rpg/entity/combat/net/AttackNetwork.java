package com.cleannrooster.anarchy_rpg.entity.combat.net;


import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;import com.cleannrooster.anarchy_rpg.entity.combat.CombatAction;
import com.cleannrooster.anarchy_rpg.entity.combat.geometry.AttackFrame;
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

    /**
     * Declare the payload type on a dedicated server. Called from common init on both sides; a client
     * declares it instead by registering its receiver ({@code SlashEffectManager.registerReceiver}), and
     * declaring it twice is an error.
     */
    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            NetworkManager.registerS2CPayloadType(SwingPayload.ID, SwingPayload.CODEC);
        }
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
                    && NetworkManager.canPlayerReceive(player, SwingPayload.ID)) {
                NetworkManager.sendToPlayer(player, payload);
            }
        }
    }
}

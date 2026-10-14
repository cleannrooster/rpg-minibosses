package com.cleannrooster.anarchy_rpg.conversation;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.net.ConversationActionPayload;
import com.cleannrooster.anarchy_rpg.conversation.net.ConversationClosedPayload;
import com.cleannrooster.anarchy_rpg.conversation.net.OpenConversationPayload;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.faction.Disposition;
import com.cleannrooster.anarchy_rpg.faction.Faction;
import com.cleannrooster.anarchy_rpg.faction.FactionState;
import com.cleannrooster.anarchy_rpg.faction.Factions;
import dev.architectury.event.events.common.PlayerEvent;
import dev.architectury.networking.NetworkManager;
import dev.architectury.platform.Platform;
import dev.architectury.utils.Env;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

/**
 * Server side of talking to adventurers. A conversation pairs one player with one adventurer: the
 * adventurer stops wandering and faces the player, and nobody else can talk to it meanwhile. A player is
 * in at most one conversation.
 *
 * <p>The server decides everything. It builds the option list, re-checks a picked option against the
 * current state before running it, and ends the conversation when it stops making sense: the player walks
 * off or leaves, the adventurer is downed, petrified or fighting, the player hits it, or it has turned
 * hostile.
 */
public final class Conversations {
    /** Beyond this the conversation ends. */
    private static final double MAX_DISTANCE = 8.0;
    /** How many greeting lines each disposition has in the lang file. */
    private static final int GREETING_VARIANTS = 3;

    /** Player UUID to the entity id of the adventurer they are talking to. */
    private static final Map<UUID, Integer> ACTIVE = new HashMap<>();

    private Conversations() {
    }

    public static void register() {
        if (Platform.getEnvironment() == Env.SERVER) {
            // A client declares these by registering its receivers; declaring them twice is an error.
            NetworkManager.registerS2CPayloadType(OpenConversationPayload.ID, OpenConversationPayload.CODEC);
            NetworkManager.registerS2CPayloadType(ConversationClosedPayload.ID, ConversationClosedPayload.CODEC);
        }
        NetworkManager.registerReceiver(NetworkManager.Side.C2S, ConversationActionPayload.ID, ConversationActionPayload.CODEC,
                (payload, context) -> context.queue(() -> {
                    if (context.getPlayer() instanceof ServerPlayerEntity player) {
                        onAction(player, payload);
                    }
                }));
        PlayerEvent.PLAYER_QUIT.register(player -> ACTIVE.remove(player.getUuid()));
        CompanionOrderOptions.register();
    }

    /**
     * Try to start a conversation. Returns false when the adventurer will not talk: it is hostile to this
     * player, fighting, downed, still petrified, or already talking to someone else. The player is told
     * why when it is something other than hostility.
     */
    public static boolean tryOpen(MinibossEntity npc, ServerPlayerEntity player) {
        if (!(npc.getWorld() instanceof ServerWorld world) || !npc.notPetrified() || npc.isDowned()) {
            return false;
        }
        Disposition disposition = Factions.disposition(npc, player);
        if (disposition == Disposition.HOSTILE) {
            return false;
        }
        UUID partner = npc.getConversationPartner();
        if (npc.getTarget() != null || (partner != null && !partner.equals(player.getUuid()))) {
            player.sendMessage(Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".busy", npc.getName()), true);
            return false;
        }
        Integer previous = ACTIVE.get(player.getUuid());
        if (previous != null && previous != npc.getId() && world.getEntityById(previous) instanceof MinibossEntity other) {
            end(other, player, true);
        }

        Faction faction = FactionState.roster(world.getServer()).get(npc.getPartyMembership().faction());
        List<OpenConversationPayload.Entry> options = new ArrayList<>();
        for (Entry<ConversationOption, ConversationOptions.Registration> entry : ConversationOptions.all().entrySet()) {
            OptionAvailability availability = entry.getValue().rule().check(npc, player, disposition);
            if (availability.shown()) {
                options.add(new OpenConversationPayload.Entry(entry.getKey(), availability.enabled(), availability.reason()));
            }
        }

        npc.setConversationPartner(player.getUuid());
        npc.getNavigation().stop();
        ACTIVE.put(player.getUuid(), npc.getId());
        NetworkManager.sendToPlayer(player, new OpenConversationPayload(npc.getId(), npc.getName(), greeting(npc, disposition),
                faction.name(), faction.flag().base(), disposition, null, options));
        return true;
    }

    private static Text greeting(MinibossEntity npc, Disposition disposition) {
        int variant = 1 + npc.getRandom().nextInt(GREETING_VARIANTS);
        return Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".greeting."
                + disposition.name().toLowerCase(Locale.ROOT) + "." + variant);
    }

    private static void onAction(ServerPlayerEntity player, ConversationActionPayload payload) {
        Integer active = ACTIVE.get(player.getUuid());
        if (active == null || active != payload.npcId()
                || !(player.getServerWorld().getEntityById(payload.npcId()) instanceof MinibossEntity npc)) {
            ACTIVE.remove(player.getUuid());
            return;
        }
        if (payload.option() == null) {
            end(npc, player, false);
            return;
        }
        // The client may be stale or lying: re-check the option against the state right now.
        String invalid = whyInvalid(npc, player);
        ConversationOptions.Registration registration = ConversationOptions.get(payload.option());
        if (invalid != null || registration == null) {
            end(npc, player, true);
            return;
        }
        OptionAvailability availability = registration.rule().check(npc, player, Factions.disposition(npc, player));
        if (!availability.shown() || !availability.enabled()) {
            end(npc, player, true);
            return;
        }
        boolean keepOpen = registration.handler().choose(npc, player);
        if (!keepOpen) {
            end(npc, player, true);
        }
    }

    /**
     * Called from the adventurer's server tick while it has a partner. Keeps it facing the player and
     * still, and ends the conversation once it no longer holds.
     */
    public static void tick(MinibossEntity npc) {
        UUID partner = npc.getConversationPartner();
        if (partner == null || !(npc.getWorld() instanceof ServerWorld world)) {
            return;
        }
        ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(partner);
        Integer active = player == null ? null : ACTIVE.get(partner);
        if (player == null || active == null || active != npc.getId()) {
            npc.setConversationPartner(null);
            return;
        }
        if (whyInvalid(npc, player) != null) {
            end(npc, player, true);
            return;
        }
        npc.getNavigation().stop();
        npc.getLookControl().lookAt(player, 30.0F, 30.0F);
    }

    /** The adventurer was hurt; a conversation with whoever hurt it is over. */
    public static void onDamagedBy(MinibossEntity npc, @Nullable UUID attacker) {
        UUID partner = npc.getConversationPartner();
        if (partner != null && partner.equals(attacker) && npc.getWorld() instanceof ServerWorld world) {
            ServerPlayerEntity player = world.getServer().getPlayerManager().getPlayer(partner);
            if (player != null) {
                end(npc, player, true);
            } else {
                npc.setConversationPartner(null);
            }
        }
    }

    private static @Nullable String whyInvalid(MinibossEntity npc, ServerPlayerEntity player) {
        if (!npc.isAlive() || npc.isRemoved() || npc.isDowned() || !npc.notPetrified()) return "npc unavailable";
        if (!player.isAlive() || player.isSpectator() || player.getWorld() != npc.getWorld()) return "player unavailable";
        if (player.squaredDistanceTo(npc) > MAX_DISTANCE * MAX_DISTANCE) return "too far";
        if (npc.getTarget() != null) return "fighting";
        if (Factions.disposition(npc, player) == Disposition.HOSTILE) return "hostile";
        return null;
    }

    /** Ends a conversation on the server, and tells the client if it did not ask for it. */
    public static void end(MinibossEntity npc, ServerPlayerEntity player, boolean notifyClient) {
        if (player.getUuid().equals(npc.getConversationPartner())) {
            npc.setConversationPartner(null);
        }
        Integer active = ACTIVE.get(player.getUuid());
        if (active != null && active == npc.getId()) {
            ACTIVE.remove(player.getUuid());
        }
        if (notifyClient) {
            NetworkManager.sendToPlayer(player, new ConversationClosedPayload(npc.getId()));
        }
    }
}

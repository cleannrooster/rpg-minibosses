package com.cleannrooster.anarchy_rpg.conversation;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Follow and Stay for the player's own companion. These carry over the toggle that right-clicking an owned
 * adventurer used to perform, with the same replies. Stay is still untimed: durations arrive with the
 * companion state machine (M4), which replaces this class.
 */
final class CompanionOrderOptions {

    private CompanionOrderOptions() {
    }

    static void register() {
        ConversationOptions.register(ConversationOption.FOLLOW,
                (npc, player, disposition) -> isOwner(npc, player) && npc.isSitting()
                        ? OptionAvailability.ENABLED : OptionAvailability.HIDDEN,
                (npc, player) -> order(npc, player, false, "I will follow."));
        ConversationOptions.register(ConversationOption.STAY,
                (npc, player, disposition) -> isOwner(npc, player) && !npc.isSitting()
                        ? OptionAvailability.ENABLED : OptionAvailability.HIDDEN,
                (npc, player) -> order(npc, player, true, "I will stay here."));
    }

    private static boolean isOwner(MinibossEntity npc, ServerPlayerEntity player) {
        return player.getUuid().equals(npc.getOwnerUuid());
    }

    private static boolean order(MinibossEntity npc, ServerPlayerEntity player, boolean sit, String reply) {
        npc.setSitting(sit);
        player.sendMessage(npc.getName().copy().append(Text.translatable(": ")).append(Text.of(reply)));
        return false;
    }
}

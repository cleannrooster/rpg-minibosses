package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOptions;
import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/** The orders a player can give their own companion in conversation. Nobody else sees them. */
final class CompanionOptions {

    private CompanionOptions() {
    }

    static void register() {
        ConversationOptions.register(ConversationOption.FOLLOW,
                (npc, player, d) -> shownFor(npc, player, mode -> mode != CompanionMode.FOLLOWING),
                (npc, player, argument) -> act(npc, player, record -> Companions.follow(player, npc, record)));
        ConversationOptions.register(ConversationOption.STAY,
                (npc, player, d) -> shownFor(npc, player, mode -> true),
                (npc, player, argument) -> act(npc, player, record -> {
                    int hours = Math.max(ConversationOption.WAIT_MIN_HOURS, Math.min(ConversationOption.WAIT_MAX_HOURS, argument));
                    Companions.stay(player, npc, record, (long) hours * ConversationOption.TICKS_PER_HOUR,
                            hours + (hours == 1 ? " hour" : " hours"));
                }));
        ConversationOptions.register(ConversationOption.GO_TO_BASE, CompanionOptions::rendezvousWaitAvailability,
                (npc, player, argument) -> act(npc, player, record -> Companions.goToRendezvous(player, npc, record)));
        ConversationOptions.register(ConversationOption.SET_RENDEZVOUS_HERE, CompanionOptions::rendezvousAvailability,
                (npc, player, argument) -> act(npc, player, record -> Companions.setRendezvous(player, npc)));
        ConversationOptions.register(ConversationOption.DISMISS,
                (npc, player, d) -> shownFor(npc, player, mode -> true),
                (npc, player, argument) -> act(npc, player, record -> Companions.dismiss(player, npc)));
    }

    private interface ModeTest {
        boolean test(CompanionMode mode);
    }

    private static OptionAvailability shownFor(MinibossEntity npc, ServerPlayerEntity player, ModeTest test) {
        CompanionRecord record = Companions.recordFor(player, npc);
        return record != null && test.test(record.mode) ? OptionAvailability.ENABLED : OptionAvailability.HIDDEN;
    }

    /** "Wait at rendezvous point": shown unless already there or on the way; usable only once one is set. */
    private static OptionAvailability rendezvousWaitAvailability(MinibossEntity npc, ServerPlayerEntity player,
                                                                 com.cleannrooster.anarchy_rpg.faction.Disposition disposition) {
        OptionAvailability shown = shownFor(npc, player, mode -> mode != CompanionMode.AT_BASE && mode != CompanionMode.TRAVELING_TO_BASE);
        if (!shown.shown()) {
            return shown;
        }
        if (Companions.rendezvousOf(player.getServer(), player.getUuid()) == null) {
            return OptionAvailability.disabled(Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".rendezvous.none"));
        }
        return OptionAvailability.ENABLED;
    }

    /** Shown to the owner always; usable only within the configured distance of their spawn point. */
    private static OptionAvailability rendezvousAvailability(MinibossEntity npc, ServerPlayerEntity player,
                                                             com.cleannrooster.anarchy_rpg.faction.Disposition disposition) {
        if (Companions.recordFor(player, npc) == null) {
            return OptionAvailability.HIDDEN;
        }
        var here = net.minecraft.util.math.GlobalPos.create(player.getWorld().getRegistryKey(), player.getBlockPos());
        if (!Companions.withinRendezvousRadius(here, Companions.spawnOf(player))) {
            int radius = (int) RPGMinibossesEntities.config.companionRendezvousRadius;
            return OptionAvailability.disabled(Text.translatable(
                    "conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".rendezvous.too_far", radius));
        }
        return OptionAvailability.ENABLED;
    }

    private interface Order {
        void apply(CompanionRecord record);
    }

    /** Orders end the conversation; the reply goes to chat. */
    private static boolean act(MinibossEntity npc, ServerPlayerEntity player, Order order) {
        CompanionRecord record = Companions.recordFor(player, npc);
        if (record != null) {
            order.apply(record);
        }
        return false;
    }
}

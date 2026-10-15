package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOption;
import com.cleannrooster.anarchy_rpg.conversation.ConversationOptions;
import com.cleannrooster.anarchy_rpg.conversation.OptionAvailability;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Shared plumbing for companion specialties: who sees the option, the daily cooldown, and registering each
 * specialty's own check and action. Only the owner ever sees a specialty, and never while the companion is
 * down, leaving or away on a simulated trip.
 */
final class CompanionSpecialties {
    private static final long TICKS_PER_DAY = 24000L;

    private CompanionSpecialties() {
    }

    /** The specialty's own requirements once the shared ones pass: hidden, disabled with a reason, or enabled. */
    @FunctionalInterface
    interface Check {
        OptionAvailability check(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record);
    }

    /** Runs the specialty. Returns whether the conversation stays open. Marks the day itself when it succeeds. */
    @FunctionalInterface
    interface Action {
        boolean run(MinibossEntity npc, ServerPlayerEntity player, CompanionRecord record, int argument);
    }

    /** The in-game day, by the overworld clock, so sleeping through the night starts a new one. */
    static long today(MinecraftServer server) {
        return server.getOverworld().getTimeOfDay() / TICKS_PER_DAY;
    }

    static boolean usedToday(CompanionRecord record, MinecraftServer server) {
        return record.specialtyDay >= today(server);
    }

    static void markUsed(CompanionRecord record, MinecraftServer server) {
        record.specialtyDay = today(server);
        CompanionRegistry.get(server).markDirty();
    }

    static void resetDay(CompanionRecord record, MinecraftServer server) {
        record.specialtyDay = -1;
        CompanionRegistry.get(server).markDirty();
    }

    /** Registers {@code specialty}'s conversation option. */
    static void register(CompanionSpecialty specialty, Check check, Action action) {
        ConversationOption option = specialty.option();
        if (option == null) {
            throw new IllegalArgumentException(specialty + " has no conversation option");
        }
        ConversationOptions.register(option,
                (npc, player, disposition) -> availability(specialty, check, npc, player),
                (npc, player, argument) -> {
                    CompanionRecord record = Companions.recordFor(player, npc);
                    // The conversation already re-checked availability; the record cannot be null here.
                    return record != null && action.run(npc, player, record, argument);
                });
    }

    private static OptionAvailability availability(CompanionSpecialty specialty, Check check, MinibossEntity npc, ServerPlayerEntity player) {
        CompanionRecord record = Companions.recordFor(player, npc);
        if (record == null || CompanionSpecialty.of(npc) != specialty || npc.isIncapacitated() || npc.isDeparting() || record.onTrip()) {
            return OptionAvailability.HIDDEN;
        }
        OptionAvailability own = check.check(npc, player, record);
        if (!own.shown()) {
            return own;
        }
        if (specialty.daily() && usedToday(record, player.getServer())) {
            return OptionAvailability.disabled(Text.translatable("conversation." + RPGMinibosses.CONTENT_NAMESPACE + ".specialty.tomorrow"));
        }
        return own;
    }
}

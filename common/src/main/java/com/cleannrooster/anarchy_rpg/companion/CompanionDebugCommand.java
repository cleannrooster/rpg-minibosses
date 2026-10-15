package com.cleannrooster.anarchy_rpg.companion;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.GlobalPos;

/**
 * {@code /rpgminibosses companion} — your companion's mode, timers, trip, last known place and base, for
 * checking the companion system in game. {@code companion specialty} shows its specialty and whether today's
 * use is spent; {@code companion specialty reset} gives it back.
 */
final class CompanionDebugCommand {

    private CompanionDebugCommand() {
    }

    static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("rpgminibosses")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("companion").executes(CompanionDebugCommand::show)
                                .then(CommandManager.literal("specialty").executes(context -> specialty(context, false))
                                        .then(CommandManager.literal("reset").executes(context -> specialty(context, true)))))));
    }

    private static int show(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        var server = context.getSource().getServer();
        CompanionRecord record = CompanionRegistry.get(server).ofOwner(player.getUuid());
        GlobalPos base = Companions.rendezvousOf(server, player.getUuid());
        if (record == null) {
            context.getSource().sendFeedback(() -> Text.literal("No companion. Rendezvous: " + describe(base)), false);
            return 1;
        }
        long now = Companions.now(server);
        StringBuilder line = new StringBuilder("Companion " + record.npc().toString().substring(0, 8) + ": " + record.mode());
        switch (record.mode()) {
            case STAYING -> line.append(", stays for ").append(Math.max(0, record.stayUntil() - now)).append(" ticks");
            case DOWNED -> line.append(", up in ").append(Math.max(0, record.downUntil() - now)).append(" ticks");
            default -> { }
        }
        if (record.onTrip()) {
            line.append(", on a simulated trip, arriving in ").append(Math.max(0, record.tripArrivesAt() - now)).append(" ticks");
        }
        line.append(", loaded: ").append(Companions.findAnywhere(server, record.npc()) != null);
        line.append(", last seen ").append(record.lastKnown() == null ? "never" : describe(record.lastKnown()));
        line.append(", rendezvous ").append(describe(base));
        String text = line.toString();
        context.getSource().sendFeedback(() -> Text.literal(text), false);
        return 1;
    }

    private static int specialty(CommandContext<ServerCommandSource> context, boolean reset) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        var server = context.getSource().getServer();
        CompanionRecord record = CompanionRegistry.get(server).ofOwner(player.getUuid());
        if (record == null) {
            context.getSource().sendError(Text.literal("No companion."));
            return 0;
        }
        if (reset) {
            CompanionSpecialties.resetDay(record, server);
        }
        var npc = Companions.findAnywhere(server, record.npc());
        String specialty = npc instanceof com.cleannrooster.anarchy_rpg.entity.MinibossEntity miniboss
                ? CompanionSpecialty.of(miniboss).name() : "unknown (not loaded)";
        String text = "Specialty " + specialty + ", today is day " + CompanionSpecialties.today(server)
                + ", last used " + (record.specialtyDay() < 0 ? "never" : "day " + record.specialtyDay())
                + (CompanionSpecialties.usedToday(record, server) ? " (spent)" : " (ready)")
                + ", patrolling " + record.patrolling()
                + (record.patrolling() ? " (rounds " + record.completedPatrols() + ", leashed " + record.patrolLeashes()
                        + ", this round " + record.patrolRoundTicks() + " ticks)" : "");
        context.getSource().sendFeedback(() -> Text.literal(text), false);
        return 1;
    }

    private static String describe(@org.jetbrains.annotations.Nullable GlobalPos pos) {
        if (pos == null) {
            return "none set";
        }
        return pos.pos().toShortString() + " in " + pos.dimension().getValue();
    }
}

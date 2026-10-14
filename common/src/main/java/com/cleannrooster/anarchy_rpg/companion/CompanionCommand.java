package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Player commands, no permission needed, so nobody can be locked out of companions:
 * <ul>
 *   <li>{@code /companion recall} — your companion makes its way home (rendezvous point, else spawn) from
 *   wherever it is, loaded or not;</li>
 *   <li>{@code /companion release} — frees your companion slot outright, for a companion that is truly
 *   stuck. Its gear and storage come back to you if it is loaded; otherwise they are lost with it.</li>
 * </ul>
 */
final class CompanionCommand {

    private CompanionCommand() {
    }

    static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("companion")
                        .then(CommandManager.literal("recall").executes(CompanionCommand::recall))
                        .then(CommandManager.literal("release").executes(CompanionCommand::release))));
    }

    private static int recall(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        CompanionRecord record = CompanionRegistry.get(player.getServer()).ofOwner(player.getUuid());
        if (record == null) {
            context.getSource().sendError(text("none"));
            return 0;
        }
        if (!record.onTrip()) {
            MinibossEntity loaded = Companions.findAnywhere(player.getServer(), record.npc()) instanceof MinibossEntity npc ? npc : null;
            Companions.sendHome(player.getServer(), record, loaded);
        }
        context.getSource().sendFeedback(() -> text("recalled"), false);
        return 1;
    }

    private static int release(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        CompanionRegistry registry = CompanionRegistry.get(player.getServer());
        CompanionRecord record = registry.ofOwner(player.getUuid());
        if (record == null) {
            context.getSource().sendError(text("none"));
            return 0;
        }
        if (Companions.findAnywhere(player.getServer(), record.npc()) instanceof MinibossEntity npc
                && npc.getCompanionIncarnation() == record.incarnation) {
            // Loaded: a normal dismissal, gear and storage handed back.
            Companions.dismiss(player, npc);
            context.getSource().sendFeedback(() -> text("released"), false);
            return 1;
        }
        // Unloaded or lost: free the slot. Any copy that loads later sees no record and simply leaves.
        registry.release(player.getUuid());
        context.getSource().sendFeedback(() -> text("released_unloaded"), false);
        return 1;
    }

    private static Text text(String key) {
        return Text.translatable("companion." + RPGMinibosses.CONTENT_NAMESPACE + "." + key);
    }
}

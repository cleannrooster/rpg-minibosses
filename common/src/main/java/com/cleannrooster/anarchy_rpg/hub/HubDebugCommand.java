package com.cleannrooster.anarchy_rpg.hub;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.cleannrooster.anarchy_rpg.faction.FactionState;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Operator commands for checking hubs in game:
 * <ul>
 *   <li>{@code /rpgminibosses hubs} — the nearest hub: kind, faction, residents present, and time to the
 *   next rotation;</li>
 *   <li>{@code /rpgminibosses hubs rotate} — rotate the nearest hub now, even with you standing in it.</li>
 * </ul>
 */
final class HubDebugCommand {
    private static final double SEARCH = 256.0;

    private HubDebugCommand() {
    }

    static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("rpgminibosses")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("hubs")
                                .executes(HubDebugCommand::show)
                                .then(CommandManager.literal("rotate").executes(HubDebugCommand::rotate)))));
    }

    private static int show(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        HubAnchor anchor = HubTurnover.nearest(player.getServerWorld(), player.getBlockPos(), SEARCH);
        if (anchor == null) {
            context.getSource().sendError(Text.literal("No hub found within " + (int) SEARCH + " blocks."));
            return 0;
        }
        long present = anchor.residents.stream()
                .filter(resident -> player.getServerWorld().getEntity(resident.uuid()) instanceof MinibossEntity).count();
        long interval = RPGMinibossesEntities.config.hubRotationTicks;
        long next = anchor.lastRotation + interval - player.getServerWorld().getTime();
        Text faction = anchor.party == null ? Text.literal("no party yet")
                : FactionState.roster(context.getSource().getServer()).get(anchor.party.faction()).name();
        Text line = Text.literal(anchor.kind + " at " + anchor.center().toShortString() + " — ")
                .append(faction)
                .append(Text.literal(", residents " + present + " present / " + anchor.residents.size() + " on roster"
                        + ", next rotation in " + Math.max(0, next) + " ticks"));
        context.getSource().sendFeedback(() -> line, false);
        return 1;
    }

    private static int rotate(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        HubAnchor anchor = HubTurnover.nearest(player.getServerWorld(), player.getBlockPos(), SEARCH);
        if (anchor == null) {
            context.getSource().sendError(Text.literal("No hub found within " + (int) SEARCH + " blocks."));
            return 0;
        }
        HubTurnover.handle(player.getServerWorld(), anchor, true);
        HubState.get(player.getServerWorld()).markDirty();
        context.getSource().sendFeedback(() -> Text.literal("Rotated. " + anchor.residents.size() + " residents on roster."), false);
        return 1;
    }
}

package com.cleannrooster.anarchy_rpg.rumor;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;

/**
 * {@code /rpgminibosses rumors}: testing aids, operators only.
 * <ul>
 *   <li>{@code refill} — the adventurer under the crosshair gets its rumor charges back;</li>
 *   <li>{@code forget} — you forget every rumor you were told truthfully, so subjects come round again;</li>
 *   <li>{@code engage} — you count as part of the adventurers' world (a rumor subject) without talking.</li>
 * </ul>
 */
final class RumorDebugCommand {
    private static final double REACH = 16.0;

    private RumorDebugCommand() {
    }

    static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("rpgminibosses")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("rumors")
                                .then(CommandManager.literal("refill").executes(RumorDebugCommand::refill))
                                .then(CommandManager.literal("forget").executes(RumorDebugCommand::forget))
                                .then(CommandManager.literal("engage").executes(RumorDebugCommand::engage)))));
    }

    private static int refill(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        var start = player.getEyePos();
        var end = start.add(player.getRotationVector().multiply(REACH));
        EntityHitResult hit = ProjectileUtil.raycast(player, start, end,
                new Box(start, end).expand(1.0), entity -> entity instanceof MinibossEntity, REACH * REACH);
        if (hit == null || !(hit.getEntity() instanceof MinibossEntity npc)) {
            context.getSource().sendError(Text.literal("No adventurer under the crosshair."));
            return 0;
        }
        int max = RPGMinibossesEntities.config.rumorChargesMax;
        npc.getRumorCharges().refill(max);
        context.getSource().sendFeedback(() -> Text.literal(npc.getName().getString() + " has " + max + " rumors again."), false);
        return 1;
    }

    private static int forget(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        RumorState.get(player.getServer()).forget(player.getUuid());
        context.getSource().sendFeedback(() -> Text.literal("Forgot every rumor you were told."), false);
        return 1;
    }

    private static int engage(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        RumorState.get(player.getServer()).markEngaged(player.getUuid());
        context.getSource().sendFeedback(() -> Text.literal("You can now be the subject of rumors."), false);
        return 1;
    }
}

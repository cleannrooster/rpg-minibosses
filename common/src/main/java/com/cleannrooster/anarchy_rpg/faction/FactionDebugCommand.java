package com.cleannrooster.anarchy_rpg.faction;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.architectury.event.events.common.CommandRegistrationEvent;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Box;
import net.minecraft.entity.projectile.ProjectileUtil;

import java.util.Arrays;
import java.util.Locale;

/**
 * Operator commands for checking the faction system in game:
 * <ul>
 *   <li>{@code /rpgminibosses factions} — this world's roster, its relations, and your standing and
 *   disposition with each faction;</li>
 *   <li>{@code /rpgminibosses factions look} — the party, faction and disposition of the adventurer
 *   under your crosshair;</li>
 *   <li>{@code /rpgminibosses factions standing <first|second|third> <value>} — set your standing.</li>
 * </ul>
 */
final class FactionDebugCommand {
    private static final double REACH = 48.0;

    private FactionDebugCommand() {
    }

    static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("rpgminibosses")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("factions")
                                .executes(FactionDebugCommand::showRoster)
                                .then(CommandManager.literal("look").executes(FactionDebugCommand::lookAt))
                                .then(CommandManager.literal("standing")
                                        .then(CommandManager.argument("faction", StringArgumentType.word())
                                                .suggests((context, builder) -> CommandSource.suggestMatching(
                                                        Arrays.stream(FactionSlot.values()).map(slot -> slot.name().toLowerCase(Locale.ROOT)), builder))
                                                .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                                        .executes(FactionDebugCommand::setStanding)))))));
    }

    private static int showRoster(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        FactionRoster roster = FactionState.roster(context.getSource().getServer());
        StandingLedger ledger = StandingLedger.get(context.getSource().getServer());
        StandingThresholds thresholds = StandingThresholds.from(com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities.config);
        for (Faction faction : roster.all()) {
            int standing = ledger.get(player.getUuid(), faction.slot());
            MutableText line = Text.literal(faction.slot().name().toLowerCase(Locale.ROOT) + ": ")
                    .append(faction.name().copy().formatted(Formatting.BOLD))
                    .append(Text.literal(" [" + faction.flag().base().asString() + ", " + faction.flag().pattern().getPath() + "]"))
                    .append(Text.literal("  standing " + standing + " (" + Disposition.ofStanding(standing, thresholds) + ")"));
            context.getSource().sendFeedback(() -> line, false);
        }
        String relations = "relations: first-second " + roster.between(FactionSlot.FIRST, FactionSlot.SECOND)
                + ", first-third " + roster.between(FactionSlot.FIRST, FactionSlot.THIRD)
                + ", second-third " + roster.between(FactionSlot.SECOND, FactionSlot.THIRD);
        context.getSource().sendFeedback(() -> Text.literal(relations), false);
        return 1;
    }

    private static int lookAt(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        var start = player.getEyePos();
        var end = start.add(player.getRotationVector().multiply(REACH));
        EntityHitResult hit = ProjectileUtil.raycast(player, start, end,
                new Box(start, end).expand(1.0), entity -> entity instanceof MinibossEntity, REACH * REACH);
        if (hit == null || !(hit.getEntity() instanceof MinibossEntity npc)) {
            context.getSource().sendError(Text.literal("No adventurer under the crosshair."));
            return 0;
        }
        PartyMembership party = npc.getPartyMembership();
        Faction faction = FactionState.roster(context.getSource().getServer()).get(party.faction());
        Text line = npc.getName().copy()
                .append(Text.literal(" — party " + party.party().toString().substring(0, 8) + ", "))
                .append(faction.name())
                .append(Text.literal(", regards you as " + Factions.disposition(npc, player)));
        context.getSource().sendFeedback(() -> line, false);
        return 1;
    }

    private static int setStanding(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        String name = StringArgumentType.getString(context, "faction").toUpperCase(Locale.ROOT);
        FactionSlot slot;
        try {
            slot = FactionSlot.valueOf(name);
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(Text.literal("Faction must be first, second or third."));
            return 0;
        }
        StandingLedger ledger = StandingLedger.get(context.getSource().getServer());
        ledger.set(player.getUuid(), slot, IntegerArgumentType.getInteger(context, "value"));
        int now = ledger.get(player.getUuid(), slot);
        context.getSource().sendFeedback(() -> Text.literal("Standing with " + name.toLowerCase(Locale.ROOT) + " is now " + now), false);
        return 1;
    }
}

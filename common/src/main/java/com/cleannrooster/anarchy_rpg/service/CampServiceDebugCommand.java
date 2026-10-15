package com.cleannrooster.anarchy_rpg.service;

import com.cleannrooster.anarchy_rpg.companion.CompanionSpecialty;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.hub.HubAnchor;
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

import java.util.Locale;

/**
 * {@code /rpgminibosses camp}: for the adventurer under the crosshair, whether it lives at a camp or tower, which
 * archetype it is, and for each camp service the price for you, the uses left today, and why it is refused.
 * {@code camp reset} clears its counts for you.
 */
final class CampServiceDebugCommand {
    private static final double REACH = 16.0;

    private CampServiceDebugCommand() {
    }

    static void register() {
        CommandRegistrationEvent.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(CommandManager.literal("rpgminibosses")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("camp").executes(context -> run(context, false))
                                .then(CommandManager.literal("reset").executes(context -> run(context, true))))));
    }

    private static int run(CommandContext<ServerCommandSource> context, boolean reset) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
        var start = player.getEyePos();
        var end = start.add(player.getRotationVector().multiply(REACH));
        EntityHitResult hit = ProjectileUtil.raycast(player, start, end,
                new Box(start, end).expand(1.0), entity -> entity instanceof MinibossEntity, REACH * REACH);
        if (hit == null || !(hit.getEntity() instanceof MinibossEntity npc)) {
            context.getSource().sendError(Text.literal("No adventurer under the crosshair."));
            return 0;
        }
        if (reset) {
            npc.getCampServiceUses().readNbt(new net.minecraft.nbt.NbtCompound());
        }
        HubAnchor hub = CampServices.hubOf(npc);
        StringBuilder line = new StringBuilder(npc.getName().getString())
                .append(": ").append(CompanionSpecialty.of(npc).name())
                .append(hub == null ? ", not a camp resident" : ", lives at a " + hub.kind().name().toLowerCase(Locale.ROOT)
                        + " centred " + hub.center().toShortString())
                .append(", day ").append(CampServices.today(player.getServer()));
        for (PaidServiceKind kind : PaidServiceKind.values()) {
            if (!(kind.service() instanceof CampService service)) {
                continue;
            }
            Text why = service.whyNot(npc, player);
            line.append("\n  ").append(kind.name()).append(": ")
                    .append(String.format(Locale.ROOT, "%.1f", service.price(npc, player)))
                    .append(", ").append(CampServices.usesLeft(npc, player, kind, service.dailyUses())).append(" left today")
                    .append(why == null ? "" : ", refused: " + why.getString());
        }
        String text = line.toString();
        context.getSource().sendFeedback(() -> Text.literal(text), false);
        return 1;
    }
}

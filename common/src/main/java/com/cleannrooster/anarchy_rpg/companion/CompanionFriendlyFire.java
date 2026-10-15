package com.cleannrooster.anarchy_rpg.companion;

import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import dev.architectury.event.EventResult;
import dev.architectury.event.events.common.EntityEvent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.Team;
import net.minecraft.server.MinecraftServer;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Who a companion must never fight, whatever drew it in:
 * <ul>
 *   <li>another player's companion: companions with different owners never target or hurt each other;</li>
 *   <li>a teammate's side: when the two owners are on the same vanilla scoreboard team, their companions never
 *   target or hurt each other or the other owner, and the owners can't hurt each other's companions. This holds
 *   whatever the team's friendlyFire setting says.</li>
 * </ul>
 * A companion and its own owner (and the owner's other pets) were already kept apart; this rule covers them too.
 * Player against player is left to vanilla.
 *
 * <p>Enforced twice: {@link MinibossEntity#getTarget} drops such a target, so it is never chosen, and the hurt
 * event cancels any damage that still gets through (a stray arrow, an area ability, a revenge swing).</p>
 */
public final class CompanionFriendlyFire {

    private CompanionFriendlyFire() {
    }

    static void register() {
        EntityEvent.LIVING_HURT.register((victim, source, amount) ->
                mustNotFight(source.getAttacker(), victim) ? EventResult.interruptFalse() : EventResult.pass());
    }

    /** One side of a possible fight: whose it is, and whether it is a companion or the player themselves. */
    private record Side(UUID owner, boolean companion) {
    }

    private static @Nullable Side sideOf(@Nullable Entity entity) {
        if (entity instanceof MinibossEntity npc && npc.getOwnerUuid() != null) {
            return new Side(npc.getOwnerUuid(), true);
        }
        if (entity instanceof PlayerEntity player) {
            return new Side(player.getUuid(), false);
        }
        return null;
    }

    /** Whether {@code a} and {@code b} must never fight. Symmetric. Server side; false on the client. */
    public static boolean mustNotFight(@Nullable Entity a, @Nullable Entity b) {
        if (a == null || b == null || a == b || a.getServer() == null) {
            return false;
        }
        Side first = sideOf(a);
        Side second = sideOf(b);
        if (first == null || second == null || (!first.companion() && !second.companion())) {
            return false;
        }
        if (first.owner().equals(second.owner())) {
            return true;
        }
        if (first.companion() && second.companion()) {
            return true;
        }
        Team team = teamOf(a.getServer(), first.owner());
        return team != null && team == teamOf(a.getServer(), second.owner());
    }

    /** A player's scoreboard team by UUID, online or not (teams hold players by name). */
    private static @Nullable Team teamOf(MinecraftServer server, UUID player) {
        PlayerEntity online = server.getPlayerManager().getPlayer(player);
        if (online != null) {
            return online.getScoreboardTeam();
        }
        var userCache = server.getUserCache();
        if (userCache == null) {
            return null;
        }
        return userCache.getByUuid(player)
                .map(profile -> server.getScoreboard().getScoreHolderTeam(profile.getName()))
                .orElse(null);
    }
}

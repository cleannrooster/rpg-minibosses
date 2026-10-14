package com.cleannrooster.anarchy_rpg.faction;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import com.cleannrooster.anarchy_rpg.entity.MinibossEntity;
import com.cleannrooster.anarchy_rpg.entity.RPGMinibossesEntities;
import dev.architectury.registry.ReloadListenerRegistry;
import net.minecraft.resource.ResourceType;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.TypeFilter;

import java.util.Comparator;

/**
 * The faction system's entry points for the rest of the mod: how an adventurer regards a player, how
 * two adventurers regard each other, and how deeds change standing. Server-side only.
 */
public final class Factions {
    /** How far a party-less adventurer looks for a party to join. */
    private static final double ADOPT_RADIUS = 24.0;

    private Factions() {
    }

    public static void register() {
        ReloadListenerRegistry.register(ResourceType.SERVER_DATA,
                (SynchronousResourceReloader) FactionPools::reload, RPGMinibosses.id("faction_pools"));
        FactionDebugCommand.register();
    }

    /**
     * How {@code npc} regards {@code player}, in order: its owner is always devoted; true anarchy makes
     * everyone else hostile; an adventurer angry at the player is hostile while the anger lasts;
     * otherwise the player's standing with the adventurer's faction decides.
     */
    public static Disposition disposition(MinibossEntity npc, ServerPlayerEntity player) {
        if (player.getUuid().equals(npc.getOwnerUuid())) {
            return Disposition.DEVOTED;
        }
        if (RPGMinibossesEntities.config.trueAnarchy) {
            return Disposition.HOSTILE;
        }
        if (npc.getAngerTime() > 0 && player.getUuid().equals(npc.getAngryAt())) {
            return Disposition.HOSTILE;
        }
        int standing = StandingLedger.get(player.getServer()).get(player.getUuid(), npc.getPartyMembership().faction());
        return Disposition.ofStanding(standing, StandingThresholds.from(RPGMinibossesEntities.config));
    }

    /** Whether the factions of two adventurers are at war. Party-mates never are. */
    public static boolean atWar(MinibossEntity a, MinibossEntity b) {
        if (!(a.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        PartyMembership first = a.getPartyMembership();
        PartyMembership second = b.getPartyMembership();
        if (first.party().equals(second.party())) {
            return false;
        }
        return FactionState.roster(world.getServer()).between(first.faction(), second.faction()) == FactionRelation.AT_WAR;
    }

    /** Records a deed by {@code player} toward {@code npc}'s faction. */
    public static void recordDeed(MinibossEntity npc, ServerPlayerEntity player, StandingCause cause, int delta) {
        var server = player.getServer();
        StandingLedger.get(server).apply(player.getUuid(),
                new StandingEvent(npc.getPartyMembership().faction(), cause, delta, npc.getUuid()),
                FactionState.roster(server));
    }

    /**
     * The party a party-less adventurer should belong to: the party of the nearest untamed adventurer
     * within {@link #ADOPT_RADIUS} that already has one, or else a new party of one. Joining nearby
     * parties keeps adventurers that spawn one at a time into the same place (camp residents) under one
     * flag instead of each rolling its own.
     */
    public static PartyMembership defaultPartyFor(MinibossEntity npc) {
        if (npc.getWorld() instanceof ServerWorld world) {
            var nearest = world.getEntitiesByType(TypeFilter.instanceOf(MinibossEntity.class),
                            npc.getBoundingBox().expand(ADOPT_RADIUS),
                            other -> other != npc && !other.isTamed() && other.hasPartyMembership())
                    .stream()
                    .min(Comparator.comparingDouble(other -> other.squaredDistanceTo(npc)));
            if (nearest.isPresent()) {
                return nearest.get().getPartyMembership();
            }
        }
        return PartyMembership.create(npc.getRandom());
    }
}

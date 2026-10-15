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
     * The faction this adventurer currently answers to, or null for a companion or a Forsaken Adventurer: a
     * devoted companion belongs to its player, not to a faction, and the Forsaken answer to no one. A
     * companion's party is kept, only set aside while it has an owner.
     */
    public static @org.jetbrains.annotations.Nullable FactionSlot factionOf(MinibossEntity npc) {
        if (npc.getOwnerUuid() != null) {
            return null;
        }
        // Asking settles whether a newly spawned adventurer is Forsaken (see getPartyMembership).
        PartyMembership party = npc.getPartyMembership();
        return npc.isForsaken() ? null : party.faction();
    }

    /**
     * How {@code npc} regards {@code player}, in order: its owner is always devoted; true anarchy makes
     * everyone else hostile; an adventurer angry at the player is hostile while the anger lasts;
     * a factionless companion is neutral to everyone but its owner; otherwise the player's standing with the
     * adventurer's faction decides.
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
        FactionSlot faction = factionOf(npc);
        // Forsaken Adventurers answer to no guild, and no standing makes them friendly: they attack on sight.
        if (npc.isForsaken()) {
            return Disposition.HOSTILE;
        }
        if (faction == null) {
            return Disposition.NEUTRAL;
        }
        int standing = StandingLedger.get(player.getServer()).get(player.getUuid(), faction);
        Disposition byStanding = Disposition.ofStanding(standing, StandingThresholds.from(RPGMinibossesEntities.config));
        // With guildsNeverHostile, bad standing bottoms out at wary: still curt, refusing services and hiring, and
        // unreliable with rumors, but never attacking on sight. Anger from being attacked still turns them hostile.
        if (byStanding == Disposition.HOSTILE && RPGMinibossesEntities.config.guildsNeverHostile) {
            return Disposition.WARY;
        }
        return byStanding;
    }

    /** Whether the factions of two adventurers are at war. Party-mates and companions never are. */
    public static boolean atWar(MinibossEntity a, MinibossEntity b) {
        if (!(a.getWorld() instanceof ServerWorld world)) {
            return false;
        }
        // The Forsaken are at odds with every guild, though not with each other; companions with neither.
        factionOf(a);
        factionOf(b);
        if (a.getOwnerUuid() == null && b.getOwnerUuid() == null && a.isForsaken() != b.isForsaken()) {
            return true;
        }
        if (factionOf(a) == null || factionOf(b) == null) {
            return false;
        }
        PartyMembership first = a.getPartyMembership();
        PartyMembership second = b.getPartyMembership();
        if (first.party().equals(second.party())) {
            return false;
        }
        return FactionState.roster(world.getServer()).between(first.faction(), second.faction()) == FactionRelation.AT_WAR;
    }

    /** Records a deed by {@code player} toward {@code npc}'s faction. A companion has none, so nothing changes. */
    public static void recordDeed(MinibossEntity npc, ServerPlayerEntity player, StandingCause cause, int delta) {
        FactionSlot faction = factionOf(npc);
        if (faction == null) {
            return;
        }
        var server = player.getServer();
        StandingLedger.get(server).apply(player.getUuid(),
                new StandingEvent(faction, cause, delta, npc.getUuid()),
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

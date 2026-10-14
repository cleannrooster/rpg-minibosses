package com.cleannrooster.anarchy_rpg.faction;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/** One change to a player's standing with one faction, before it spreads to the other two. */
public record StandingEvent(FactionSlot faction, StandingCause cause, int delta, @Nullable UUID npc) {
}

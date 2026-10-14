package com.cleannrooster.anarchy_rpg.faction;

/** Why a player's standing with a faction changed. */
public enum StandingCause {
    KILLED_MEMBER,
    SPARED_MEMBER,
    PROVOKED,
    TRADED,
    HIRED,
    CONTRACT_DONE,
    CONTRACT_FAILED,
    COMPANION_SERVED,
    /** Set directly by an operator command. */
    ADMIN
}

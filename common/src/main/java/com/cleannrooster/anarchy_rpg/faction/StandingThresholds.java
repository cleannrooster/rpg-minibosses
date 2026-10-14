package com.cleannrooster.anarchy_rpg.faction;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;

/** Standing values at which a player's disposition with a faction changes tier. */
public record StandingThresholds(int hostileBelow, int waryBelow, int friendlyAbove, int devotedAbove) {

    public static StandingThresholds from(ServerConfig config) {
        return new StandingThresholds(config.standingHostileBelow, config.standingWaryBelow,
                config.standingFriendlyAbove, config.standingDevotedAbove);
    }
}

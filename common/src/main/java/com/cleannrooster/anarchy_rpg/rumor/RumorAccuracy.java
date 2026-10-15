package com.cleannrooster.anarchy_rpg.rumor;

import com.cleannrooster.anarchy_rpg.config.ServerConfig;
import com.cleannrooster.anarchy_rpg.faction.Disposition;

/**
 * How much to trust what an adventurer says, from how it regards the asker. Precise rumors are always true
 * and come with a fine bearing and a tight distance; the others are rounded, and sometimes wrong.
 */
public enum RumorAccuracy {
    PRECISE,
    MOSTLY,
    UNRELIABLE;

    public static RumorAccuracy of(Disposition disposition) {
        return switch (disposition) {
            case FRIENDLY, DEVOTED -> PRECISE;
            case NEUTRAL -> MOSTLY;
            case WARY, HOSTILE -> UNRELIABLE;
        };
    }

    public double truthChance(ServerConfig config) {
        return switch (this) {
            case PRECISE -> 1.0;
            case MOSTLY -> config.rumorTruthChanceNeutral;
            case UNRELIABLE -> config.rumorTruthChanceWary;
        };
    }

    public boolean fine() {
        return this == PRECISE;
    }
}

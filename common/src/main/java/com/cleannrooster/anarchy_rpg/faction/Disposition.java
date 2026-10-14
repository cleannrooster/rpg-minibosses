package com.cleannrooster.anarchy_rpg.faction;

/**
 * How an adventurer regards one particular player. Only {@link #HOSTILE} attacks on sight and refuses
 * conversation.
 */
public enum Disposition {
    HOSTILE,
    WARY,
    NEUTRAL,
    FRIENDLY,
    DEVOTED;

    /** The tier a standing value falls in, under the configured thresholds. */
    public static Disposition ofStanding(int standing, StandingThresholds thresholds) {
        if (standing < thresholds.hostileBelow()) {
            return HOSTILE;
        }
        if (standing < thresholds.waryBelow()) {
            return WARY;
        }
        if (standing > thresholds.devotedAbove()) {
            return DEVOTED;
        }
        if (standing > thresholds.friendlyAbove()) {
            return FRIENDLY;
        }
        return NEUTRAL;
    }
}

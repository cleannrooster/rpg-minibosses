package com.cleannrooster.anarchy_rpg.faction;

/**
 * How two factions regard each other. {@link #propagation} is the share of a standing change with one
 * faction that the other faction also feels: helping or hurting a faction pleases its allies and
 * displeases its enemies, and the reverse.
 */
public enum FactionRelation {
    ALLIED(0.5),
    NEUTRAL(0.0),
    RIVAL(-0.25),
    AT_WAR(-0.5);

    private final double propagation;

    FactionRelation(double propagation) {
        this.propagation = propagation;
    }

    public double propagation() {
        return this.propagation;
    }
}

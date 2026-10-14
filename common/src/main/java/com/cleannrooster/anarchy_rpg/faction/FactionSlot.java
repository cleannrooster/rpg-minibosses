package com.cleannrooster.anarchy_rpg.faction;

/**
 * One of a world's three factions. The slot is a faction's stable identity: names and flags are rolled
 * per world, but everything that refers to a faction (standing, party membership, relations) refers to
 * its slot.
 */
public enum FactionSlot {
    FIRST,
    SECOND,
    THIRD;

    private static final FactionSlot[] VALUES = values();

    public int index() {
        return this.ordinal();
    }

    public static FactionSlot fromIndex(int index) {
        return VALUES[Math.floorMod(index, VALUES.length)];
    }
}

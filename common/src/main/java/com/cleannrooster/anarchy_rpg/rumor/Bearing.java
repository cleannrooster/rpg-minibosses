package com.cleannrooster.anarchy_rpg.rumor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.random.Random;

import java.util.Locale;

/**
 * A 16-point compass bearing. Precise rumors use all sixteen; everything else is rounded to the eight
 * even points ({@link #coarse()}). North is -Z, east is +X, as on the debug screen.
 */
public enum Bearing {
    NORTH, NORTH_NORTHEAST, NORTHEAST, EAST_NORTHEAST,
    EAST, EAST_SOUTHEAST, SOUTHEAST, SOUTH_SOUTHEAST,
    SOUTH, SOUTH_SOUTHWEST, SOUTHWEST, WEST_SOUTHWEST,
    WEST, WEST_NORTHWEST, NORTHWEST, NORTH_NORTHWEST;

    private static final Bearing[] VALUES = values();

    /** The bearing of a horizontal offset (dx east, dz south). */
    public static Bearing of(double dx, double dz) {
        double degrees = Math.toDegrees(Math.atan2(dx, -dz)); // 0 = north, clockwise
        int index = MathHelper.floorMod((int) Math.round(degrees / 22.5), VALUES.length);
        return VALUES[index];
    }

    /** Rounded to the nearest of the eight even points. */
    public Bearing coarse() {
        int index = this.ordinal();
        if (index % 2 == 0) {
            return this;
        }
        return VALUES[(index + 1) % VALUES.length];
    }

    /** Turned clockwise by {@code steps} of the sixteen points. */
    public Bearing rotate(int steps) {
        return VALUES[MathHelper.floorMod(this.ordinal() + steps, VALUES.length)];
    }

    /** A wrong but plausible bearing: at least a right angle off, on the coarse compass. */
    public Bearing distort(Random random) {
        int eighths = 2 + random.nextInt(5); // 90° to 270°
        return this.coarse().rotate(eighths * 2);
    }

    public Text label() {
        return Text.translatable("rumor." + RPGMinibosses.CONTENT_NAMESPACE + ".bearing." + this.name().toLowerCase(Locale.ROOT));
    }

    /** Compass letters: N, NNE, NE, ... */
    public String abbreviation() {
        StringBuilder letters = new StringBuilder();
        for (String part : this.name().split("_")) {
            letters.append(switch (part) {
                case "NORTHEAST" -> "NE";
                case "NORTHWEST" -> "NW";
                case "SOUTHEAST" -> "SE";
                case "SOUTHWEST" -> "SW";
                default -> part.substring(0, 1);
            });
        }
        return letters.toString();
    }

    public static Bearing fromIndex(int index) {
        return VALUES[MathHelper.floorMod(index, VALUES.length)];
    }
}

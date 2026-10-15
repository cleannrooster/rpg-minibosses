package com.cleannrooster.anarchy_rpg.rumor;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.text.Text;
import net.minecraft.util.math.random.Random;

import java.util.Locale;

/**
 * How far off a rumor's subject is, deliberately fuzzy. Precise rumors use the tight bands; everything else
 * the three broad ones. Never a coordinate.
 */
public enum DistanceBand {
    // Tight bands, for precise rumors.
    UNDER_100(true, 100),
    FEW_HUNDRED(true, 300),
    FIVE_HUNDRED(true, 600),
    THOUSAND(true, 1200),
    TWO_THOUSAND(true, 2500),
    VERY_FAR(true, Double.MAX_VALUE),
    // Broad bands, for everything else.
    NEAR(false, 300),
    A_DAYS_WALK(false, 1500),
    FAR(false, Double.MAX_VALUE);

    private final boolean tight;
    /** Upper bound in blocks (exclusive); each band starts where the previous one of its kind ends. */
    private final double below;

    DistanceBand(boolean tight, double below) {
        this.tight = tight;
        this.below = below;
    }

    public static DistanceBand of(double distance, boolean tight) {
        DistanceBand last = null;
        for (DistanceBand band : values()) {
            if (band.tight != tight) {
                continue;
            }
            last = band;
            if (distance < band.below) {
                return band;
            }
        }
        return last;
    }

    /** A neighbouring band of the same kind: wrong, but not absurdly so. */
    public DistanceBand distort(Random random) {
        DistanceBand[] all = values();
        int first = this.tight ? UNDER_100.ordinal() : NEAR.ordinal();
        int last = this.tight ? VERY_FAR.ordinal() : FAR.ordinal();
        int step = random.nextBoolean() ? 1 : -1;
        int index = this.ordinal() + step;
        if (index < first || index > last) {
            index = this.ordinal() - step;
        }
        return all[index];
    }

    public Text label() {
        return Text.translatable("rumor." + RPGMinibosses.CONTENT_NAMESPACE + ".band." + this.name().toLowerCase(Locale.ROOT));
    }
}

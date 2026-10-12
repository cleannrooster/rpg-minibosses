package com.cleannrooster.rpg_minibosses.magic.fx;

import com.cleannrooster.rpg_minibosses.RPGMinibosses;
import net.minecraft.util.Identifier;

/**
 * Ids of the built-in cues: one per AtlasVFX shape, tinted by school on the client. A boss that needs
 * something bespoke registers its own id with the client's cue handlers and emits it the same way.
 */
public final class FxCues {
    /** A blink and a ring: a hit, a cast going off. {@code at}, {@code size}. */
    public static final Identifier PULSE = id("pulse");
    /** A ring running out across the ground. {@code at}, {@code size} = radius. */
    public static final Identifier NOVA = id("nova");
    /** A bloom with debris. {@code at}, {@code size}. */
    public static final Identifier EXPLOSION = id("explosion");
    /** A shaft of light rising from {@code at}. */
    public static final Identifier BLAST = id("blast");
    /** A shaft of light falling onto {@code at}: a meteor's landing. */
    public static final Identifier BLAST_FALLING = id("blast_falling");
    /** A straight beam {@code at} → {@code to}, {@code size} = width. */
    public static final Identifier BEAM = id("beam");
    /** A jagged arc {@code at} → {@code to}. */
    public static final Identifier ARC = id("arc");
    /** A blast one way: {@code at}, axis {@code to}, half-angle {@code angle}, reach {@code size}. */
    public static final Identifier CONE = id("cone");
    /** Debris thrown out from {@code at} within {@code size}. */
    public static final Identifier BURST = id("burst");
    /** Motes drifting within {@code size} of {@code at}: a cloud pulsing. */
    public static final Identifier WISP = id("wisp");
    /**
     * A horizontal blade trail from {@code at}, facing {@code to} (a direction), reaching {@code size}
     * blocks through {@code angle} degrees either side; {@code tier} 1 sweeps from the right.
     */
    public static final Identifier SLASH = id("slash");
    /** Smoke and dust rising over {@code size} around {@code at}; school is ignored. */
    public static final Identifier SMOKE = id("smoke");
    /** The school's falling motes over {@code size} around {@code at}. */
    public static final Identifier RAIN = id("rain");
    /**
     * A faint warning line {@code at} → {@code to}, {@code size} = width: marks where something is about to
     * strike without looking like the strike. Re-send every few ticks and it breathes.
     */
    public static final Identifier GUIDE_LINE = id("guide_line");
    /** A faint warning ring of radius {@code size} around {@code at}, lying across the direction {@code to}. */
    public static final Identifier GUIDE_RING = id("guide_ring");

    private FxCues() {
    }

    private static Identifier id(String name) {
        return RPGMinibosses.id("fx/" + name);
    }
}

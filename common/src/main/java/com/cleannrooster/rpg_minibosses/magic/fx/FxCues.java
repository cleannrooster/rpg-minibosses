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
    /** The school's falling motes over {@code size} around {@code at}. */
    public static final Identifier RAIN = id("rain");

    private FxCues() {
    }

    private static Identifier id(String name) {
        return RPGMinibosses.id("fx/" + name);
    }
}

package com.cleannrooster.anarchy_rpg.magic.fx;

import com.cleannrooster.anarchy_rpg.RPGMinibosses;
import net.minecraft.util.Identifier;

/**
 * Ids of this mod's own cues, drawn by {@code client.fx.CueHandlers}. The generic shapes (pulse, nova, beam,
 * slash...) are AtlasVFX's built-in cues in {@link dev.eann.atlasvfx.cue.FxCues}; both are sent the same way,
 * with {@link FxEmitter}.
 */
public final class BossCues {
    /**
     * A faint warning line {@code at} → {@code to}, {@code size} = width: marks where something is about to
     * strike without looking like the strike. Re-send every few ticks and it breathes.
     */
    public static final Identifier GUIDE_LINE = id("guide_line");
    /** A faint warning ring of radius {@code size} around {@code at}, lying across the direction {@code to}. */
    public static final Identifier GUIDE_RING = id("guide_ring");
    /**
     * Holy lightning landing at {@code at} over {@code size} blocks: a gold-white shaft coming down, forked
     * bolts striking out of the sky across the area, and a flash. The style is ignored; it is always holy.
     */
    public static final Identifier HOLY_STRIKE = id("holy_strike");

    private BossCues() {
    }

    private static Identifier id(String name) {
        return RPGMinibosses.id("fx/" + name);
    }
}

package com.cleannrooster.rpg_minibosses.client.fx;

import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import dev.eann.atlasvfx.FxBlend;
import dev.eann.atlasvfx.FxColor;
import dev.eann.atlasvfx.FxSprite;
import dev.eann.atlasvfx.FxStyle;
import dev.eann.atlasvfx.MoteRecipe;
import dev.eann.atlasvfx.Motion;

import java.util.EnumMap;
import java.util.Map;

/**
 * How each school looks in AtlasVFX: its glow colour, and the debris it throws. Built once from vanilla
 * particle sprites, so nothing here needs textures of its own. Client only.
 */
public final class SchoolStyles {
    private static final Map<MagicSchool, FxStyle> STYLES = new EnumMap<>(MagicSchool.class);

    static {
        var generic = FxSprite.particleRun("generic", 8);
        for (MagicSchool school : MagicSchool.values()) {
            var accentSprite = switch (school) {
                case FIRE -> FxSprite.particle("flame");
                case SOUL -> FxSprite.particle("soul_fire_flame");
                case FROST -> FxSprite.particle("snowflake");
                case LIGHTNING -> FxSprite.particleRun("spark", 8);
                case ARCANE, GENERIC, HEALING -> FxSprite.particleRun("glitter", 8);
                case PHYSICAL_MELEE, PHYSICAL_RANGED -> generic;
            };
            STYLES.put(school, style(school.color(), generic, accentSprite, school == MagicSchool.FROST));
        }
    }

    private SchoolStyles() {
    }

    public static FxStyle of(MagicSchool school) {
        return STYLES.get(school);
    }

    private static FxStyle style(int colour, FxSprite debrisSprite, FxSprite accentSprite, boolean snows) {
        int hot = FxColor.mix(colour, FxColor.WHITE, 0.7f);
        var debris = new MoteRecipe(debrisSprite, FxBlend.EMISSIVE, Motion.BURST,
                colour, 0.14f, 14, 0.0f, 0.05f, 0.0f, 0.0f, 0.1f, true, false);
        var accent = new MoteRecipe(accentSprite, FxBlend.EMISSIVE, Motion.BURST,
                hot, 0.12f, 18, 0.0f, 0.05f, 0.0f, 0.0f, 0.1f, accentSprite.frameCount() > 1, false);
        var wisp = accent.drifting(-0.002f).lasting(2.0f);
        var falling = snows ? accent.drifting(0.01f).lasting(2.5f) : null;
        return new FxStyle(colour, hot, debris, accent, wisp, falling, null, colour, false);
    }
}

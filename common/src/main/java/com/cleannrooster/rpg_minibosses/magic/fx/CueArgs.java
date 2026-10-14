package com.cleannrooster.rpg_minibosses.magic.fx;

import com.cleannrooster.rpg_minibosses.magic.MagicSchool;
import dev.eann.atlasvfx.cue.FxCueArgs;
import net.minecraft.util.math.Vec3d;

/**
 * AtlasVFX's cue arguments, drawn in a school's look: each school is a style the client registers under
 * {@link MagicSchool#styleId()}. Each cue reads the arguments it needs: a nova uses {@code at} and
 * {@code size}; a beam, cone or arc also uses {@code to} (an end point, or a direction for a cone);
 * {@code angle} is a cone's half-angle.
 */
public final class CueArgs {

    private CueArgs() {
    }

    public static FxCueArgs at(MagicSchool school, Vec3d at, float size, int tier, long seed) {
        return FxCueArgs.at(school.styleId(), at, size, tier, seed);
    }

    public static FxCueArgs between(MagicSchool school, Vec3d from, Vec3d to, float size, int tier, long seed) {
        return FxCueArgs.between(school.styleId(), from, to, size, tier, seed);
    }

    public static FxCueArgs cone(MagicSchool school, Vec3d at, Vec3d axis, float halfAngle, float reach, int tier, long seed) {
        return FxCueArgs.cone(school.styleId(), at, axis, halfAngle, reach, tier, seed);
    }

    /** Every argument at once; {@code entityId} -1 for none. */
    public static FxCueArgs of(MagicSchool school, Vec3d at, Vec3d to, float size, float angle, int tier, long seed,
                               int entityId) {
        return new FxCueArgs(school.styleId(), at, to, size, angle, tier, seed, entityId);
    }
}

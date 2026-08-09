package com.cleannrooster.rpg_minibosses.entity.combat.geometry;

/**
 * The geometry families an attack can be built from.
 *
 * <p>Every shape is evaluated through one parameterisation in {@link AttackGeometry}: a point on the
 * attack is {@code surfacePoint(s, t)} where {@code s} is progress through the swing and {@code t} is
 * distance along the blade. Hit detection and the particle slash both read that function, which is what
 * keeps the visible shape and the damaging shape the same object rather than two approximations.
 */
public enum AttackShape {
    /** Crescent swept in the plane containing the facing vector and the attacker's right. */
    HORIZONTAL_ARC(Family.ARC),
    /** Crescent swept in the vertical plane — overhead and rising cuts. */
    VERTICAL_ARC(Family.ARC),
    /** Crescent in a tilted plane; the same maths at a different {@code planeRoll}. */
    DIAGONAL_ARC(Family.ARC),
    /** A narrow lane extending along the facing vector, so a thrust genuinely stabs outward. */
    THRUST_LANE(Family.LANE),
    /** A wide frontal wedge resolving across its whole span in one tick. */
    FRONTAL_CONE(Family.ARC),
    /** A volume travelling with the attacker, sweeping the segment covered since last tick. */
    CHARGE_PATH(Family.PATH),
    /** A sphere centred on the origin — landing impacts and shockwaves. Resolves once. */
    RADIAL_IMPACT(Family.RADIAL);

    /** How a shape is evaluated, so callers branch on behaviour rather than on every constant. */
    public enum Family {
        ARC, LANE, PATH, RADIAL
    }

    private final Family family;

    AttackShape(Family family) {
        this.family = family;
    }

    public Family family() {
        return this.family;
    }

    /** True when the shape sweeps progressively across the active window rather than resolving at once. */
    public boolean isSwept() {
        return this == HORIZONTAL_ARC || this == VERTICAL_ARC || this == DIAGONAL_ARC || this == THRUST_LANE;
    }

    /** True when the volume is re-anchored to the attacker every tick instead of frozen at commit. */
    public boolean followsAttacker() {
        return this == CHARGE_PATH;
    }
}

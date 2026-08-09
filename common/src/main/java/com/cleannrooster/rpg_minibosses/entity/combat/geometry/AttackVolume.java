package com.cleannrooster.rpg_minibosses.entity.combat.geometry;

/**
 * The spatial half of an attack: its shape, the path its blade travels, its reach and its thickness.
 *
 * <p>Kept separate from the timing/behaviour half in
 * {@link com.cleannrooster.rpg_minibosses.entity.combat.CombatAction} so {@link AttackGeometry} depends on
 * nothing but geometry — it can be reasoned about, and checked, without an entity or a brain in sight.
 *
 * @param shape          which family the volume belongs to
 * @param swing          where the blade starts, where it finishes, and how fast it gets there
 * @param range          outer reach in blocks, measured from the attack origin
 * @param innerRadius    dead zone at the root of the blade — nothing inside is damaged or drawn
 * @param width          full lane/path width, for {@link AttackShape#THRUST_LANE} and {@link AttackShape#CHARGE_PATH}
 * @param verticalExtent full thickness of the volume perpendicular to the swing plane
 * @param offsetForward  origin offset along the attacker's facing
 * @param offsetLateral  origin offset to the attacker's right, signed by the swing's handedness
 * @param offsetVertical origin offset up from the attacker's feet
 */
public record AttackVolume(
        AttackShape shape,
        SwingPath swing,
        double range,
        double innerRadius,
        double width,
        double verticalExtent,
        double offsetForward,
        double offsetLateral,
        double offsetVertical
) {
    /** A horizontal sweep at shoulder height — the shape most of these mobs' basics want. */
    public static AttackVolume sweep(SwingPath swing, double range, double verticalExtent) {
        return new AttackVolume(AttackShape.HORIZONTAL_ARC, swing, range, 0.7, 2.0, verticalExtent,
                0.2, 0.35, 1.1);
    }

    /** A wedge resolving all at once — shoves and point-blank pokes. */
    public static AttackVolume cone(float spanDegrees, double range) {
        return new AttackVolume(AttackShape.FRONTAL_CONE, SwingPath.fixedSpan(spanDegrees), range, 0.0,
                2.0, 2.4, 0.0, 0.0, 1.0);
    }

    /** A volume carried along with a dash. */
    public static AttackVolume path(double trailLength, double width, double verticalExtent) {
        return new AttackVolume(AttackShape.CHARGE_PATH, SwingPath.lane(0.0f), trailLength, 0.0, width,
                verticalExtent, 0.0, 0.0, 1.0);
    }

    /** A shockwave centred on the attacker's feet. */
    public static AttackVolume radial(double range) {
        return new AttackVolume(AttackShape.RADIAL_IMPACT, SwingPath.fixedSpan(360.0f), range, 0.0, 2.0,
                2.0, 0.0, 0.0, 0.4);
    }

    public AttackVolume withSwing(SwingPath swing) {
        return new AttackVolume(this.shape, swing, this.range, this.innerRadius, this.width,
                this.verticalExtent, this.offsetForward, this.offsetLateral, this.offsetVertical);
    }

    public AttackVolume withOrigin(double forward, double lateral, double vertical) {
        return new AttackVolume(this.shape, this.swing, this.range, this.innerRadius, this.width,
                this.verticalExtent, forward, lateral, vertical);
    }

    public AttackVolume withInnerRadius(double innerRadius) {
        return new AttackVolume(this.shape, this.swing, this.range, innerRadius, this.width,
                this.verticalExtent, this.offsetForward, this.offsetLateral, this.offsetVertical);
    }

    /** Roll of the swing plane about the facing vector, in degrees. */
    public float planeRoll() {
        return this.swing.planeRoll();
    }
}

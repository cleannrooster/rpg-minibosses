package com.cleannrooster.rpg_minibosses.entity.combat;

import net.minecraft.particle.ParticleEffect;

/**
 * How a swing is drawn. Deliberately only the <em>look</em> — never the shape.
 *
 * <p>Where the particles go is not stated here and cannot be: {@link SwingExecution} samples them from
 * {@link com.cleannrooster.rpg_minibosses.entity.combat.geometry.AttackGeometry}, the same surface the hit
 * test reads, so the crescent on screen is a picture of the damage volume rather than a separate effect
 * that has to be kept in step with it. This record only decides which particle, how many, and how much
 * they drift.
 *
 * @param particle which particle to draw the arc with
 * @param perTick  particles emitted per active tick; the arc's density, not its size
 * @param speed    how fast each particle travels along the blade's outward direction
 * @param scatter  random positional jitter in blocks, to stop the surface reading as a wireframe
 */
public record SlashVisual(ParticleEffect particle, int perTick, double speed, double scatter) {

    /** A heavy weapon's arc: dense, slow, and thrown outward along the swing. */
    public static SlashVisual heavy(ParticleEffect particle) {
        return new SlashVisual(particle, 26, 0.24, 0.08);
    }

    /** A fast blade's arc: fewer particles, quicker, tighter. */
    public static SlashVisual light(ParticleEffect particle) {
        return new SlashVisual(particle, 16, 0.32, 0.05);
    }

    /** A shove or a body check — visible, but not a blade. */
    public static SlashVisual blunt(ParticleEffect particle) {
        return new SlashVisual(particle, 10, 0.16, 0.12);
    }

    public SlashVisual withCount(int perTick) {
        return new SlashVisual(this.particle, perTick, this.speed, this.scatter);
    }
}

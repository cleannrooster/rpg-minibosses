package com.cleannrooster.rpg_minibosses.entity.combat.geometry;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;

/**
 * Vertical aiming for attacks: how far up or down a swing tilts toward its target.
 *
 * <h2>Why this exists rather than just using the attacker's pitch</h2>
 *
 * <p>Because an entity's pitch is not a value a mob can own. {@code LookControl.tick()} begins with
 *
 * <pre>
 * if (this.shouldStayHorizontal()) this.entity.setPitch(0.0F);
 * </pre>
 *
 * and {@code MobEntity.tickNewAi()} runs {@code lookControl.tick()} <em>after</em> {@code mobTick()},
 * where the brains live. So an aim computed by combat logic is stored and then erased before anything
 * can read it, every tick. The visible symptom is melee that behaves as though the target were always
 * exactly level with the attacker — a mob on a ledge swinging straight ahead at a player below it.
 *
 * <p>Rather than fight the look control for ownership of a field it resets by design, attacks carry
 * their own aim. {@link Aiming#attackPitch()} is read at capture time, is never touched by vanilla,
 * and is independent of wherever the entity's head happens to be pointing — which also lets a mob
 * angle a swing without tilting its whole body.
 *
 * <p>Ported from the same solution in Divine Encounters; the curve and cap are deliberately identical
 * so the two mods' melee reads the same way.
 */
public final class AttackAim {

    /**
     * Hard cap, up and down. An attack tilted further than this stops reading as a swing at an angle
     * and starts reading as a swing at the floor or the sky.
     */
    public static final float MAX_PITCH = 45.0f;

    private AttackAim() {
    }

    /** A mob whose attacks aim vertically. Implemented by the entity, driven by its own tick. */
    public interface Aiming {
        /**
         * Pitch to apply to committed attacks, in Minecraft's convention: negative is up, positive is
         * down. Already curved and clamped — see {@link #curve}.
         */
        float attackPitch();
    }

    /**
     * The true elevation angle from {@code attacker} to {@code target}, in degrees, MC convention.
     *
     * <p>Measured centre-of-mass to centre-of-mass rather than eye to feet, because a swing is thrown
     * by the whole body and aiming at a player's shoes looks like a miss even when it connects.
     */
    public static float rawPitchToward(LivingEntity attacker, Entity target) {
        var dx = target.getX() - attacker.getX();
        var dz = target.getZ() - attacker.getZ();
        var dy = (target.getY() + target.getHeight() * 0.5)
                - (attacker.getY() + attacker.getHeight() * 0.5);
        var horizontal = Math.sqrt(dx * dx + dz * dz);
        if (horizontal < 1.0e-4 && Math.abs(dy) < 1.0e-4) {
            return 0.0f;
        }
        return (float) -(MathHelper.atan2(dy, horizontal) * (180.0 / Math.PI));
    }

    /** The curved, capped pitch an attack should commit at. */
    public static float pitchToward(LivingEntity attacker, Entity target) {
        return curve(rawPitchToward(attacker, target));
    }

    /**
     * Compress a true elevation angle into the usable range: {@code MAX_PITCH * tanh(raw / MAX_PITCH)}.
     *
     * <ul>
     *   <li><b>Unit slope at zero.</b> For a target within a few degrees of level — most of any fight —
     *       the aim is essentially exact. Compressing near the origin would trade away accuracy in the
     *       common case to solve a rare one.</li>
     *   <li><b>Progressive compression.</b> A target 45° above tilts the attack 34°, one 90° above
     *       tilts it 43°. Steep geometry still reads as steep without the swing going fully vertical,
     *       which on a horizontal arc would collapse the arc's lateral spread into a line and make a
     *       wide cleave much harder to read than it should be.</li>
     *   <li><b>Asymptotic, not clipped.</b> It approaches the cap without reaching it, so there is no
     *       angle at which the aim visibly stops responding. A hard clamp produces a dead zone where
     *       climbing higher changes nothing, and the player feels it.</li>
     * </ul>
     *
     * <p>Odd-symmetric, so aiming up and aiming down behave identically.
     */
    public static float curve(float rawDegrees) {
        var compressed = MAX_PITCH * (float) Math.tanh(rawDegrees / MAX_PITCH);
        return MathHelper.clamp(compressed, -MAX_PITCH, MAX_PITCH);
    }

    /**
     * Whether an attack's geometry should be tilted by the attacker's aim at all.
     *
     * <p>Radial impacts are excluded. A shockwave is a disc on the ground with no direction but
     * outward, and tilting it would lift half the ring into the air and bury the other half — turning
     * a readable ground slam into something that misses in a way the player cannot see.
     */
    public static boolean appliesTo(AttackVolume volume) {
        return volume.shape().family() != AttackShape.Family.RADIAL;
    }

    /**
     * Ease the stored aim toward a new target angle.
     *
     * <p>Attacks capture this the instant they commit, so letting it jump would make a mob snap its
     * aim onto a player who hopped up a block. Easing means the aim lags slightly, which is both more
     * readable and more honest: the attack is thrown where the target <em>was</em> when the swing
     * started, which is the contract the rest of the attack system already keeps.
     */
    public static float ease(float current, float desired, float rate) {
        return current + (desired - current) * MathHelper.clamp(rate, 0.0f, 1.0f);
    }
}

package com.cleannrooster.anarchy_rpg.magic;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * Everything an {@link Impact} needs that is the same for every target of one cast.
 *
 * @param origin    where the hit comes from — knockback pushes away from it
 * @param direct    the projectile or cloud that carried the hit, or null for a direct cast
 * @param basePower the caster's unrolled power, resolved once per cast so every target sees the same base
 * @param scale     extra multiplier for this delivery (a cloud pulse, a partial charge); 1 normally
 */
public record ImpactContext(LivingEntity caster, MagicSchool school, Vec3d origin, @Nullable Entity direct,
                            double basePower, float scale) {

    public static ImpactContext of(LivingEntity caster, MagicSchool school, Vec3d origin, @Nullable Entity direct) {
        return new ImpactContext(caster, school, origin, direct, MagicBackends.basePower(caster, school), 1.0f);
    }

    public ImpactContext withOrigin(Vec3d origin) {
        return new ImpactContext(this.caster, this.school, origin, this.direct, this.basePower, this.scale);
    }

    public ImpactContext withDirect(@Nullable Entity direct) {
        return new ImpactContext(this.caster, this.school, this.origin, direct, this.basePower, this.scale);
    }

    public ImpactContext withBasePower(double basePower) {
        return new ImpactContext(this.caster, this.school, this.origin, this.direct, basePower, this.scale);
    }

    public ImpactContext withScale(float scale) {
        return new ImpactContext(this.caster, this.school, this.origin, this.direct, this.basePower, scale);
    }
}

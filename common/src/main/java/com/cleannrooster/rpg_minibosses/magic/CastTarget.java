package com.cleannrooster.rpg_minibosses.magic;

import com.cleannrooster.rpg_minibosses.entity.combat.ActionContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

/**
 * What a cast is aimed at, frozen at the moment it was committed.
 *
 * @param entity   the target, if there is one and it is still alive
 * @param position where the target stood at commit (or wherever the cast is aimed without one)
 * @param yaw      the facing a cone or line is turned to — the caster's committed heading
 */
public record CastTarget(@Nullable LivingEntity entity, Vec3d position, float yaw) {

    /** From a running action: its committed target, target position and heading. */
    public static CastTarget of(ActionContext context) {
        var target = context.target();
        var position = context.committedTargetPos() != null ? context.committedTargetPos()
                : target != null ? target.getPos() : context.committedOrigin();
        var forward = context.committedForward();
        return new CastTarget(target, position, yawOf(forward));
    }

    /** At a living target as it stands now, faced from the caster. */
    public static CastTarget at(LivingEntity caster, LivingEntity target) {
        return new CastTarget(target, target.getPos(), yawOf(target.getPos().subtract(caster.getPos())));
    }

    /** At a point, faced from the caster. */
    public static CastTarget at(LivingEntity caster, Vec3d position) {
        return new CastTarget(null, position, yawOf(position.subtract(caster.getPos())));
    }

    /** The game's yaw for a horizontal direction: 0 is south (+z), as {@code TelegraphArea.forward} expects. */
    public static float yawOf(Vec3d direction) {
        if (direction.horizontalLengthSquared() < 1.0e-8) {
            return 0.0f;
        }
        return (float) Math.toDegrees(Math.atan2(-direction.x, direction.z));
    }
}

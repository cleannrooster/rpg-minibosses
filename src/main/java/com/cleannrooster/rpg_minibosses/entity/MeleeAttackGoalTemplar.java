package com.cleannrooster.rpg_minibosses.entity;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.mob.PathAwareEntity;

public class MeleeAttackGoalTemplar extends MeleeAttackGoal {
    public MeleeAttackGoalTemplar(PathAwareEntity mob, double speed, boolean pauseWhenMobIdle) {
        super(mob, speed, pauseWhenMobIdle);
    }

    @Override
    protected double getSquaredMaxAttackDistance(LivingEntity entity) {
        return Math.max(super.getSquaredMaxAttackDistance(entity),3.5F*3.5F);
    }
}

package com.cleannrooster.rpg_minibosses.entity.brain;

import com.cleannrooster.rpg_minibosses.entity.MinibossEntity;
import net.minecraft.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * Wraps a MobBrain as a Minecraft Goal so the brain ticks through the
 * standard goal-selector infrastructure. Add this at priority 2 in
 * initCustomGoals() replacing MeleeAttackGoal.
 *
 * Controls MOVE and LOOK so movement from other goals doesn't fight the brain.
 */
public final class MobBrainGoal extends Goal {

    private final MinibossEntity entity;
    private final MobBrain brain;

    public MobBrainGoal(MinibossEntity entity, MobBrain brain) {
        this.entity = entity;
        this.brain  = brain;
        this.setControls(EnumSet.of(Control.MOVE, Control.LOOK));
    }

    @Override
    public void start() {
        this.entity.setAttacking(true);

        super.start();
    }

    @Override
    public boolean canStart() {
        return entity.getTarget() != null && !entity.getWorld().isClient();
    }

    @Override
    public boolean shouldContinue() {
        return entity.getTarget() != null;
    }

    @Override
    public boolean shouldRunEveryTick() {
        return false;
    }

    @Override
    public void tick() {
        brain.tick();
    }

    @Override
    public void stop() {
        this.entity.setAttacking(false);

        brain.onTargetLost();
    }
}

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

    /**
     * Must stay true.
     *
     * <p>Everything the brain owns is counted in ticks and assumes it is counting real ones: attack
     * windup/active/recovery phases, ability cooldowns, and the steering integrator that rewrites the
     * mob's velocity each tick. When this returned false the goal selector only ticked the goal on
     * alternate ticks, so every one of those ran at half rate — an attack's damage resolved twice as many
     * real ticks after it started as its animation said it would, and the steering only got to write
     * velocity every other tick, letting ground friction eat the rest.
     *
     * <p>This was survivable before the combat overhaul because the old abilities did their timing through
     * {@code WorldScheduler}, which ticks with the world rather than with the goal. Moving that timing into
     * the goal-driven action runner is what made it matter.
     */
    @Override
    public boolean shouldRunEveryTick() {
        return true;
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

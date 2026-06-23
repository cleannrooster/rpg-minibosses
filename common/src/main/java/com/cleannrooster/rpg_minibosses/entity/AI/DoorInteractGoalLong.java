package com.cleannrooster.rpg_minibosses.entity.AI;

import net.minecraft.block.BlockState;
import net.minecraft.block.DoorBlock;
import net.minecraft.entity.ai.goal.DoorInteractGoal;
import net.minecraft.entity.ai.goal.LongDoorInteractGoal;
import net.minecraft.entity.mob.IllagerEntity;
import net.minecraft.entity.mob.MobEntity;

public class DoorInteractGoalLong extends LongDoorInteractGoal {
    private final boolean delayedClose;
    private int ticksLeft;

    public DoorInteractGoalLong(MobEntity mob, boolean delayedClose) {
        super(mob, false);
        this.mob = mob;
        this.delayedClose = delayedClose;
    }

    @Override
    public boolean shouldContinue() {
        return super.shouldContinue();
    }

    public boolean canStart() {
        return super.canStart();
    }

    public void start() {
        this.ticksLeft = 20;
        this.setDoorOpen(true);
    }
    protected void setDoorOpen(boolean open) {
            BlockState blockState = this.mob.getWorld().getBlockState(this.doorPos);
            if (blockState.getBlock() instanceof DoorBlock) {
                ((DoorBlock)blockState.getBlock()).setOpen(this.mob, this.mob.getWorld(), blockState, this.doorPos, open);
            }


    }
    public void stop() {
        this.setDoorOpen(false);
    }

    public void tick() {
        --this.ticksLeft;
        super.tick();
    }
}

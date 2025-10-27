package com.cleannrooster.rpg_minibosses.entity.AI;

import net.minecraft.block.DoorBlock;
import net.minecraft.entity.ai.NavigationConditions;
import net.minecraft.entity.ai.goal.DoorInteractGoal;
import net.minecraft.entity.ai.pathing.MobNavigation;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.util.math.BlockPos;

public class OpenDoorGoal extends DoorInteractGoal {
    public OpenDoorGoal(MobEntity mob) {
        super(mob);
    }


}

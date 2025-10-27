package com.cleannrooster.rpg_minibosses.entity.AI;

import com.cleannrooster.rpg_minibosses.entity.TricksterEntity;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.entity.ai.pathing.PathNodeType;

public class RogueNodeMaker extends LandPathNodeMaker {
    @Override
    public int getSuccessors(PathNode[] neighbors, PathNode current) {
        var count = super.getSuccessors(neighbors, current);
        var valid = 0;
        for (int i = 0; i < count; i++) {
            PathNode node = neighbors[i];
            float stepCost = this.distanceBetween(current, node) + node.penalty ;
            if(this.entity.getTarget() != null && this.entity.getTarget().distanceTo(this.entity) < 12) {
                if (!(stepCost >= 1.6F || (this.entity instanceof TricksterEntity trickster && trickster.defensetimer <= 0) || (this.entity.getTarget() != null && this.entity.getTarget().squaredDistanceTo(node.x, node.y, node.z) + 1 > this.entity.getTarget().squaredDistanceTo(current.x, current.y, current.z))))
                    continue;
            }
            neighbors[valid++] = node;
        }
        return valid;
    }
    protected float distanceBetween(PathNode node1, PathNode node2) {
        float dx = (float)(node2.x - node1.x);
        float dy = (float)(node2.y - node1.y);
        float dz = (float)(node2.z - node1.z);
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

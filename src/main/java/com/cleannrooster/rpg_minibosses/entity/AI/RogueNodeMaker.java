package com.cleannrooster.rpg_minibosses.entity.AI;

import com.cleannrooster.rpg_minibosses.entity.brain.impl.RogueBrain;
import net.minecraft.entity.ai.pathing.LandPathNodeMaker;
import net.minecraft.entity.ai.pathing.PathNode;

public class RogueNodeMaker extends LandPathNodeMaker {

    /** Step cost for a flat horizontal move — used as the "rough" floor. */
    private static final float ROUGH_COST_THRESHOLD = 1.05f;
    /**
     * Probability (0–1) of dropping a flat node when rough alternatives exist.
     * Higher = rogue picks difficult routes more consistently; lower = more variety.
     */
    private static final float FLAT_DROP_CHANCE = 0.55f;

    @Override
    public int getSuccessors(PathNode[] neighbors, PathNode current) {
        int count = super.getSuccessors(neighbors, current);
        if (count == 0) return 0;

        boolean withdrawing = isWithdrawing();
        double sqDistCurrentToTarget = sqDistToTarget(current);

        // Pass 1: apply direction filter during WITHDRAWING — drop nodes that close distance.
        // Separately, count how many rough nodes survive so we know whether to bias flat ones.
        int roughCount = 0;
        int valid = 0;
        for (int i = 0; i < count; i++) {
            PathNode node = neighbors[i];

            if (withdrawing && entity.getTarget() != null) {
                double sqDistNodeToTarget = sqDistToTarget(node);
                // Drop nodes that move the rogue significantly toward the target
                if (sqDistNodeToTarget < sqDistCurrentToTarget - 0.5) {
                    continue;
                }
            }

            neighbors[valid++] = node;
            float stepCost = distanceBetween(current, node) + node.penalty;
            if (stepCost >= ROUGH_COST_THRESHOLD) roughCount++;
        }

        // Pass 2: probabilistically drop flat nodes when rough alternatives are available.
        // This makes the rogue weave through uneven terrain instead of hugging the cleanest path.
        if (roughCount > 0) {
            int biasedValid = 0;
            for (int i = 0; i < valid; i++) {
                PathNode node = neighbors[i];
                float stepCost = distanceBetween(current, node) + node.penalty;
                boolean isFlat = stepCost < ROUGH_COST_THRESHOLD;
                if (isFlat && Math.random() < FLAT_DROP_CHANCE) {
                    continue;
                }
                neighbors[biasedValid++] = node;
            }
            // Safety: never leave the pathfinder with zero options
            valid = biasedValid > 0 ? biasedValid : valid;
        }

        return valid > 0 ? valid : count;
    }

    private boolean isWithdrawing() {
        if (!(entity instanceof com.cleannrooster.rpg_minibosses.entity.TricksterEntity t)) return false;
        if (!(t.brain instanceof RogueBrain rb)) return false;
        return rb.getCurrentCombatState() == RogueBrain.CombatState.WITHDRAWING;
    }

    private double sqDistToTarget(PathNode node) {
        if (entity.getTarget() == null) return 0;
        double dx = node.x - entity.getTarget().getX();
        double dz = node.z - entity.getTarget().getZ();
        return dx * dx + dz * dz;
    }

    protected float distanceBetween(PathNode node1, PathNode node2) {
        float dx = (float)(node2.x - node1.x);
        float dy = (float)(node2.y - node1.y);
        float dz = (float)(node2.z - node1.z);
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
}

package com.cleannrooster.rpg_minibosses.entity.brain;

import java.util.List;

public record PhaseTransition(
    float healthThreshold,
    BrainState forcedStance,
    List<BrainState> lockedOutStates,
    List<BrainState> unlockedStates,
    Runnable onEnter
) {
    public boolean isTriggered(float currentHealthPct) {
        return currentHealthPct <= healthThreshold;
    }
}

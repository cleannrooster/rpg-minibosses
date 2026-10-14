package com.cleannrooster.anarchy_rpg.entity.brain;

public interface BrainState {
    String id();
    default boolean isTerminal() { return false; }
}

package com.cleannrooster.rpg_minibosses.entity.brain;

public interface BrainState {
    String id();
    default boolean isTerminal() { return false; }
}

package com.cleannrooster.rpg_minibosses.entity.brain;

import java.util.function.Predicate;

public record Transition(
    BrainState from,
    Predicate<AIStimulus> condition,
    BrainState to,
    int priority
) {}

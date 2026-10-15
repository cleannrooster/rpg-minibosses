package com.cleannrooster.anarchy_rpg.companion;

/**
 * What a companion is doing.
 * <pre>
 *   recruit -> FOLLOWING <-> STAYING (always timed) -> TRAVELING_TO_BASE -> AT_BASE -> FOLLOWING
 *   any mode -- lethal blow --> DOWNED -- after a while --> the mode it was in before
 *   Juggernaut: "Guard my home" -> PATROLLING (travels to the spawn point first if far; any other order ends it)
 *   Rogue: "Lead me somewhere" -> GUIDING -> FOLLOWING (on arrival, Follow, owner gone, or timeout)
 * </pre>
 */
public enum CompanionMode {
    FOLLOWING,
    STAYING,
    TRAVELING_TO_BASE,
    AT_BASE,
    DOWNED,
    PATROLLING,
    GUIDING
}

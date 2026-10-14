package com.cleannrooster.anarchy_rpg.companion;

/**
 * What a companion is doing.
 * <pre>
 *   recruit -> FOLLOWING <-> STAYING (always timed) -> TRAVELING_TO_BASE -> AT_BASE -> FOLLOWING
 *   any mode -- lethal blow --> DOWNED -- after a while --> the mode it was in before
 * </pre>
 */
public enum CompanionMode {
    FOLLOWING,
    STAYING,
    TRAVELING_TO_BASE,
    AT_BASE,
    DOWNED
}

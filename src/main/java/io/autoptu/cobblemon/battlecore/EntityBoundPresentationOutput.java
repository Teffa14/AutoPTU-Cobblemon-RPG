package io.autoptu.cobblemon.battlecore;

/**
 * Marker contract for one adapter-neutral presentation output whose combatant identity has already
 * been resolved to the reservation's opaque presentation entity binding.
 *
 * Implementations contain rendering state only. They do not execute PTU rules or authorize entity
 * mutations independently of the authoritative battle result that produced them.
 */
public sealed interface EntityBoundPresentationOutput
        permits EntityBoundBattleHealthProjection,
                EntityBoundBattleWorldRelocation,
                EntityBoundMoveAnimation,
                EntityBoundBattlePresentationCommand {
    long sequence();

    int ordinal();
}

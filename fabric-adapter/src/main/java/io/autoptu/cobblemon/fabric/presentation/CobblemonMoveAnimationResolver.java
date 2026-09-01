package io.autoptu.cobblemon.fabric.presentation;

import java.util.Optional;

/**
 * Resolves a trusted battle move identity to a Cobblemon poser animation name.
 *
 * Implementations are presentation-only. They must resolve from server-owned authoritative metadata
 * and must never read Cobblemon BattleState, Pokemon gameplay fields, client packets or Minecraft
 * combat state to decide the animation category.
 */
@FunctionalInterface
public interface CobblemonMoveAnimationResolver {
    Optional<String> resolve(String moveId);

    static CobblemonMoveAnimationResolver none() {
        return moveId -> Optional.empty();
    }
}

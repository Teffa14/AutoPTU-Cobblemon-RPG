package io.autoptu.cobblemon.fabric.battle;

import java.util.Optional;

/**
 * Durable authority boundary for the one active world-encounter request owned by a canonical player.
 *
 * Implementations persist only already-resolved RPG/world facts. They do not infer battle legality,
 * combatants, RNG, HP, status, action economy or battle outcome.
 */
public interface WorldEncounterTriggerRequestRepository {
    Optional<WorldEncounterTriggerRequestService.Request> findPending(String canonicalPlayerId);

    boolean saveIfAbsent(WorldEncounterTriggerRequestService.Request request);

    boolean clear(String canonicalPlayerId);
}

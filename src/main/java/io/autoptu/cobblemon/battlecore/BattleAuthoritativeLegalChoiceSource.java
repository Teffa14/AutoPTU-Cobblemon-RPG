package io.autoptu.cobblemon.battlecore;

/**
 * Supplies the current legal action-space snapshot from the authoritative battle core.
 * Implementations must derive this from current server-owned runtime state; clients never
 * provide or cache a trusted choice set.
 */
@FunctionalInterface
public interface BattleAuthoritativeLegalChoiceSource {
    BattleCoreLegalChoiceSet legalChoices(String reservationId, String actorId);
}

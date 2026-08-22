package io.autoptu.cobblemon.battlecore;

/**
 * Executes one exact legal choice previously emitted by the authoritative battle core.
 * The integration layer must pass the selected choice through unchanged and must not
 * calculate legality, costs, targeting, damage, effects, or outcomes.
 */
@FunctionalInterface
public interface BattleAuthoritativeChoiceExecutor {
    void execute(String reservationId, BattleCoreLegalChoice choice);
}

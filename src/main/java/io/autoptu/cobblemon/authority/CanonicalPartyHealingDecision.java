package io.autoptu.cobblemon.authority;

import java.util.List;

/** Result of one server-authoritative out-of-battle party healing request. */
public record CanonicalPartyHealingDecision(
        Outcome outcome,
        int healedPokemon,
        int alreadyFullPokemon,
        List<String> failedPokemonIds,
        String reason
) {
    public enum Outcome {
        APPLIED,
        PARTIAL,
        NO_PARTY,
        INVALID_REQUEST
    }

    public CanonicalPartyHealingDecision {
        if (outcome == null) throw new IllegalArgumentException("outcome is required");
        if (healedPokemon < 0 || alreadyFullPokemon < 0) {
            throw new IllegalArgumentException("healing counts must be non-negative");
        }
        failedPokemonIds = failedPokemonIds == null ? List.of() : List.copyOf(failedPokemonIds);
        reason = reason == null ? "" : reason;
    }

    public boolean changedState() {
        return healedPokemon > 0;
    }
}

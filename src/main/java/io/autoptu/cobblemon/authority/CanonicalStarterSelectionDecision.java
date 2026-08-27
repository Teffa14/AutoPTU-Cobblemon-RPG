package io.autoptu.cobblemon.authority;

/** Result of a one-time server-owned starter claim. */
public record CanonicalStarterSelectionDecision(
        Outcome outcome,
        String pokemonId,
        String speciesId,
        String detail
) {
    public enum Outcome {
        CHOSEN,
        ALREADY_CHOSEN,
        INVALID_REQUEST,
        INVALID_STARTER,
        CONFLICT
    }

    public CanonicalStarterSelectionDecision {
        if (outcome == null) throw new IllegalArgumentException("outcome is required");
        pokemonId = pokemonId == null ? "" : pokemonId;
        speciesId = speciesId == null ? "" : speciesId;
        detail = detail == null ? "" : detail;
    }

    public boolean chosen() {
        return outcome == Outcome.CHOSEN;
    }
}

package io.autoptu.cobblemon.authority;

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
        pokemonId = pokemonId == null ? "" : pokemonId;
        speciesId = speciesId == null ? "" : speciesId;
        detail = detail == null ? "" : detail;
    }

    public boolean chosen() {
        return outcome == Outcome.CHOSEN;
    }
}

package io.autoptu.cobblemon.authority;

import java.util.Optional;

public record CanonicalPlayerMutationDecision(
        Outcome outcome,
        CanonicalPlayerState state,
        String reason
) {
    public enum Outcome {
        APPLIED,
        PLAYER_NOT_FOUND,
        STALE_REVISION,
        INVALID_MUTATION,
        CONCURRENT_WRITE
    }

    public CanonicalPlayerMutationDecision {
        if (outcome == null) throw new IllegalArgumentException("outcome is required");
        reason = reason == null ? "" : reason.strip();
        if (outcome == Outcome.APPLIED && state == null) {
            throw new IllegalArgumentException("applied mutation requires state");
        }
    }

    public boolean applied() {
        return outcome == Outcome.APPLIED;
    }

    public Optional<CanonicalPlayerState> resultingState() {
        return Optional.ofNullable(state);
    }
}

package io.autoptu.cobblemon.authority;

import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;

/**
 * Applies server-owned canonical player mutations behind an atomic revision check.
 *
 * The mutation callback is application/domain code running on the server. Client payloads must be
 * decoded into requests and revalidated before they ever reach this boundary; clients do not submit
 * replacement CanonicalPlayerState objects or trusted revision outcomes.
 */
public final class CanonicalPlayerMutationService {
    private final VersionedCanonicalStateRepository repository;

    public CanonicalPlayerMutationService(VersionedCanonicalStateRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public CanonicalPlayerMutationDecision mutate(
            String playerId,
            long expectedRevision,
            UnaryOperator<CanonicalPlayerState> mutation
    ) {
        if (playerId == null || playerId.isBlank() || expectedRevision < 0 || mutation == null) {
            return new CanonicalPlayerMutationDecision(
                    CanonicalPlayerMutationDecision.Outcome.INVALID_MUTATION,
                    null,
                    "playerId, non-negative expectedRevision and mutation are required"
            );
        }
        String canonicalPlayerId = playerId.strip();
        Optional<CanonicalPlayerState> currentResult = repository.findPlayer(canonicalPlayerId);
        if (currentResult.isEmpty()) {
            return new CanonicalPlayerMutationDecision(
                    CanonicalPlayerMutationDecision.Outcome.PLAYER_NOT_FOUND,
                    null,
                    "canonical player does not exist"
            );
        }

        CanonicalPlayerState current = currentResult.get();
        if (!current.playerId().equals(canonicalPlayerId)) {
            return new CanonicalPlayerMutationDecision(
                    CanonicalPlayerMutationDecision.Outcome.INVALID_MUTATION,
                    null,
                    "repository returned a mismatched canonical player identity"
            );
        }
        if (current.revision() != expectedRevision) {
            return new CanonicalPlayerMutationDecision(
                    CanonicalPlayerMutationDecision.Outcome.STALE_REVISION,
                    current,
                    "expected revision does not match authoritative state"
            );
        }

        CanonicalPlayerState replacement;
        try {
            replacement = mutation.apply(current);
        } catch (RuntimeException invalidMutation) {
            return new CanonicalPlayerMutationDecision(
                    CanonicalPlayerMutationDecision.Outcome.INVALID_MUTATION,
                    current,
                    "server mutation failed validation"
            );
        }
        if (replacement == null
                || !replacement.playerId().equals(current.playerId())
                || replacement.revision() != expectedRevision + 1) {
            return new CanonicalPlayerMutationDecision(
                    CanonicalPlayerMutationDecision.Outcome.INVALID_MUTATION,
                    current,
                    "replacement must preserve player identity and increment revision exactly once"
            );
        }

        if (!repository.replacePlayerIfRevision(canonicalPlayerId, expectedRevision, replacement)) {
            Optional<CanonicalPlayerState> latest = repository.findPlayer(canonicalPlayerId);
            return new CanonicalPlayerMutationDecision(
                    CanonicalPlayerMutationDecision.Outcome.CONCURRENT_WRITE,
                    latest.orElse(current),
                    "another authoritative write won the revision race"
            );
        }
        return new CanonicalPlayerMutationDecision(
                CanonicalPlayerMutationDecision.Outcome.APPLIED,
                replacement,
                ""
        );
    }
}

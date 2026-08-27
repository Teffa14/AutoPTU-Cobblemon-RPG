package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Server-authoritative persistent party lead mutation. Party slot zero is the encounter lead. */
public final class CanonicalPartyLeadService {
    private static final int MAX_CONCURRENT_WRITE_RETRIES = 3;

    public enum Outcome {
        APPLIED,
        ALREADY_LEAD,
        NO_PARTY,
        INVALID_SLOT,
        CONCURRENT_WRITE
    }

    public record Decision(Outcome outcome, CanonicalPlayerEncounterProfile profile, String reason) {
        public Decision {
            Objects.requireNonNull(outcome, "outcome");
            reason = reason == null ? "" : reason;
        }

        public boolean changedState() {
            return outcome == Outcome.APPLIED;
        }
    }

    private final VersionedCanonicalPlayerEncounterProfileRepository repository;

    public CanonicalPartyLeadService(VersionedCanonicalPlayerEncounterProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Promotes the requested one-based party slot to lead while preserving the relative order of all
     * other members. The server resolves the slot against the current durable party on every retry;
     * callers never submit Pokemon identity, stats, or replacement party state.
     */
    public Decision setLead(String playerId, int oneBasedSlot) {
        if (playerId == null || playerId.isBlank()) {
            return new Decision(Outcome.NO_PARTY, null, "canonical player id is required");
        }
        String canonicalPlayerId = playerId.strip();

        for (int attempt = 0; attempt < MAX_CONCURRENT_WRITE_RETRIES; attempt++) {
            Optional<CanonicalPlayerEncounterProfile> currentResult = repository.findProfile(canonicalPlayerId);
            if (currentResult.isEmpty()) {
                return new Decision(Outcome.NO_PARTY, null, "persistent canonical party is not configured");
            }

            CanonicalPlayerEncounterProfile current = currentResult.get();
            List<String> party = current.pokemonIds();
            if (oneBasedSlot < 1 || oneBasedSlot > party.size()) {
                return new Decision(
                        Outcome.INVALID_SLOT,
                        current,
                        "party slot must be between 1 and " + party.size()
                );
            }
            if (oneBasedSlot == 1) {
                return new Decision(Outcome.ALREADY_LEAD, current, "selected Pokemon is already the party lead");
            }

            ArrayList<String> reordered = new ArrayList<>(party);
            String selected = reordered.remove(oneBasedSlot - 1);
            reordered.add(0, selected);
            CanonicalPlayerEncounterProfile replacement = new CanonicalPlayerEncounterProfile(
                    current.playerId(),
                    reordered,
                    current.consumableQuantities(),
                    current.arena(),
                    current.revision() + 1
            );

            if (repository.replaceProfileIfRevision(canonicalPlayerId, current.revision(), replacement)) {
                return new Decision(Outcome.APPLIED, replacement, "");
            }
        }

        CanonicalPlayerEncounterProfile latest = repository.findProfile(canonicalPlayerId).orElse(null);
        return new Decision(
                Outcome.CONCURRENT_WRITE,
                latest,
                "party changed concurrently; retry the request"
        );
    }
}

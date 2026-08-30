package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Server-authoritative persistent party reordering. */
public final class CanonicalPartyOrderService {
    private static final int MAX_CONCURRENT_WRITE_RETRIES = 3;

    public enum Outcome {
        APPLIED,
        ALREADY_ORDERED,
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

    public CanonicalPartyOrderService(VersionedCanonicalPlayerEncounterProfileRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    /**
     * Moves one current one-based party slot to another. The server resolves both slots against the
     * durable party on every retry; callers never provide Pokemon identity or replacement party state.
     */
    public Decision move(String playerId, int fromOneBasedSlot, int toOneBasedSlot) {
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
            if (fromOneBasedSlot < 1 || fromOneBasedSlot > party.size()
                    || toOneBasedSlot < 1 || toOneBasedSlot > party.size()) {
                return new Decision(
                        Outcome.INVALID_SLOT,
                        current,
                        "party slots must be between 1 and " + party.size()
                );
            }
            if (fromOneBasedSlot == toOneBasedSlot) {
                return new Decision(Outcome.ALREADY_ORDERED, current, "Pokemon is already in that party slot");
            }

            ArrayList<String> reordered = new ArrayList<>(party);
            String selected = reordered.remove(fromOneBasedSlot - 1);
            reordered.add(toOneBasedSlot - 1, selected);
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
        return new Decision(Outcome.CONCURRENT_WRITE, latest, "party changed concurrently; retry the request");
    }
}

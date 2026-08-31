package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Read-only server-authoritative projection of durable RPG faction reputation. */
public final class CanonicalFactionReputationQueryService {
    private final CanonicalFactionCatalogue catalogue;
    private final FileCanonicalFactionReputationRepository repository;

    public CanonicalFactionReputationQueryService(
            CanonicalFactionCatalogue catalogue,
            FileCanonicalFactionReputationRepository repository
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Snapshot inspect(String playerId, String factionId) {
        var faction = catalogue.faction(factionId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical factionId: " + factionId));
        var state = repository.findOrCreate(playerId, faction.factionId());
        return new Snapshot(state.playerId(), faction.factionId(), faction.displayName(), state.reputation(), state.revision());
    }

    public record Snapshot(String playerId, String factionId, String displayName, int reputation, long revision) { }
}

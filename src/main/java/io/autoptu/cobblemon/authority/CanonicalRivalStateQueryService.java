package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Read-only projection of durable server-owned rival identity/history/story state. */
public final class CanonicalRivalStateQueryService {
    private final CanonicalRivalCatalogue catalogue;
    private final FileCanonicalRivalStateRepository repository;

    public CanonicalRivalStateQueryService(
            CanonicalRivalCatalogue catalogue,
            FileCanonicalRivalStateRepository repository
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Snapshot inspect(String playerId, String rivalId) {
        var rival = catalogue.rival(rivalId)
                .orElseThrow(() -> new IllegalArgumentException("unknown canonical rivalId: " + rivalId));
        var state = repository.findOrCreate(playerId, rival.rivalId());
        return new Snapshot(
                state.playerId(),
                rival.rivalId(),
                rival.displayName(),
                state.historyEventKeys(),
                state.storyFlags(),
                state.revision());
    }

    public record Snapshot(
            String playerId,
            String rivalId,
            String displayName,
            List<String> historyEventKeys,
            Set<String> storyFlags,
            long revision
    ) { }
}

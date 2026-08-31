package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Read-only projection of durable server-owned Trainer records. */
public final class CanonicalTrainerRecordQueryService {
    private final FileCanonicalTrainerRecordRepository repository;

    public CanonicalTrainerRecordQueryService(FileCanonicalTrainerRecordRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Snapshot inspect(String playerId) {
        var state = repository.findOrCreate(playerId);
        return new Snapshot(
                state.playerId(),
                state.wins(),
                state.losses(),
                state.badgeIds(),
                state.tournamentRecordIds(),
                state.revision());
    }

    public record Snapshot(
            String playerId,
            long wins,
            long losses,
            Set<String> badgeIds,
            List<String> tournamentRecordIds,
            long revision
    ) { }
}

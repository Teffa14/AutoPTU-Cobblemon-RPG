package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Read-only projection of durable server-owned Trainer progression. */
public final class CanonicalTrainerProgressionQueryService {
    private final FileCanonicalTrainerProgressionRepository repository;

    public CanonicalTrainerProgressionQueryService(FileCanonicalTrainerProgressionRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public Snapshot inspect(String playerId) {
        var state = repository.findOrCreate(playerId);
        return new Snapshot(state.playerId(), state.trainerLevel(), state.trainerXp(), state.revision());
    }

    public record Snapshot(String playerId, int trainerLevel, long trainerXp, long revision) { }
}

package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Read-only server-authoritative Trainer projection for Minecraft UI/command surfaces. */
public final class CanonicalTrainerSummaryService {
    private final CanonicalStateRepository players;

    public CanonicalTrainerSummaryService(CanonicalStateRepository players) {
        this.players = Objects.requireNonNull(players, "players");
    }

    public Optional<Summary> find(String playerId) {
        if (playerId == null || playerId.isBlank()) return Optional.empty();
        return players.findPlayer(playerId).map(CanonicalTrainerSummaryService::project);
    }

    private static Summary project(CanonicalPlayerState state) {
        List<String> classes = state.trainerClasses().stream().sorted().toList();
        List<Skill> skills = state.skillRanks().entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> new Skill(entry.getKey(), entry.getValue()))
                .toList();
        List<String> features = state.trainerFeatures().stream().sorted().toList();
        List<String> capabilities = state.availablePokemonCapabilities().stream().sorted().toList();
        return new Summary(
                state.playerId(),
                classes,
                skills,
                features,
                capabilities,
                state.actionPoints(),
                state.initiativeModifier(),
                state.explicitInitiativeSpeed(),
                state.teamId(),
                state.revision());
    }

    public record Skill(String id, int rank) {
        public Skill {
            if (id == null || id.isBlank()) throw new IllegalArgumentException("skill id must not be blank");
            if (rank < 0) throw new IllegalArgumentException("skill rank must be >= 0");
        }
    }

    public record Summary(
            String playerId,
            List<String> trainerClasses,
            List<Skill> skills,
            List<String> trainerFeatures,
            List<String> availablePokemonCapabilities,
            int actionPoints,
            int initiativeModifier,
            Integer explicitInitiativeSpeed,
            String teamId,
            long revision
    ) {
        public Summary {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
            trainerClasses = List.copyOf(trainerClasses == null ? List.of() : trainerClasses);
            skills = List.copyOf(skills == null ? List.of() : skills);
            trainerFeatures = List.copyOf(trainerFeatures == null ? List.of() : trainerFeatures);
            availablePokemonCapabilities = List.copyOf(
                    availablePokemonCapabilities == null ? List.of() : availablePokemonCapabilities);
            teamId = teamId == null ? "" : teamId;
            if (actionPoints < 0) throw new IllegalArgumentException("actionPoints must be >= 0");
            if (revision < 0) throw new IllegalArgumentException("revision must be >= 0");
        }
    }
}

package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Server-authoritative Gym/League registration shell. It never starts or resolves a battle. */
public final class CanonicalLeagueRegistrationService {
    private static final int MAX_STALE_RETRIES = 16;

    private final CanonicalLeagueChallengeCatalogue catalogue;
    private final CanonicalStateRepository players;
    private final FileCanonicalLeagueRegistrationRepository registrations;
    private final FileCanonicalTrainerRecordRepository records;

    public CanonicalLeagueRegistrationService(
            CanonicalLeagueChallengeCatalogue catalogue,
            CanonicalStateRepository players,
            FileCanonicalLeagueRegistrationRepository registrations,
            FileCanonicalTrainerRecordRepository records
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.players = Objects.requireNonNull(players, "players");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        this.records = Objects.requireNonNull(records, "records");
    }

    public Summary inspect(String authenticatedPlayerId) {
        String playerId = requireTrainer(authenticatedPlayerId);
        var state = registrations.findOrCreate(playerId);
        var record = records.findOrCreate(playerId);
        return project(state, record);
    }

    public RegistrationResult register(String authenticatedPlayerId, String challengeId) {
        String playerId = requireTrainer(authenticatedPlayerId);
        CanonicalLeagueChallengeCatalogue.Challenge challenge = catalogue.challenge(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("unknown authored Gym/League challenge"));

        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            var current = registrations.findOrCreate(playerId);
            if (current.challengeIds().contains(challenge.challengeId())) {
                return new RegistrationResult(false, challenge, project(current, records.findOrCreate(playerId)));
            }
            ArrayList<String> ids = new ArrayList<>(current.challengeIds());
            ids.add(challenge.challengeId());
            var replacement = new FileCanonicalLeagueRegistrationRepository.RegistrationState(
                    playerId, ids, current.revision() + 1);
            if (registrations.replaceIfRevision(replacement, current.revision())) {
                return new RegistrationResult(true, challenge, project(replacement, records.findOrCreate(playerId)));
            }
        }
        throw new IllegalStateException("Gym/League registration retry exhausted");
    }

    private String requireTrainer(String authenticatedPlayerId) {
        if (authenticatedPlayerId == null || authenticatedPlayerId.isBlank()) {
            throw new IllegalArgumentException("authenticated player id is required");
        }
        String playerId = authenticatedPlayerId.strip();
        if (players.findPlayer(playerId).isEmpty()) {
            throw new IllegalStateException("canonical Trainer state is not loaded");
        }
        return playerId;
    }

    private Summary project(
            FileCanonicalLeagueRegistrationRepository.RegistrationState state,
            FileCanonicalTrainerRecordRepository.TrainerRecord record
    ) {
        List<Registration> entries = state.challengeIds().stream()
                .map(id -> catalogue.challenge(id)
                        .map(challenge -> new Registration(challenge.challengeId(), challenge.kind(), challenge.displayName()))
                        .orElseThrow(() -> new IllegalStateException("persisted registration references unknown authored challenge " + id)))
                .toList();
        return new Summary(
                state.playerId(),
                entries,
                record.wins(),
                record.losses(),
                record.badgeIds(),
                record.tournamentRecordIds(),
                state.revision(),
                record.revision());
    }

    public record Registration(String challengeId, CanonicalLeagueChallengeCatalogue.Kind kind, String displayName) { }

    public record Summary(
            String playerId,
            List<Registration> registrations,
            long wins,
            long losses,
            Set<String> badgeIds,
            List<String> tournamentRecordIds,
            long registrationRevision,
            long recordRevision
    ) {
        public Summary {
            registrations = List.copyOf(registrations);
            badgeIds = Set.copyOf(badgeIds);
            tournamentRecordIds = List.copyOf(tournamentRecordIds);
        }
    }

    public record RegistrationResult(boolean newlyRegistered, CanonicalLeagueChallengeCatalogue.Challenge challenge, Summary summary) { }
}

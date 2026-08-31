package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/** Server-authoritative Gym/League registration shell. It never starts or resolves a battle. */
public final class CanonicalLeagueRegistrationService {
    private static final int MAX_STALE_RETRIES = 16;

    private final CanonicalLeagueChallengeCatalogue registrationCatalogue;
    private final CanonicalTrainerChallengeCatalogue challengeCatalogue;
    private final CanonicalStateRepository players;
    private final FileCanonicalLeagueRegistrationRepository registrations;
    private final FileCanonicalTrainerRecordRepository records;

    public CanonicalLeagueRegistrationService(
            CanonicalLeagueChallengeCatalogue registrationCatalogue,
            CanonicalTrainerChallengeCatalogue challengeCatalogue,
            CanonicalStateRepository players,
            FileCanonicalLeagueRegistrationRepository registrations,
            FileCanonicalTrainerRecordRepository records
    ) {
        this.registrationCatalogue = Objects.requireNonNull(registrationCatalogue, "registrationCatalogue");
        this.challengeCatalogue = Objects.requireNonNull(challengeCatalogue, "challengeCatalogue");
        this.players = Objects.requireNonNull(players, "players");
        this.registrations = Objects.requireNonNull(registrations, "registrations");
        this.records = Objects.requireNonNull(records, "records");
        for (var definition : registrationCatalogue.registrations()) {
            if (challengeCatalogue.challenge(definition.challengeId()).isEmpty()) {
                throw new IllegalArgumentException("registration references unknown canonical Trainer challenge " + definition.challengeId());
            }
        }
    }

    public Summary inspect(String authenticatedPlayerId) {
        String playerId = requireTrainer(authenticatedPlayerId);
        return project(registrations.findOrCreate(playerId), records.findOrCreate(playerId));
    }

    public RegistrationResult register(String authenticatedPlayerId, String challengeId) {
        String playerId = requireTrainer(authenticatedPlayerId);
        CanonicalLeagueChallengeCatalogue.RegistrationDefinition definition = registrationCatalogue.registration(challengeId)
                .orElseThrow(() -> new IllegalArgumentException("unknown authored Gym/League registration"));
        CanonicalTrainerChallengeCatalogue.Challenge challenge = challengeCatalogue.challenge(definition.challengeId())
                .orElseThrow(() -> new IllegalStateException("registration references unavailable canonical Trainer challenge"));

        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            var current = registrations.findOrCreate(playerId);
            if (current.challengeIds().contains(challenge.challengeId())) {
                return new RegistrationResult(false, definition.kind(), challenge, project(current, records.findOrCreate(playerId)));
            }
            ArrayList<String> ids = new ArrayList<>(current.challengeIds());
            ids.add(challenge.challengeId());
            var replacement = new FileCanonicalLeagueRegistrationRepository.RegistrationState(
                    playerId, ids, current.revision() + 1);
            if (registrations.replaceIfRevision(replacement, current.revision())) {
                return new RegistrationResult(true, definition.kind(), challenge, project(replacement, records.findOrCreate(playerId)));
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
        List<Registration> entries = state.challengeIds().stream().map(id -> {
            CanonicalLeagueChallengeCatalogue.RegistrationDefinition definition = registrationCatalogue.registration(id)
                    .orElseThrow(() -> new IllegalStateException("persisted registration references unknown authored registration " + id));
            CanonicalTrainerChallengeCatalogue.Challenge challenge = challengeCatalogue.challenge(id)
                    .orElseThrow(() -> new IllegalStateException("persisted registration references unknown canonical Trainer challenge " + id));
            return new Registration(challenge.challengeId(), definition.kind(), challenge.displayName());
        }).toList();
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

    public record RegistrationResult(
            boolean newlyRegistered,
            CanonicalLeagueChallengeCatalogue.Kind kind,
            CanonicalTrainerChallengeCatalogue.Challenge challenge,
            Summary summary
    ) { }
}

package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalLeagueRegistrationServiceTest {
    @TempDir Path tempDir;

    @Test
    void registrationUsesExistingCanonicalChallengeAndSurvivesRepositoryReopen() {
        CanonicalStateRepository players = players("player-one");
        var service = service(players, tempDir);

        var first = service.register("player-one", "cedar-gym-trial-registration");
        var repeated = service.register("player-one", "cedar-gym-trial-registration");

        assertTrue(first.newlyRegistered());
        assertFalse(repeated.newlyRegistered());
        assertEquals("Cedar Gym Trial", first.challenge().displayName());
        assertEquals(CanonicalLeagueChallengeCatalogue.Kind.GYM, first.kind());
        assertEquals(1, repeated.summary().registrations().size());
        assertEquals(1L, repeated.summary().registrationRevision());

        var reopened = service(players, tempDir).inspect("player-one");
        assertEquals(1, reopened.registrations().size());
        assertEquals("cedar-gym-trial-registration", reopened.registrations().getFirst().challengeId());
        assertEquals("Cedar Gym Trial", reopened.registrations().getFirst().displayName());
    }

    @Test
    void registrationReadIncludesOnlyAlreadyPersistedTrainerRecords() {
        CanonicalStateRepository players = players("player-one");
        var registrationRepository = new FileCanonicalLeagueRegistrationRepository(tempDir);
        var recordRepository = new FileCanonicalTrainerRecordRepository(tempDir);
        var service = new CanonicalLeagueRegistrationService(
                CanonicalLeagueChallengeCatalogue.DEFAULT,
                CanonicalTrainerChallengeCatalogue.DEFAULT,
                players,
                registrationRepository,
                recordRepository);

        var record = recordRepository.findOrCreate("player-one");
        assertTrue(recordRepository.replaceIfRevision(
                new FileCanonicalTrainerRecordRepository.TrainerRecord(
                        "player-one", 4L, 2L, Set.of("cedar-badge"), List.of("spring-open-2026"), 1L),
                record.revision()));

        var summary = service.register("player-one", "cedar-gym-trial-registration").summary();

        assertEquals(4L, summary.wins());
        assertEquals(2L, summary.losses());
        assertEquals(Set.of("cedar-badge"), summary.badgeIds());
        assertEquals(List.of("spring-open-2026"), summary.tournamentRecordIds());
    }

    @Test
    void unknownRegistrationFailsBeforeCreatingOwnerState() {
        CanonicalStateRepository players = players("player-one");
        var repository = new FileCanonicalLeagueRegistrationRepository(tempDir);
        var service = new CanonicalLeagueRegistrationService(
                CanonicalLeagueChallengeCatalogue.DEFAULT,
                CanonicalTrainerChallengeCatalogue.DEFAULT,
                players,
                repository,
                new FileCanonicalTrainerRecordRepository(tempDir));

        assertThrows(IllegalArgumentException.class, () -> service.register("player-one", "client-invented-final"));
        assertTrue(repository.find("player-one").isEmpty());
    }

    @Test
    void registrationIsOwnerScoped() {
        CanonicalStateRepository players = players("player-one", "player-two");
        var service = service(players, tempDir);

        service.register("player-one", "cedar-gym-trial-registration");

        assertEquals(1, service.inspect("player-one").registrations().size());
        assertTrue(service.inspect("player-two").registrations().isEmpty());
    }

    @Test
    void repositoryRejectsStaleRevision() {
        var repository = new FileCanonicalLeagueRegistrationRepository(tempDir);
        var initial = repository.findOrCreate("player-one");
        var first = new FileCanonicalLeagueRegistrationRepository.RegistrationState(
                "player-one", List.of("cedar-gym-trial-registration"), initial.revision() + 1);
        assertTrue(repository.replaceIfRevision(first, initial.revision()));

        var staleReplacement = new FileCanonicalLeagueRegistrationRepository.RegistrationState(
                "player-one", List.of("cedar-gym-trial-registration", "future-league"), initial.revision() + 1);
        assertFalse(repository.replaceIfRevision(staleReplacement, initial.revision()));
        assertEquals(List.of("cedar-gym-trial-registration"), repository.findOrCreate("player-one").challengeIds());
    }

    @Test
    void registrationCatalogueMustReferenceCanonicalTrainerChallenge() {
        var badRegistrationCatalogue = new CanonicalLeagueChallengeCatalogue(List.of(
                new CanonicalLeagueChallengeCatalogue.RegistrationDefinition("missing-challenge", CanonicalLeagueChallengeCatalogue.Kind.LEAGUE)));
        assertThrows(IllegalArgumentException.class, () -> new CanonicalLeagueRegistrationService(
                badRegistrationCatalogue,
                CanonicalTrainerChallengeCatalogue.DEFAULT,
                players("player-one"),
                new FileCanonicalLeagueRegistrationRepository(tempDir),
                new FileCanonicalTrainerRecordRepository(tempDir)));
    }

    private static CanonicalLeagueRegistrationService service(CanonicalStateRepository players, Path root) {
        return new CanonicalLeagueRegistrationService(
                CanonicalLeagueChallengeCatalogue.DEFAULT,
                CanonicalTrainerChallengeCatalogue.DEFAULT,
                players,
                new FileCanonicalLeagueRegistrationRepository(root),
                new FileCanonicalTrainerRecordRepository(root));
    }

    private static CanonicalStateRepository players(String... playerIds) {
        Set<String> ids = Set.of(playerIds);
        return playerId -> ids.contains(playerId)
                ? Optional.of(new CanonicalPlayerState(playerId, Set.of(), Map.of(), Set.of(), 0L))
                : Optional.empty();
    }
}

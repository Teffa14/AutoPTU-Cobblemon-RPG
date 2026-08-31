package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalTrainerRecordQueryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void newTrainerStartsNeutralAndSurvivesRepositoryReopen() {
        var first = new CanonicalTrainerRecordQueryService(
                new FileCanonicalTrainerRecordRepository(tempDir)
        ).inspect("trainer:alpha");

        assertEquals("trainer:alpha", first.playerId());
        assertEquals(0L, first.wins());
        assertEquals(0L, first.losses());
        assertEquals(Set.of(), first.badgeIds());
        assertEquals(List.of(), first.tournamentRecordIds());
        assertEquals(0L, first.revision());

        var reopened = new CanonicalTrainerRecordQueryService(
                new FileCanonicalTrainerRecordRepository(tempDir)
        ).inspect("trainer:alpha");
        assertEquals(first, reopened);
    }

    @Test
    void repositoryPersistsOnlyExplicitServerOwnedRecordFacts() {
        var repository = new FileCanonicalTrainerRecordRepository(tempDir);
        var current = repository.findOrCreate("trainer:alpha");
        var replacement = new FileCanonicalTrainerRecordRepository.TrainerRecord(
                current.playerId(),
                3L,
                1L,
                Set.of("badge:cedar"),
                List.of("tournament:cedar_open:participant"),
                current.revision() + 1);

        assertTrue(repository.replaceIfRevision(replacement, current.revision()));
        var reopened = new FileCanonicalTrainerRecordRepository(tempDir)
                .find("trainer:alpha").orElseThrow();
        assertEquals(3L, reopened.wins());
        assertEquals(1L, reopened.losses());
        assertEquals(Set.of("badge:cedar"), reopened.badgeIds());
        assertEquals(List.of("tournament:cedar_open:participant"), reopened.tournamentRecordIds());
        assertEquals(1L, reopened.revision());
        assertFalse(repository.replaceIfRevision(
                new FileCanonicalTrainerRecordRepository.TrainerRecord(
                        current.playerId(), 0L, 0L, Set.of(), List.of(), 1L),
                current.revision()));
    }

    @Test
    void trainersRemainOwnerScoped() {
        var repository = new FileCanonicalTrainerRecordRepository(tempDir);
        var alpha = repository.findOrCreate("trainer:alpha");
        var beta = repository.findOrCreate("trainer:beta");

        assertTrue(repository.replaceIfRevision(
                new FileCanonicalTrainerRecordRepository.TrainerRecord(
                        alpha.playerId(), 0L, 0L, Set.of("badge:cedar"), List.of(), 1L),
                alpha.revision()));
        assertEquals(Set.of(), repository.find("trainer:beta").orElseThrow().badgeIds());
        assertEquals(beta, repository.find("trainer:beta").orElseThrow());
    }
}

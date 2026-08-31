package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileCanonicalTrainerProgressionRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void createsNeutralServerOwnedBaselineAndSurvivesRepositoryReopen() {
        var first = new FileCanonicalTrainerProgressionRepository(tempDir);
        var created = first.findOrCreate("player-1");
        assertEquals(1, created.trainerLevel());
        assertEquals(0L, created.trainerXp());
        assertEquals(0L, created.revision());

        var reopened = new FileCanonicalTrainerProgressionRepository(tempDir);
        assertEquals(created, reopened.findOrCreate("player-1"));
    }

    @Test
    void revisionCasPersistsOnlyExplicitServerProvidedFacts() {
        var repository = new FileCanonicalTrainerProgressionRepository(tempDir);
        var initial = repository.findOrCreate("player-2");
        var replacement = new FileCanonicalTrainerProgressionRepository.ProgressionState(
                initial.playerId(), 3, 1250L, 1L);

        assertTrue(repository.replaceIfRevision(replacement, 0L));
        assertFalse(repository.replaceIfRevision(
                new FileCanonicalTrainerProgressionRepository.ProgressionState("player-2", 99, 999999L, 1L),
                0L));

        var reopened = new FileCanonicalTrainerProgressionRepository(tempDir);
        assertEquals(replacement, reopened.findOrCreate("player-2"));
    }
}

package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalQuestObjectiveServiceTest {
    @TempDir Path tempDir;

    @Test
    void ignoresEventsUntilQuestAcceptedThenCompletesEachObjectiveOnceAcrossRestart() {
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var objectives = new FileCanonicalQuestObjectiveRepository(tempDir);
        var service = new CanonicalQuestObjectiveService(CanonicalQuestObjectiveCatalogue.DEFAULT, journals, objectives);

        assertFalse(service.observe("player-1", "cedar_meadow:lookout_watching").changed());

        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals)
                .accept("player-1", "cedar-ranger", "cedar-field-notes");

        var first = service.observe("player-1", "cedar_meadow:lookout_watching");
        assertTrue(first.changed());
        assertEquals(1L, first.updates().getFirst().questProgress().completedCount());
        assertFalse(first.updates().getFirst().questProgress().complete());

        var duplicate = service.observe("player-1", "cedar_meadow:lookout_watching");
        assertFalse(duplicate.changed());
        assertEquals(1L, duplicate.updates().getFirst().questProgress().completedCount());

        var reopened = new CanonicalQuestObjectiveService(
                CanonicalQuestObjectiveCatalogue.DEFAULT,
                new FileCanonicalQuestJournalRepository(tempDir),
                new FileCanonicalQuestObjectiveRepository(tempDir)
        );
        var second = reopened.observe("player-1", "cedar_meadow:feeders_alarmed");
        assertTrue(second.changed());
        assertEquals(2L, second.updates().getFirst().questProgress().completedCount());
        assertTrue(second.updates().getFirst().questProgress().complete());

        var persisted = reopened.inspectQuest("player-1", "cedar-field-notes");
        assertEquals(2L, persisted.completedCount());
        assertTrue(persisted.complete());
    }

    @Test
    void unknownWorldEventsDoNotMutateProgress() {
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals)
                .accept("player-1", "cedar-ranger", "cedar-field-notes");
        var repository = new FileCanonicalQuestObjectiveRepository(tempDir);
        var service = new CanonicalQuestObjectiveService(CanonicalQuestObjectiveCatalogue.DEFAULT, journals, repository);

        assertFalse(service.observe("player-1", "client:says_completed").changed());
        assertEquals(0L, repository.findOrCreate("player-1").revision());
    }
}

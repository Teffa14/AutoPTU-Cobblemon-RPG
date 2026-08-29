package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalQuestTrackingServiceTest {
    @TempDir Path tempDir;

    @Test
    void tracksOnlyAcceptedQuestAndPersistsAcrossRepositoryReopen() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, repository)
                .accept("player-1", "cedar-ranger", "cedar-field-notes");
        var service = new CanonicalQuestTrackingService(CanonicalQuestCatalogue.DEFAULT, repository);

        var first = service.track("player-1", "cedar-field-notes");
        assertTrue(first.changed());
        assertEquals(2L, first.journalRevision());

        var retry = service.track("player-1", "cedar-field-notes");
        assertFalse(retry.changed());
        assertEquals(2L, retry.journalRevision());

        var reopened = new FileCanonicalQuestJournalRepository(tempDir).findOrCreate("player-1");
        assertEquals("cedar-field-notes", reopened.trackedQuestId());
        assertEquals(2L, reopened.revision());
    }

    @Test
    void rejectsUnknownOrUnacceptedQuestWithoutMutatingJournal() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        var service = new CanonicalQuestTrackingService(CanonicalQuestCatalogue.DEFAULT, repository);

        assertThrows(IllegalArgumentException.class, () -> service.track("player-1", "not-authored"));
        assertThrows(IllegalArgumentException.class, () -> service.track("player-1", "cedar-field-notes"));
        assertEquals(0L, repository.findOrCreate("player-1").revision());
    }

    @Test
    void queryMarksTrackedQuestFromCanonicalJournalState() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, repository)
                .accept("player-1", "cedar-ranger", "cedar-field-notes");
        new CanonicalQuestTrackingService(CanonicalQuestCatalogue.DEFAULT, repository)
                .track("player-1", "cedar-field-notes");

        var snapshot = new CanonicalQuestJournalQueryService(CanonicalQuestCatalogue.DEFAULT, repository)
                .inspect("player-1");
        assertEquals("cedar-field-notes", snapshot.trackedQuestId());
        assertTrue(snapshot.quests().getFirst().tracked());
    }
}

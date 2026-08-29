package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalQuestJournalServiceTest {
    @TempDir Path tempDir;

    @Test
    void acceptsOnceAndPersistsAcrossRepositoryReopen() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        var service = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, repository);

        var first = service.accept("player-1", "cedar-ranger", "cedar-field-notes");
        assertTrue(first.newlyAccepted());
        assertEquals(1L, first.commit().journal().revision());
        assertEquals(FileCanonicalQuestJournalRepository.QuestState.ACCEPTED,
                first.commit().journal().entries().get("cedar-field-notes").state());

        var retry = service.accept("player-1", "cedar-ranger", "cedar-field-notes");
        assertFalse(retry.newlyAccepted());
        assertEquals(1L, retry.commit().journal().revision());

        var reopened = new FileCanonicalQuestJournalRepository(tempDir).findOrCreate("player-1");
        assertEquals(1L, reopened.revision());
        assertEquals(1, reopened.entries().size());
    }

    @Test
    void rejectsWrongQuestGiverAndStaleRepositoryMutation() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        var service = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, repository);
        assertThrows(IllegalArgumentException.class,
                () -> service.accept("player-1", "someone-else", "cedar-field-notes"));

        var current = repository.findOrCreate("player-1");
        var accepted = repository.accept("player-1", "cedar-field-notes", current.revision());
        assertEquals(FileCanonicalQuestJournalRepository.AcceptStatus.ACCEPTED, accepted.status());
        var stale = repository.accept("player-1", "another-quest", 0L);
        assertEquals(FileCanonicalQuestJournalRepository.AcceptStatus.STALE_REVISION, stale.status());
        assertFalse(stale.journal().entries().containsKey("another-quest"));
    }
}

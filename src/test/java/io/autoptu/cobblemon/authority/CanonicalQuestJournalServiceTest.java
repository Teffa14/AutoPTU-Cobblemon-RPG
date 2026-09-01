package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalQuestJournalServiceTest {
    @TempDir Path tempDir;

    @Test
    void acceptsOnceAndPersistsAcrossRepositoryReopen() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        var storyRepository = new FileCanonicalWorldStoryRepository(tempDir);
        new CanonicalWorldStoryService(CanonicalWorldStoryCatalogue.DEFAULT, storyRepository)
                .choose("player-1", "cedar-meadow-approach", "observe-first");
        var service = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, repository, storyRepository);

        var first = service.accept("player-1", "cedar-ranger", "cedar-field-notes");
        assertTrue(first.newlyAccepted());
        assertEquals(1L, first.commit().journal().revision());
        assertEquals(FileCanonicalQuestJournalRepository.QuestState.ACCEPTED,
                first.commit().journal().entries().get("cedar-field-notes").state());

        var retry = service.accept("player-1", "cedar-ranger", "cedar-field-notes");
        assertFalse(retry.newlyAccepted());
        assertEquals(CanonicalQuestJournalService.AcceptQuestStatus.ALREADY_ACCEPTED, retry.status());
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

    @Test
    void blocksAuthoredQuestUntilCanonicalPrerequisiteIsAcceptedThenUnlocksIt() {
        var repository = new FileCanonicalQuestJournalRepository(tempDir);
        var service = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, repository);
        String playerId = "trainer:marea";

        var eligibility = service.eligibility(playerId, "ouros.npc.mara_veyra", "marea-route-field-check");
        assertFalse(eligibility.eligible());
        assertEquals(java.util.List.of("marea-market-shortfall"), eligibility.missingAcceptedQuestIds());
        assertTrue(eligibility.missingStoryFlags().isEmpty());

        var blocked = service.accept(playerId, "ouros.npc.mara_veyra", "marea-route-field-check");
        assertTrue(blocked.blockedByPrerequisites());
        assertNull(blocked.commit());
        assertEquals(0L, blocked.journal().revision());
        assertFalse(blocked.journal().entries().containsKey("marea-route-field-check"));

        var root = service.accept(playerId, "ouros.npc.ivo_serrat", "marea-market-shortfall");
        assertTrue(root.newlyAccepted());
        var unlocked = service.eligibility(playerId, "ouros.npc.mara_veyra", "marea-route-field-check");
        assertTrue(unlocked.eligible());
        assertTrue(unlocked.missingAcceptedQuestIds().isEmpty());

        var route = service.accept(playerId, "ouros.npc.mara_veyra", "marea-route-field-check");
        assertTrue(route.newlyAccepted());
        assertEquals(2L, route.journal().revision());
        assertTrue(route.journal().entries().containsKey("marea-route-field-check"));
    }

    @Test
    void blocksCedarFieldNotesUntilPersistedObserveFirstStoryFlagExists() {
        String playerId = "trainer:cedar-story";
        var journals = new FileCanonicalQuestJournalRepository(tempDir);
        var stories = new FileCanonicalWorldStoryRepository(tempDir);
        var service = new CanonicalQuestJournalService(CanonicalQuestCatalogue.DEFAULT, journals, stories);

        var blocked = service.accept(playerId, "cedar-ranger", "cedar-field-notes");
        assertTrue(blocked.blockedByPrerequisites());
        assertEquals(java.util.List.of("cedar_meadow_observe_first"), blocked.missingStoryFlags());
        assertTrue(blocked.missingAcceptedQuestIds().isEmpty());
        assertEquals(0L, blocked.journal().revision());
        assertFalse(blocked.journal().entries().containsKey("cedar-field-notes"));

        new CanonicalWorldStoryService(CanonicalWorldStoryCatalogue.DEFAULT, stories)
                .choose(playerId, "cedar-meadow-approach", "observe-first");

        var eligibility = service.eligibility(playerId, "cedar-ranger", "cedar-field-notes");
        assertTrue(eligibility.eligible());
        assertTrue(eligibility.missingStoryFlags().isEmpty());
        assertEquals(1L, eligibility.storyRevision());

        var accepted = service.accept(playerId, "cedar-ranger", "cedar-field-notes");
        assertTrue(accepted.newlyAccepted());
        assertTrue(accepted.missingStoryFlags().isEmpty());
        assertEquals(1L, accepted.journal().revision());
    }
}

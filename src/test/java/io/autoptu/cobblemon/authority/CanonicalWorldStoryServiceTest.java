package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class CanonicalWorldStoryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void authoredChoiceCommitsConsequencesOnceAndSurvivesReopen() {
        var service = new CanonicalWorldStoryService(
                CanonicalWorldStoryCatalogue.DEFAULT,
                new FileCanonicalWorldStoryRepository(tempDir));

        var first = service.choose("trainer:alpha", "cedar-meadow-approach", "observe-first");
        assertTrue(first.newlyCommitted());
        assertEquals(Map.of("cedar-meadow-approach", "observe-first"), first.snapshot().selectedChoices());
        assertEquals(Set.of("cedar_meadow_observe_first"), first.snapshot().storyFlags());
        assertEquals(1L, first.snapshot().revision());

        var reopened = new CanonicalWorldStoryService(
                CanonicalWorldStoryCatalogue.DEFAULT,
                new FileCanonicalWorldStoryRepository(tempDir));
        var repeated = reopened.choose("trainer:alpha", "cedar-meadow-approach", "observe-first");
        assertFalse(repeated.newlyCommitted());
        assertEquals(1L, repeated.snapshot().revision());
        assertEquals(first.snapshot(), reopened.inspect("trainer:alpha"));
    }

    @Test
    void clientCannotInventNodeChoiceOrRewriteCommittedChoice() {
        var service = new CanonicalWorldStoryService(
                CanonicalWorldStoryCatalogue.DEFAULT,
                new FileCanonicalWorldStoryRepository(tempDir));

        assertThrows(IllegalArgumentException.class,
                () -> service.choose("trainer:alpha", "client-node", "observe-first"));
        assertThrows(IllegalArgumentException.class,
                () -> service.choose("trainer:alpha", "cedar-meadow-approach", "client-choice"));
        assertEquals(0L, service.inspect("trainer:alpha").revision());

        service.choose("trainer:alpha", "cedar-meadow-approach", "observe-first");
        assertThrows(IllegalStateException.class,
                () -> service.choose("trainer:alpha", "cedar-meadow-approach", "engage-directly"));
        var state = service.inspect("trainer:alpha");
        assertEquals("observe-first", state.selectedChoices().get("cedar-meadow-approach"));
        assertFalse(state.storyFlags().contains("cedar_meadow_engage_directly"));
    }

    @Test
    void storyStateIsOwnerScopedAndRepositoryRejectsStaleCas() {
        var repository = new FileCanonicalWorldStoryRepository(tempDir);
        var service = new CanonicalWorldStoryService(CanonicalWorldStoryCatalogue.DEFAULT, repository);
        service.choose("trainer:alpha", "cedar-meadow-approach", "observe-first");

        assertTrue(service.inspect("trainer:beta").selectedChoices().isEmpty());
        var alpha = repository.find("trainer:alpha").orElseThrow();
        var stale = new FileCanonicalWorldStoryRepository.StoryState(
                alpha.playerId(), alpha.selectedChoices(), alpha.storyFlags(), alpha.revision() + 1);
        assertTrue(repository.replaceIfRevision(stale, alpha.revision()));
        assertFalse(repository.replaceIfRevision(
                new FileCanonicalWorldStoryRepository.StoryState(
                        alpha.playerId(), alpha.selectedChoices(), alpha.storyFlags(), alpha.revision() + 1),
                alpha.revision()));
    }
}

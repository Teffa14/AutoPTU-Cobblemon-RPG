package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalRivalStateQueryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void knownRivalStartsEmptyAndSurvivesRepositoryReopen() {
        var firstRepository = new FileCanonicalRivalStateRepository(tempDir);
        var firstService = new CanonicalRivalStateQueryService(CanonicalRivalCatalogue.DEFAULT, firstRepository);

        var first = firstService.inspect("trainer:alpha", "cedar_challenger");
        assertEquals("trainer:alpha", first.playerId());
        assertEquals("cedar_challenger", first.rivalId());
        assertEquals("Cedar Challenger", first.displayName());
        assertEquals(List.of(), first.historyEventKeys());
        assertEquals(Set.of(), first.storyFlags());
        assertEquals(0L, first.revision());

        var reopened = new CanonicalRivalStateQueryService(
                CanonicalRivalCatalogue.DEFAULT,
                new FileCanonicalRivalStateRepository(tempDir)
        ).inspect("trainer:alpha", "cedar_challenger");
        assertEquals(first, reopened);
    }

    @Test
    void repositoryPersistsOnlyExplicitServerOwnedHistoryAndFlags() {
        var repository = new FileCanonicalRivalStateRepository(tempDir);
        var current = repository.findOrCreate("trainer:alpha", "cedar_challenger");
        var replacement = new FileCanonicalRivalStateRepository.RivalState(
                current.playerId(),
                current.rivalId(),
                List.of("story:introduced"),
                Set.of("story:cedar_path_open"),
                current.revision() + 1);

        assertTrue(repository.replaceIfRevision(replacement, current.revision()));
        var reopened = new FileCanonicalRivalStateRepository(tempDir)
                .find("trainer:alpha", "cedar_challenger").orElseThrow();
        assertEquals(List.of("story:introduced"), reopened.historyEventKeys());
        assertEquals(Set.of("story:cedar_path_open"), reopened.storyFlags());
        assertEquals(1L, reopened.revision());
        assertFalse(repository.replaceIfRevision(
                new FileCanonicalRivalStateRepository.RivalState(
                        current.playerId(), current.rivalId(), List.of(), Set.of(), 1L),
                current.revision()));
    }

    @Test
    void unknownRivalIsRejectedBeforeStateProvisioning() {
        var repository = new FileCanonicalRivalStateRepository(tempDir);
        var service = new CanonicalRivalStateQueryService(CanonicalRivalCatalogue.DEFAULT, repository);

        assertThrows(IllegalArgumentException.class, () -> service.inspect("trainer:alpha", "client_rival"));
        assertTrue(repository.find("trainer:alpha", "client_rival").isEmpty());
    }
}

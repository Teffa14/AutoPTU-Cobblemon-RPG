package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalFactionReputationQueryServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void knownFactionStartsNeutralAndSurvivesRepositoryReopen() {
        var firstRepository = new FileCanonicalFactionReputationRepository(tempDir);
        var firstService = new CanonicalFactionReputationQueryService(CanonicalFactionCatalogue.DEFAULT, firstRepository);

        var first = firstService.inspect("trainer:alpha", "cedar_rangers");
        assertEquals("trainer:alpha", first.playerId());
        assertEquals("cedar_rangers", first.factionId());
        assertEquals("Cedar Rangers", first.displayName());
        assertEquals(0, first.reputation());
        assertEquals(0L, first.revision());

        var reopenedRepository = new FileCanonicalFactionReputationRepository(tempDir);
        var reopened = new CanonicalFactionReputationQueryService(CanonicalFactionCatalogue.DEFAULT, reopenedRepository)
                .inspect("trainer:alpha", "cedar_rangers");
        assertEquals(first, reopened);
    }

    @Test
    void differentTrainersRemainOwnerScoped() {
        var repository = new FileCanonicalFactionReputationRepository(tempDir);
        var service = new CanonicalFactionReputationQueryService(CanonicalFactionCatalogue.DEFAULT, repository);

        var alpha = service.inspect("trainer:alpha", "cedar_rangers");
        var beta = service.inspect("trainer:beta", "cedar_rangers");

        assertEquals("trainer:alpha", alpha.playerId());
        assertEquals("trainer:beta", beta.playerId());
        assertEquals(0, alpha.reputation());
        assertEquals(0, beta.reputation());
    }

    @Test
    void unknownFactionIsRejectedBeforeAnyStateIsProvisioned() {
        var repository = new FileCanonicalFactionReputationRepository(tempDir);
        var service = new CanonicalFactionReputationQueryService(CanonicalFactionCatalogue.DEFAULT, repository);

        assertThrows(IllegalArgumentException.class, () -> service.inspect("trainer:alpha", "client_invented"));
        assertFalse(repository.find("trainer:alpha", "client_invented").isPresent());
    }

    @Test
    void repositoryRequiresExactRevisionForFutureAuthorizedMutations() {
        var repository = new FileCanonicalFactionReputationRepository(tempDir);
        var current = repository.findOrCreate("trainer:alpha", "cedar_rangers");
        var replacement = new FileCanonicalFactionReputationRepository.ReputationState(
                current.playerId(), current.factionId(), 5, current.revision() + 1);

        assertEquals(true, repository.replaceIfRevision(replacement, current.revision()));
        assertEquals(5, repository.findOrCreate("trainer:alpha", "cedar_rangers").reputation());
        assertEquals(false, repository.replaceIfRevision(
                new FileCanonicalFactionReputationRepository.ReputationState("trainer:alpha", "cedar_rangers", 10, 2L),
                0L));
    }
}

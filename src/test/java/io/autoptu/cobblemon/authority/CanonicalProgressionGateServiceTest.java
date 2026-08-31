package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalProgressionGateServiceTest {
    @TempDir Path tempDir;

    @Test
    void deniesWithoutRequiredBadgeAndAllowsPersistedCanonicalBadge() {
        var repository = new FileCanonicalTrainerRecordRepository(tempDir);
        var service = new CanonicalProgressionGateService(new CanonicalTrainerRecordQueryService(repository));

        assertFalse(service.canPass("trainer-a", CanonicalProgressionGateCatalogue.CEDAR_BADGE_GATE_ID).allowed());

        var current = repository.findOrCreate("trainer-a");
        assertTrue(repository.replaceIfRevision(new FileCanonicalTrainerRecordRepository.TrainerRecord(
                current.playerId(),
                current.wins(),
                current.losses(),
                Set.of(CanonicalProgressionGateCatalogue.CEDAR_TRIAL_BADGE_ID),
                List.of(),
                current.revision() + 1),
                current.revision()));

        assertTrue(service.canPass("trainer-a", CanonicalProgressionGateCatalogue.CEDAR_BADGE_GATE_ID).allowed());
    }

    @Test
    void rejectsUnknownGateInsteadOfAcceptingClientSuppliedProgression() {
        var service = new CanonicalProgressionGateService(
                new CanonicalTrainerRecordQueryService(new FileCanonicalTrainerRecordRepository(tempDir)));
        assertThrows(IllegalArgumentException.class, () -> service.canPass("trainer-a", "invented_gate"));
    }
}

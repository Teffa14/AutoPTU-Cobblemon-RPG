package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalBadgeGateServiceTest {
    @Test
    void gateFailsClosedWithoutRequiredBadgeAndAllowsPersistedBadge() throws Exception {
        var root = Files.createTempDirectory("autoptu-badge-gate-test");
        var repository = new FileCanonicalTrainerRecordRepository(root);
        var service = new CanonicalBadgeGateService(new CanonicalTrainerRecordQueryService(repository));

        var locked = service.canPass("player-a", CanonicalBadgeGateService.CEDAR_LEAGUE_GATE_ID);
        assertFalse(locked.allowed());
        assertTrue(locked.requiredBadgeIds().contains(CanonicalBadgeGateService.CEDAR_GYM_BADGE_ID));

        var current = repository.findOrCreate("player-a");
        assertTrue(repository.replaceIfRevision(new FileCanonicalTrainerRecordRepository.TrainerRecord(
                current.playerId(), current.wins(), current.losses(),
                Set.of(CanonicalBadgeGateService.CEDAR_GYM_BADGE_ID), List.of(), current.revision() + 1),
                current.revision()));

        var reopened = new CanonicalBadgeGateService(new CanonicalTrainerRecordQueryService(
                new FileCanonicalTrainerRecordRepository(root)));
        assertTrue(reopened.canPass("player-a", CanonicalBadgeGateService.CEDAR_LEAGUE_GATE_ID).allowed());
        assertFalse(reopened.canPass("player-b", CanonicalBadgeGateService.CEDAR_LEAGUE_GATE_ID).allowed());
        assertFalse(reopened.canPass("player-a", "client-invented-gate").allowed());
    }
}

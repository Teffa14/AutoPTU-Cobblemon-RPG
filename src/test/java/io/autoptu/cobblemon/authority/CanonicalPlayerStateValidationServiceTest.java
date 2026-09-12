package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

final class CanonicalPlayerStateValidationServiceTest {
    private final CanonicalPlayerStateValidationService service = new CanonicalPlayerStateValidationService();

    @Test
    void acceptsStructurallyConsistentCanonicalState() {
        var report = service.validate(
                "player-1",
                trainer("player-1"),
                new CanonicalPartySummary(
                        "player-1",
                        List.of(new CanonicalPartySummary.Member(
                                1, "pokemon-1", "bulbasaur", 5, 18, 20, List.of(), 2)),
                        3),
                new CanonicalBagQueryService.BagSnapshot(
                        "player-1",
                        List.of(new CanonicalBagQueryService.BagEntry(
                                "item-1", "field_ration", 3, 3, 0, null, false, 1)),
                        3,
                        3,
                        0,
                        0),
                Optional.of(new FileCanonicalTrainerProgressionRepository.ProgressionState(
                        "player-1", 2, 125L, 4L)));

        assertTrue(report.valid());
        assertEquals(0, report.errorCount());
        assertEquals(0, report.warningCount());
        assertTrue(report.issues().isEmpty());
    }

    @Test
    void reportsCrossProjectionAndAggregateInconsistenciesWithoutPtuInference() {
        var report = service.validate(
                "player-1",
                trainer("wrong-trainer-owner"),
                new CanonicalPartySummary(
                        "wrong-party-owner",
                        List.of(
                                new CanonicalPartySummary.Member(
                                        1, "pokemon-1", "bulbasaur", 5, 18, 20, List.of(), 2),
                                new CanonicalPartySummary.Member(
                                        1, "pokemon-1", "bulbasaur", 5, 18, 20, List.of(), 2)),
                        3),
                new CanonicalBagQueryService.BagSnapshot(
                        "wrong-bag-owner",
                        List.of(
                                new CanonicalBagQueryService.BagEntry(
                                        "item-1", "field_ration", 3, 2, 1, "reservation-1", false, 1),
                                new CanonicalBagQueryService.BagEntry(
                                        "item-1", "field_ration", 2, 2, 0, null, false, 1)),
                        99,
                        99,
                        99,
                        99),
                Optional.of(new FileCanonicalTrainerProgressionRepository.ProgressionState(
                        "wrong-progression-owner", 2, 125L, 4L)));

        assertFalse(report.valid());
        assertEquals(11, report.errorCount());
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("TRAINER_OWNER_MISMATCH")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("PARTY_DUPLICATE_SLOT")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("PARTY_DUPLICATE_POKEMON")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("BAG_DUPLICATE_ITEM_INSTANCE")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("BAG_TOTAL_QUANTITY_MISMATCH")));
        assertTrue(report.issues().stream().anyMatch(issue -> issue.code().equals("PROGRESSION_OWNER_MISMATCH")));
    }

    @Test
    void missingProgressionIsVisibleButDoesNotInventAProgressionRuleFailure() {
        var report = service.validate(
                "player-1",
                trainer("player-1"),
                null,
                new CanonicalBagQueryService.BagSnapshot("player-1", List.of(), 0, 0, 0, 0),
                Optional.empty());

        assertTrue(report.valid());
        assertEquals(0, report.errorCount());
        assertEquals(1, report.warningCount());
        assertEquals("PROGRESSION_MISSING", report.issues().get(0).code());
    }

    private static CanonicalTrainerSummaryService.Summary trainer(String playerId) {
        return new CanonicalTrainerSummaryService.Summary(
                playerId,
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                0,
                0,
                null,
                "",
                1L);
    }
}

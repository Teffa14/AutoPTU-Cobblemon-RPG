package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FileTrainerActionUsageLedgerTest {
    @TempDir
    Path tempDirectory;

    @Test
    void dailyQuotaSurvivesRestartAndClockRollback() {
        CanonicalTrainerActionRule dailyTwice = rule("training-focus", TrainerActionFrequency.DAILY, 2);
        FileTrainerActionUsageLedger first = new FileTrainerActionUsageLedger(tempDirectory);

        assertEquals(TrainerActionUsageDecision.Status.RESERVED, first.reserve(attempt("op-1", dailyTwice, null, 12)).status());
        assertEquals(TrainerActionUsageDecision.Status.RESERVED, first.reserve(attempt("op-2", dailyTwice, null, 12)).status());
        assertEquals(TrainerActionUsageDecision.Status.LIMIT_REACHED, first.reserve(attempt("op-3", dailyTwice, null, 12)).status());

        FileTrainerActionUsageLedger restarted = new FileTrainerActionUsageLedger(tempDirectory);
        assertEquals(12L, restarted.highestObservedOverworldDay());
        TrainerActionUsageDecision rollbackAttempt = restarted.reserve(attempt("op-4", dailyTwice, null, 3));
        assertEquals(TrainerActionUsageDecision.Status.LIMIT_REACHED, rollbackAttempt.status());
        assertEquals("day:12", rollbackAttempt.windowId());
    }

    @Test
    void newObservedDayRefreshesDailyQuotaWithoutBankingSkippedDays() {
        CanonicalTrainerActionRule dailyOnce = rule("research", TrainerActionFrequency.DAILY, 1);
        FileTrainerActionUsageLedger ledger = new FileTrainerActionUsageLedger(tempDirectory);
        assertTrue(ledger.reserve(attempt("day-1", dailyOnce, null, 1)).allowed());
        assertEquals(TrainerActionUsageDecision.Status.LIMIT_REACHED, ledger.reserve(attempt("same-day", dailyOnce, null, 1)).status());

        TrainerActionUsageDecision jumped = ledger.reserve(attempt("day-9", dailyOnce, null, 9));
        assertEquals(TrainerActionUsageDecision.Status.RESERVED, jumped.status());
        assertEquals("day:9", jumped.windowId());
        assertEquals(TrainerActionUsageDecision.Status.LIMIT_REACHED, ledger.reserve(attempt("no-banked-use", dailyOnce, null, 9)).status());
    }

    @Test
    void canonicalSceneAndEncounterContextsOwnTheirOwnWindows() {
        FileTrainerActionUsageLedger ledger = new FileTrainerActionUsageLedger(tempDirectory);
        CanonicalTrainerActionRule scene = rule("scene-feature", TrainerActionFrequency.SCENE, 1);
        assertEquals(TrainerActionUsageDecision.Status.CONTEXT_REQUIRED, ledger.reserve(attempt("missing", scene, null, 2)).status());
        assertEquals(TrainerActionUsageDecision.Status.RESERVED, ledger.reserve(attempt("scene-a", scene, "scene-a", 2)).status());
        assertEquals(TrainerActionUsageDecision.Status.LIMIT_REACHED, ledger.reserve(attempt("scene-a-2", scene, "scene-a", 2)).status());
        assertEquals(TrainerActionUsageDecision.Status.RESERVED, ledger.reserve(attempt("scene-b", scene, "scene-b", 2)).status());

        CanonicalTrainerActionRule encounter = rule("encounter-feature", TrainerActionFrequency.ENCOUNTER, 1);
        assertEquals(TrainerActionUsageDecision.Status.RESERVED, ledger.reserve(attempt("enc-a", encounter, "encounter-a", 2)).status());
        assertEquals(TrainerActionUsageDecision.Status.RESERVED, ledger.reserve(attempt("enc-b", encounter, "encounter-b", 2)).status());
    }

    @Test
    void reservationIsIdempotentAndReleaseRefundsOnlyPendingUse() {
        FileTrainerActionUsageLedger ledger = new FileTrainerActionUsageLedger(tempDirectory);
        CanonicalTrainerActionRule dailyOnce = rule("daily-feature", TrainerActionFrequency.DAILY, 1);
        TrainerActionUsageDecision first = ledger.reserve(attempt("same-operation", dailyOnce, null, 4));
        TrainerActionUsageDecision retry = ledger.reserve(attempt("same-operation", dailyOnce, null, 4));
        assertEquals(TrainerActionUsageDecision.Status.ALREADY_RESERVED, retry.status());
        assertEquals(first.reservation().orElseThrow().reservationId(), retry.reservation().orElseThrow().reservationId());

        String reservationId = first.reservation().orElseThrow().reservationId();
        assertTrue(ledger.release(reservationId, "player-a"));
        assertFalse(ledger.findByOperationId("same-operation").isPresent());
        assertEquals(TrainerActionUsageDecision.Status.RESERVED, ledger.reserve(attempt("replacement", dailyOnce, null, 4)).status());
    }

    @Test
    void committedUseRemainsConsumedAndRetryDoesNotDoubleSpend() {
        FileTrainerActionUsageLedger ledger = new FileTrainerActionUsageLedger(tempDirectory);
        CanonicalTrainerActionRule dailyOnce = rule("daily-feature", TrainerActionFrequency.DAILY, 1);
        TrainerActionUsageDecision first = ledger.reserve(attempt("commit-op", dailyOnce, null, 7));
        String reservationId = first.reservation().orElseThrow().reservationId();
        assertTrue(ledger.commit(reservationId, "player-a"));
        assertTrue(ledger.commit(reservationId, "player-a"));
        assertFalse(ledger.release(reservationId, "player-a"));
        assertEquals(TrainerActionUsageDecision.Status.ALREADY_COMMITTED, ledger.reserve(attempt("commit-op", dailyOnce, null, 7)).status());
        assertEquals(TrainerActionUsageDecision.Status.LIMIT_REACHED, ledger.reserve(attempt("other-op", dailyOnce, null, 7)).status());
    }

    @Test
    void operationIdCannotBeReusedForDifferentCanonicalAction() {
        FileTrainerActionUsageLedger ledger = new FileTrainerActionUsageLedger(tempDirectory);
        assertTrue(ledger.reserve(attempt("operation", rule("feature-a", TrainerActionFrequency.DAILY, 1), null, 5)).allowed());
        assertEquals(
                TrainerActionUsageDecision.Status.OPERATION_CONFLICT,
                ledger.reserve(attempt("operation", rule("feature-b", TrainerActionFrequency.DAILY, 1), null, 5)).status()
        );
    }

    private static CanonicalTrainerActionRule rule(String actionId, TrainerActionFrequency frequency, int maxUses) {
        return new CanonicalTrainerActionRule(actionId, frequency, maxUses);
    }

    private static TrainerActionUsageAttempt attempt(
            String operationId,
            CanonicalTrainerActionRule rule,
            String contextId,
            long day
    ) {
        return new TrainerActionUsageAttempt(operationId, "player-a", rule, contextId, day, 1_000L);
    }
}

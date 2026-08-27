package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileTrainerPtuDailyActionReservationTest {
    @TempDir
    Path tempDir;

    @Test
    void pendingReservationCountsAgainstCapAndSurvivesRestart() {
        Path storage = tempDir.resolve("usage");
        FileTrainerPtuDailyActionLedger first = new FileTrainerPtuDailyActionLedger(storage);

        var reservation = first.tryReserve("player-1", "feature:daily-training", 1, 7, "op-1");
        assertEquals(FileTrainerPtuDailyActionLedger.ReservationStatus.RESERVED, reservation.status());
        assertEquals(0, reservation.usage().remaining());

        FileTrainerPtuDailyActionLedger restarted = new FileTrainerPtuDailyActionLedger(storage);
        var blocked = restarted.tryReserve("player-1", "feature:daily-training", 1, 7, "op-2");
        assertEquals(FileTrainerPtuDailyActionLedger.ReservationStatus.LIMIT_REACHED, blocked.status());
        assertEquals(1, blocked.usage().used());
    }

    @Test
    void retryingSameOperationIsIdempotent() {
        FileTrainerPtuDailyActionLedger ledger = new FileTrainerPtuDailyActionLedger(tempDir.resolve("usage"));

        var first = ledger.tryReserve("player-1", "feature:daily-training", 2, 4, "op-idempotent");
        var retry = ledger.tryReserve("player-1", "feature:daily-training", 2, 4, "op-idempotent");

        assertEquals(FileTrainerPtuDailyActionLedger.ReservationStatus.RESERVED, first.status());
        assertEquals(FileTrainerPtuDailyActionLedger.ReservationStatus.ALREADY_RESERVED, retry.status());
        assertEquals(1, retry.usage().used());
        assertEquals(1, retry.usage().remaining());
    }

    @Test
    void releaseRefundsOnlyPendingUse() {
        FileTrainerPtuDailyActionLedger ledger = new FileTrainerPtuDailyActionLedger(tempDir.resolve("usage"));
        assertTrue(ledger.tryReserve("player-1", "feature:daily-training", 1, 8, "op-release").allowed());

        assertTrue(ledger.releaseReservation("player-1", "op-release"));
        assertEquals(0, ledger.view("player-1", "feature:daily-training", 1, 8).used());
        assertFalse(ledger.releaseReservation("player-1", "op-release"));
        assertEquals(
                FileTrainerPtuDailyActionLedger.ReservationStatus.RESERVED,
                ledger.tryReserve("player-1", "feature:daily-training", 1, 8, "op-replacement").status()
        );
    }

    @Test
    void committedReservationCannotBeReleasedAndRetryDoesNotSpendAgain() {
        Path storage = tempDir.resolve("usage");
        FileTrainerPtuDailyActionLedger ledger = new FileTrainerPtuDailyActionLedger(storage);
        assertTrue(ledger.tryReserve("player-1", "feature:daily-training", 1, 11, "op-commit").allowed());
        assertTrue(ledger.commitReservation("player-1", "op-commit"));
        assertTrue(ledger.commitReservation("player-1", "op-commit"));
        assertFalse(ledger.releaseReservation("player-1", "op-commit"));

        FileTrainerPtuDailyActionLedger restarted = new FileTrainerPtuDailyActionLedger(storage);
        var retry = restarted.tryReserve("player-1", "feature:daily-training", 1, 11, "op-commit");
        assertEquals(FileTrainerPtuDailyActionLedger.ReservationStatus.ALREADY_COMMITTED, retry.status());
        assertEquals(1, retry.usage().used());
    }

    @Test
    void operationIdCannotBeReusedForAnotherCanonicalAction() {
        FileTrainerPtuDailyActionLedger ledger = new FileTrainerPtuDailyActionLedger(tempDir.resolve("usage"));
        assertTrue(ledger.tryReserve("player-1", "feature:a", 1, 3, "same-operation").allowed());

        var conflict = ledger.tryReserve("player-1", "feature:b", 1, 3, "same-operation");
        assertEquals(FileTrainerPtuDailyActionLedger.ReservationStatus.OPERATION_CONFLICT, conflict.status());
        assertEquals(0, conflict.usage().used());
    }

    @Test
    void releasingOldDayReservationCannotRefundNewDayUse() {
        FileTrainerPtuDailyActionLedger ledger = new FileTrainerPtuDailyActionLedger(tempDir.resolve("usage"));
        assertTrue(ledger.tryReserve("player-1", "feature:daily-training", 1, 5, "old-day").allowed());
        assertTrue(ledger.tryReserve("player-1", "feature:daily-training", 1, 6, "new-day").allowed());

        assertTrue(ledger.releaseReservation("player-1", "old-day"));
        var current = ledger.view("player-1", "feature:daily-training", 1, 6);
        assertEquals(1, current.used());
        assertEquals(0, current.remaining());
    }

    @Test
    void immediateConsumptionAndPendingReservationsShareOneCap() {
        FileTrainerPtuDailyActionLedger ledger = new FileTrainerPtuDailyActionLedger(tempDir.resolve("usage"));
        assertTrue(ledger.tryConsume("player-1", "feature:daily-training", 2, 9).consumed());
        assertTrue(ledger.tryReserve("player-1", "feature:daily-training", 2, 9, "op-pending").allowed());

        var blocked = ledger.tryReserve("player-1", "feature:daily-training", 2, 9, "op-blocked");
        assertEquals(FileTrainerPtuDailyActionLedger.ReservationStatus.LIMIT_REACHED, blocked.status());
        assertEquals(2, blocked.usage().used());
    }
}

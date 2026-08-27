package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FileTrainerPtuDailyActionLedgerTest {
    @TempDir
    Path tempDir;

    @Test
    void capsUsageWithinOneRpgDayAndResetsOnNextDay() {
        FileTrainerPtuDailyActionLedger ledger = new FileTrainerPtuDailyActionLedger(tempDir.resolve("usage"));

        var first = ledger.tryConsume("player-1", "feature:daily-training", 2, 7);
        var second = ledger.tryConsume("player-1", "feature:daily-training", 2, 7);
        var blocked = ledger.tryConsume("player-1", "feature:daily-training", 2, 7);

        assertTrue(first.consumed());
        assertTrue(second.consumed());
        assertFalse(blocked.consumed());
        assertEquals(0, blocked.usage().remaining());

        var nextDay = ledger.tryConsume("player-1", "feature:daily-training", 2, 8);
        assertTrue(nextDay.consumed());
        assertEquals(1, nextDay.usage().used());
        assertEquals(1, nextDay.usage().remaining());
    }

    @Test
    void restartDoesNotRestoreUses() {
        Path storage = tempDir.resolve("usage");
        FileTrainerPtuDailyActionLedger firstProcess = new FileTrainerPtuDailyActionLedger(storage);
        assertTrue(firstProcess.tryConsume("player-1", "feature:daily-training", 1, 12).consumed());

        FileTrainerPtuDailyActionLedger restarted = new FileTrainerPtuDailyActionLedger(storage);
        var result = restarted.tryConsume("player-1", "feature:daily-training", 1, 12);

        assertFalse(result.consumed());
        assertEquals(1, result.usage().used());
    }

    @Test
    void usageIsIsolatedByTrainerAndAction() {
        FileTrainerPtuDailyActionLedger ledger = new FileTrainerPtuDailyActionLedger(tempDir.resolve("usage"));
        assertTrue(ledger.tryConsume("player-a", "feature:a", 1, 2).consumed());

        assertTrue(ledger.tryConsume("player-a", "feature:b", 1, 2).consumed());
        assertTrue(ledger.tryConsume("player-b", "feature:a", 1, 2).consumed());
    }
}

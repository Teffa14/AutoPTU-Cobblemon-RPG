package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class FileMonotonicRpgDayClockTest {
    @TempDir
    Path tempDir;

    @Test
    void advancesOnlyWhenMinecraftWorldDayMovesForward() {
        FileMonotonicRpgDayClock clock = new FileMonotonicRpgDayClock(tempDir.resolve("rpg-day.properties"));

        assertEquals(0, clock.observeWorldTime(5_000));
        assertEquals(0, clock.observeWorldTime(23_999));
        assertEquals(1, clock.observeWorldTime(24_000));
        assertEquals(3, clock.observeWorldTime(72_000));
    }

    @Test
    void movingWorldTimeBackwardsCannotRestoreOrCreateDailyWindows() {
        FileMonotonicRpgDayClock clock = new FileMonotonicRpgDayClock(tempDir.resolve("rpg-day.properties"));

        assertEquals(0, clock.observeWorldTime(48_000));
        assertEquals(1, clock.observeWorldTime(72_000));
        assertEquals(1, clock.observeWorldTime(24_000));
        assertEquals(1, clock.observeWorldTime(71_999));
        assertEquals(2, clock.observeWorldTime(96_000));
    }

    @Test
    void restartKeepsTheSameRpgDayAndAcceptedWorldBoundary() {
        Path state = tempDir.resolve("rpg-day.properties");
        FileMonotonicRpgDayClock firstProcess = new FileMonotonicRpgDayClock(state);
        assertEquals(0, firstProcess.observeWorldTime(24_000));
        assertEquals(1, firstProcess.observeWorldTime(48_000));

        FileMonotonicRpgDayClock restarted = new FileMonotonicRpgDayClock(state);
        assertEquals(1, restarted.observeWorldTime(24_000));
        assertEquals(2, restarted.observeWorldTime(72_000));
    }
}

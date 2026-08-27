package io.autoptu.cobblemon.fabric.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

final class FabricRpgDayClockTest {
    @Test
    void mapsOverworldTicksToMinecraftDayWindows() {
        assertEquals(0L, FabricRpgDayClock.dayIndex(0L));
        assertEquals(0L, FabricRpgDayClock.dayIndex(23_999L));
        assertEquals(1L, FabricRpgDayClock.dayIndex(24_000L));
        assertEquals(25L, FabricRpgDayClock.dayIndex(600_123L));
    }

    @Test
    void negativeClockInputCannotCreateNegativeRpgDay() {
        assertEquals(0L, FabricRpgDayClock.dayIndex(-1L));
        assertEquals(0L, FabricRpgDayClock.dayIndex(-48_000L));
    }
}

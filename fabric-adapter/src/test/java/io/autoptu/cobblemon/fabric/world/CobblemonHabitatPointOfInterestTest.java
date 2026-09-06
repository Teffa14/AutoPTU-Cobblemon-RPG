package io.autoptu.cobblemon.fabric.world;

import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CobblemonHabitatPointOfInterestTest {
    @Test
    void choosesNearestPhysicalHabitatPointInsideAuthoredLeash() {
        var selected = CobblemonHabitatPointOfInterest.selectNearest(
                List.of(new BlockPos(6, 64, 0), new BlockPos(2, 64, 0)),
                0.5D, 64.0D, 0.5D,
                0.5D, 0.5D,
                12);

        assertEquals(new BlockPos(2, 64, 0), selected.orElseThrow());
    }

    @Test
    void rejectsPhysicalHabitatPointOutsideCanonicalLeash() {
        var selected = CobblemonHabitatPointOfInterest.selectNearest(
                List.of(new BlockPos(20, 64, 0)),
                19.5D, 64.0D, 0.5D,
                0.5D, 0.5D,
                12);

        assertTrue(selected.isEmpty());
    }

    @Test
    void verticalDistanceParticipatesInNearestPoiSelection() {
        var selected = CobblemonHabitatPointOfInterest.selectNearest(
                List.of(new BlockPos(2, 80, 0), new BlockPos(5, 64, 0)),
                0.5D, 64.0D, 0.5D,
                0.5D, 0.5D,
                12);

        assertEquals(new BlockPos(5, 64, 0), selected.orElseThrow());
    }

    @Test
    void noObservedHabitatBlockKeepsCanonicalFallbackAvailable() {
        var selected = CobblemonHabitatPointOfInterest.selectNearest(
                List.of(),
                0.5D, 64.0D, 0.5D,
                0.5D, 0.5D,
                12);

        assertTrue(selected.isEmpty());
    }
}

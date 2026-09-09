package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildHerdRegroupingPresentationRuntimeTest {
    @Test
    void marksOnlyObservedOffsetsOutsideAuthoredCohesion() {
        assertFalse(WildHerdRegroupingPresentationRuntime.isOutsideCohesion(0.0D, 0.0D, 6.0D));
        assertFalse(WildHerdRegroupingPresentationRuntime.isOutsideCohesion(3.0D, 4.0D, 5.0D));
        assertTrue(WildHerdRegroupingPresentationRuntime.isOutsideCohesion(3.1D, 4.0D, 5.0D));
        assertTrue(WildHerdRegroupingPresentationRuntime.isOutsideCohesion(-8.0D, 0.0D, 6.0D));
    }

    @Test
    void invalidMinecraftGeometryFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.isOutsideCohesion(Double.NaN, 0.0D, 6.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.isOutsideCohesion(0.0D, Double.POSITIVE_INFINITY, 6.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.isOutsideCohesion(0.0D, 0.0D, -1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildHerdRegroupingPresentationRuntime.isOutsideCohesion(0.0D, 0.0D, Double.NaN));
    }
}

package io.autoptu.cobblemon.fabric.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BattleShapeVisualLabRuntimeTest {
    @Test
    void aoeFootprintKeepsAuthoredInsideAndOutsideControlsVisuallySeparated() {
        double radius = BattleShapeVisualLabRuntime.aoeRadius();
        double rioluOffset = Math.hypot(0.0D, 2.0D);
        double eeveeOffset = Math.hypot(2.0D, -2.0D);

        assertTrue(rioluOffset < radius);
        assertTrue(eeveeOffset > radius);
    }

    @Test
    void blastConeWidensWithDistanceInsteadOfReadingLikeALine() {
        double near = BattleShapeVisualLabRuntime.blastHalfWidth(2.0D);
        double middle = BattleShapeVisualLabRuntime.blastHalfWidth(6.0D);
        double far = BattleShapeVisualLabRuntime.blastHalfWidth(10.0D);

        assertTrue(near < middle);
        assertTrue(middle < far);
        assertTrue(far > BattleShapeVisualLabRuntime.lineHalfWidth() * 3.0D);
        assertTrue(2.0D < BattleShapeVisualLabRuntime.blastHalfWidth(8.0D),
                "the authored second compact target must sit visibly inside the cone at x=8");
    }

    @Test
    void lineRemainsAConstantNarrowCorridorComparedWithAoe() {
        assertEquals(0.62D, BattleShapeVisualLabRuntime.lineHalfWidth(), 0.000001D);
        assertTrue(BattleShapeVisualLabRuntime.aoeRadius() > BattleShapeVisualLabRuntime.lineHalfWidth() * 3.0D);
    }

    @Test
    void invalidBlastDistanceFailsClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> BattleShapeVisualLabRuntime.blastHalfWidth(Double.NaN));
        assertThrows(IllegalArgumentException.class,
                () -> BattleShapeVisualLabRuntime.blastHalfWidth(-0.01D));
    }
}

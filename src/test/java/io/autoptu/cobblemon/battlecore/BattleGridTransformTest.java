package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleGridTransformTest {
    @Test
    void mapsGridToWorldWithoutOwningMovementRules() {
        BattleGridTransform transform = new BattleGridTransform(
                new WorldBlockCoordinate("minecraft:overworld", 100, 64, -20),
                HorizontalGridAxis.POSITIVE_X,
                HorizontalGridAxis.POSITIVE_Z
        );

        assertEquals(
                new WorldBlockCoordinate("minecraft:overworld", 104, 64, -17),
                transform.toWorld(new BattleGridCoordinate(4, 3))
        );
    }

    @Test
    void supportsRotatedAndMirroredArenaOrientation() {
        BattleGridTransform transform = new BattleGridTransform(
                new WorldBlockCoordinate("minecraft:overworld", 10, 70, 20),
                HorizontalGridAxis.NEGATIVE_Z,
                HorizontalGridAxis.POSITIVE_X
        );

        BattleGridCoordinate grid = new BattleGridCoordinate(3, -2);
        WorldBlockCoordinate world = transform.toWorld(grid);

        assertEquals(new WorldBlockCoordinate("minecraft:overworld", 8, 70, 17), world);
        assertEquals(grid, transform.toGrid(world));
    }

    @Test
    void rejectsNonOrthogonalGridBasis() {
        assertThrows(IllegalArgumentException.class, () -> new BattleGridTransform(
                new WorldBlockCoordinate("minecraft:overworld", 0, 64, 0),
                HorizontalGridAxis.POSITIVE_X,
                HorizontalGridAxis.NEGATIVE_X
        ));
    }

    @Test
    void inverseMappingRejectsDifferentDimensionAndElevation() {
        BattleGridTransform transform = new BattleGridTransform(
                new WorldBlockCoordinate("minecraft:overworld", 0, 64, 0),
                HorizontalGridAxis.POSITIVE_X,
                HorizontalGridAxis.POSITIVE_Z
        );

        assertThrows(IllegalArgumentException.class, () -> transform.toGrid(
                new WorldBlockCoordinate("minecraft:the_nether", 2, 64, 3)
        ));
        assertThrows(IllegalArgumentException.class, () -> transform.toGrid(
                new WorldBlockCoordinate("minecraft:overworld", 2, 65, 3)
        ));
    }

    @Test
    void overflowFailsClosed() {
        BattleGridTransform transform = new BattleGridTransform(
                new WorldBlockCoordinate("minecraft:overworld", Integer.MAX_VALUE, 64, 0),
                HorizontalGridAxis.POSITIVE_X,
                HorizontalGridAxis.POSITIVE_Z
        );

        assertThrows(ArithmeticException.class, () -> transform.toWorld(new BattleGridCoordinate(1, 0)));
    }
}

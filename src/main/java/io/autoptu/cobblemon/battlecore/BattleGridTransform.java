package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Integer transform between the authoritative 2D battle grid and a horizontal world block plane.
 *
 * This class only maps coordinates. It never decides movement legality, collision, terrain cost,
 * forced movement, targeting, or any other PTU rule.
 */
public record BattleGridTransform(
        WorldBlockCoordinate origin,
        HorizontalGridAxis gridX,
        HorizontalGridAxis gridY
) {
    public BattleGridTransform {
        origin = Objects.requireNonNull(origin, "origin");
        gridX = Objects.requireNonNull(gridX, "gridX");
        gridY = Objects.requireNonNull(gridY, "gridY");
        if (!gridX.isPerpendicularTo(gridY)) {
            throw new IllegalArgumentException("grid axes must be perpendicular");
        }
    }

    public WorldBlockCoordinate toWorld(BattleGridCoordinate grid) {
        Objects.requireNonNull(grid, "grid");
        int worldX = Math.addExact(
                origin.x(),
                Math.addExact(Math.multiplyExact(grid.x(), gridX.dx()), Math.multiplyExact(grid.y(), gridY.dx()))
        );
        int worldZ = Math.addExact(
                origin.z(),
                Math.addExact(Math.multiplyExact(grid.x(), gridX.dz()), Math.multiplyExact(grid.y(), gridY.dz()))
        );
        return new WorldBlockCoordinate(origin.dimensionId(), worldX, origin.y(), worldZ);
    }

    public BattleGridCoordinate toGrid(WorldBlockCoordinate world) {
        Objects.requireNonNull(world, "world");
        if (!origin.dimensionId().equals(world.dimensionId())) {
            throw new IllegalArgumentException("world coordinate belongs to a different dimension");
        }
        if (origin.y() != world.y()) {
            throw new IllegalArgumentException("world coordinate is outside the battle grid plane");
        }

        int deltaX = Math.subtractExact(world.x(), origin.x());
        int deltaZ = Math.subtractExact(world.z(), origin.z());
        int gridCoordinateX = Math.addExact(
                Math.multiplyExact(deltaX, gridX.dx()),
                Math.multiplyExact(deltaZ, gridX.dz())
        );
        int gridCoordinateY = Math.addExact(
                Math.multiplyExact(deltaX, gridY.dx()),
                Math.multiplyExact(deltaZ, gridY.dz())
        );
        return new BattleGridCoordinate(gridCoordinateX, gridCoordinateY);
    }
}

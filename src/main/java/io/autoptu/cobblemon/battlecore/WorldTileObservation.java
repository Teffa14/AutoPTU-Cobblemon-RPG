package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Immutable adapter-observed world facts for one authoritative battle-grid coordinate.
 *
 * These values are observations only. They do not classify PTU terrain, determine movement cost,
 * establish traversability, create hazards, or decide targeting/reaction legality.
 */
public record WorldTileObservation(
        BattleGridCoordinate gridCoordinate,
        WorldBlockCoordinate gridAnchor,
        int observedSurfaceY,
        String blockStateId,
        String fluidStateId,
        boolean collisionShapePresent,
        boolean airAtAnchor,
        boolean replaceableAtAnchor
) {
    public WorldTileObservation {
        gridCoordinate = Objects.requireNonNull(gridCoordinate, "gridCoordinate");
        gridAnchor = Objects.requireNonNull(gridAnchor, "gridAnchor");
        if (blockStateId == null || blockStateId.isBlank()) {
            throw new IllegalArgumentException("blockStateId is required");
        }
        blockStateId = blockStateId.strip();
        fluidStateId = fluidStateId == null ? "" : fluidStateId.strip();
    }

    public boolean hasFluidObservation() {
        return !fluidStateId.isEmpty();
    }
}

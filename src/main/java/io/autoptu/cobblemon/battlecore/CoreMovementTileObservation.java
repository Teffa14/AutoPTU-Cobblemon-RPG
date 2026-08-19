package io.autoptu.cobblemon.battlecore;

import java.util.Objects;

/**
 * Adapter-neutral physical observations for one battle-grid tile before PTU movement semantics.
 *
 * This record deliberately omits Minecraft block/fluid identifiers and does not assign PTU
 * terrain classes, movement costs, traversability, hazards, reactions, or forced-movement rules.
 * Those meanings remain authoritative upstream concerns.
 */
public record CoreMovementTileObservation(
        BattleGridCoordinate gridCoordinate,
        int observedSurfaceY,
        boolean collisionShapePresent,
        boolean airAtAnchor,
        boolean fluidPresent,
        boolean replaceableAtAnchor
) {
    public CoreMovementTileObservation {
        gridCoordinate = Objects.requireNonNull(gridCoordinate, "gridCoordinate");
    }
}

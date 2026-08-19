package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable core-facing observation package derived from a frozen battlefield world snapshot.
 *
 * The package intentionally stops before AutoPTU-Java MovementGrid semantics. It carries only
 * coordinate identity and raw physical observations that can remain stable across adapters.
 * PTU terrain classification, terrain cost, blockers, Swim/Sky/Overland legality, pathfinding,
 * hazards, reactions and forced movement must be supplied or resolved by authoritative core
 * contracts when those mappings are explicitly available.
 */
public record BattlefieldMovementObservationInput(
        String reservationId,
        BattleArenaSnapshot arena,
        Map<BattleGridCoordinate, CoreMovementTileObservation> tilesByCoordinate
) {
    public BattlefieldMovementObservationInput {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        arena = Objects.requireNonNull(arena, "arena");
        tilesByCoordinate = Map.copyOf(Objects.requireNonNull(tilesByCoordinate, "tilesByCoordinate"));
        if (tilesByCoordinate.isEmpty()) {
            throw new IllegalArgumentException("at least one movement observation is required");
        }
        for (Map.Entry<BattleGridCoordinate, CoreMovementTileObservation> entry : tilesByCoordinate.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "movement observation coordinate");
            CoreMovementTileObservation observation = Objects.requireNonNull(entry.getValue(), "movement observation");
            if (!entry.getKey().equals(observation.gridCoordinate())) {
                throw new IllegalArgumentException("movement observation key must match embedded grid coordinate");
            }
        }
    }

    public static BattlefieldMovementObservationInput from(BattlefieldWorldSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        LinkedHashMap<BattleGridCoordinate, CoreMovementTileObservation> observations = new LinkedHashMap<>();
        for (WorldTileObservation tile : snapshot.tiles()) {
            CoreMovementTileObservation observation = new CoreMovementTileObservation(
                    tile.gridCoordinate(),
                    tile.observedSurfaceY(),
                    tile.collisionShapePresent(),
                    tile.airAtAnchor(),
                    tile.hasFluidObservation(),
                    tile.replaceableAtAnchor()
            );
            CoreMovementTileObservation previous = observations.put(tile.gridCoordinate(), observation);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate movement observation coordinate");
            }
        }
        return new BattlefieldMovementObservationInput(snapshot.reservationId(), snapshot.arena(), observations);
    }

    public CoreMovementTileObservation tile(BattleGridCoordinate coordinate) {
        CoreMovementTileObservation observation = tilesByCoordinate.get(Objects.requireNonNull(coordinate, "coordinate"));
        if (observation == null) {
            throw new IllegalArgumentException("no movement observation for coordinate " + coordinate);
        }
        return observation;
    }
}

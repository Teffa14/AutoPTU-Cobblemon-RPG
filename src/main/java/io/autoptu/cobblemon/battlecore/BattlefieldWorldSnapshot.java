package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Frozen adapter-neutral world observations bound to one server-owned battle reservation.
 *
 * The snapshot preserves world facts and coordinate identity only. AutoPTU-Java remains
 * responsible for interpreting those facts into PTU terrain, movement, targeting, hazards,
 * zones, reactions, forced movement, and battle outcomes.
 */
public record BattlefieldWorldSnapshot(
        String reservationId,
        BattleArenaSnapshot arena,
        List<WorldTileObservation> tiles
) {
    public BattlefieldWorldSnapshot {
        if (reservationId == null || reservationId.isBlank()) {
            throw new IllegalArgumentException("reservationId is required");
        }
        reservationId = reservationId.strip();
        arena = Objects.requireNonNull(arena, "arena");
        tiles = tiles == null ? List.of() : List.copyOf(tiles);
        if (tiles.isEmpty()) {
            throw new IllegalArgumentException("at least one world tile observation is required");
        }

        BattleGridTransform transform = BattleGridTransform.from(arena);
        Set<BattleGridCoordinate> gridCoordinates = new HashSet<>();
        Set<WorldBlockCoordinate> gridAnchors = new HashSet<>();
        for (WorldTileObservation tile : tiles) {
            Objects.requireNonNull(tile, "world tile observation");
            if (!gridCoordinates.add(tile.gridCoordinate())) {
                throw new IllegalArgumentException("duplicate battle-grid coordinate observation");
            }
            if (!gridAnchors.add(tile.gridAnchor())) {
                throw new IllegalArgumentException("duplicate world grid anchor observation");
            }
            WorldBlockCoordinate expectedAnchor = transform.toWorld(tile.gridCoordinate());
            if (!expectedAnchor.equals(tile.gridAnchor())) {
                throw new IllegalArgumentException("world tile observation does not match frozen arena transform");
            }
        }
    }

    public static BattlefieldWorldSnapshot from(
            BattleAuthoritySnapshot battle,
            List<WorldTileObservation> tiles
    ) {
        Objects.requireNonNull(battle, "battle");
        if (battle.arena() == null) {
            throw new IllegalArgumentException("battle must have a frozen arena before world observations are captured");
        }
        return new BattlefieldWorldSnapshot(battle.reservationId(), battle.arena(), tiles);
    }
}

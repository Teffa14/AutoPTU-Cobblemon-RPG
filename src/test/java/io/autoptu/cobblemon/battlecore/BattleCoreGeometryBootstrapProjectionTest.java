package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreGeometryBootstrapProjectionTest {
    @Test
    void bindsCanonicalSizeLabelsToExactAuthoritativeRoster() {
        BattleCoreMovementBootstrapProjection movement = movementBootstrap();
        BattleCoreGeometryBootstrapProjection geometry = new BattleCoreGeometryBootstrapProjection(
                "reservation-1",
                movement,
                Map.of(
                        "p1", new BattleCombatantGeometryProjection("p1", "Large"),
                        "p2", new BattleCombatantGeometryProjection("p2", "Medium")
                )
        );

        assertEquals("Large", geometry.geometryByCombatant().get("p1").sizeLabel());
        assertEquals(Set.of("p1", "p2"), geometry.geometryByCombatant().keySet());
    }

    @Test
    void rejectsIncompleteOrInjectedGeometry() {
        BattleCoreMovementBootstrapProjection movement = movementBootstrap();
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreGeometryBootstrapProjection(
                "reservation-1", movement,
                Map.of("p1", new BattleCombatantGeometryProjection("p1", "Large"))
        ));
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreGeometryBootstrapProjection(
                "reservation-1", movement,
                Map.of(
                        "p1", new BattleCombatantGeometryProjection("p1", "Large"),
                        "p2", new BattleCombatantGeometryProjection("p2", "Medium"),
                        "injected", new BattleCombatantGeometryProjection("injected", "Gigantic")
                )
        ));
    }

    @Test
    void rejectsReservationAndEmbeddedIdMismatch() {
        BattleCoreMovementBootstrapProjection movement = movementBootstrap();
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreGeometryBootstrapProjection(
                "other", movement,
                Map.of(
                        "p1", new BattleCombatantGeometryProjection("p1", "Large"),
                        "p2", new BattleCombatantGeometryProjection("p2", "Medium")
                )
        ));
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreGeometryBootstrapProjection(
                "reservation-1", movement,
                Map.of(
                        "p1", new BattleCombatantGeometryProjection("wrong", "Large"),
                        "p2", new BattleCombatantGeometryProjection("p2", "Medium")
                )
        ));
    }

    @Test
    void geometryMapIsDefensivelyCopiedAndImmutable() {
        BattleCoreMovementBootstrapProjection movement = movementBootstrap();
        LinkedHashMap<String, BattleCombatantGeometryProjection> source = new LinkedHashMap<>();
        source.put("p1", new BattleCombatantGeometryProjection("p1", "Large"));
        source.put("p2", new BattleCombatantGeometryProjection("p2", "Medium"));
        BattleCoreGeometryBootstrapProjection geometry = new BattleCoreGeometryBootstrapProjection(
                "reservation-1", movement, source);
        source.clear();

        assertEquals(2, geometry.geometryByCombatant().size());
        assertThrows(UnsupportedOperationException.class, () -> geometry.geometryByCombatant().clear());
    }

    private static BattleCoreMovementBootstrapProjection movementBootstrap() {
        BattleCoreBootstrapProjection combat = new BattleCoreBootstrapProjection(
                "reservation-1", 42L, Set.of("p1", "p2"), Map.of());
        BattleInitialPlacementSnapshot placement = new BattleInitialPlacementSnapshot(
                "reservation-1",
                Map.of(
                        "p1", new BattleCombatantInitialPlacement("p1", new BattleGridCoordinate(1, 1)),
                        "p2", new BattleCombatantInitialPlacement("p2", new BattleGridCoordinate(4, 4))
                )
        );
        BattleCorePlacedBootstrapProjection placed = new BattleCorePlacedBootstrapProjection(
                "reservation-1", combat, placement);
        return new BattleCoreMovementBootstrapProjection(
                "reservation-1",
                placed,
                Map.of(
                        "p1", new BattleCombatantBaseMovementProjection("p1", 6, 2, 0, 2, 1),
                        "p2", new BattleCombatantBaseMovementProjection("p2", 5, 0, 3, 1, 1)
                )
        );
    }
}

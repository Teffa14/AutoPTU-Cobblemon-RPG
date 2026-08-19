package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleCoreTraitsBootstrapProjectionTest {
    @Test
    void bindsCanonicalTypesAndAbilitiesToExactGeometryRoster() {
        BattleCoreTraitsBootstrapProjection traits = new BattleCoreTraitsBootstrapProjection(
                "reservation-1",
                geometryBootstrap(),
                Map.of(
                        "p1", new BattleCombatantTraitsProjection("p1", List.of("Fire", "Flying"), List.of("Blaze")),
                        "p2", new BattleCombatantTraitsProjection("p2", List.of("Water"), List.of())
                )
        );

        assertEquals(List.of("Fire", "Flying"), traits.traitsByCombatant().get("p1").types());
        assertEquals(List.of("Blaze"), traits.traitsByCombatant().get("p1").abilities());
        assertEquals(Set.of("p1", "p2"), traits.traitsByCombatant().keySet());
    }

    @Test
    void rejectsIncompleteInjectedOrMismatchedTraits() {
        BattleCoreGeometryBootstrapProjection geometry = geometryBootstrap();
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreTraitsBootstrapProjection(
                "reservation-1", geometry,
                Map.of("p1", new BattleCombatantTraitsProjection("p1", List.of("Fire"), List.of()))));
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreTraitsBootstrapProjection(
                "reservation-1", geometry,
                Map.of(
                        "p1", new BattleCombatantTraitsProjection("wrong", List.of("Fire"), List.of()),
                        "p2", new BattleCombatantTraitsProjection("p2", List.of("Water"), List.of()))));
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreTraitsBootstrapProjection(
                "other", geometry,
                Map.of(
                        "p1", new BattleCombatantTraitsProjection("p1", List.of("Fire"), List.of()),
                        "p2", new BattleCombatantTraitsProjection("p2", List.of("Water"), List.of()))));
    }

    @Test
    void traitsMapIsDefensivelyCopiedAndImmutable() {
        LinkedHashMap<String, BattleCombatantTraitsProjection> source = new LinkedHashMap<>();
        source.put("p1", new BattleCombatantTraitsProjection("p1", List.of("Fire"), List.of("Blaze")));
        source.put("p2", new BattleCombatantTraitsProjection("p2", List.of("Water"), List.of()));
        BattleCoreTraitsBootstrapProjection traits = new BattleCoreTraitsBootstrapProjection(
                "reservation-1", geometryBootstrap(), source);
        source.clear();

        assertEquals(2, traits.traitsByCombatant().size());
        assertThrows(UnsupportedOperationException.class, () -> traits.traitsByCombatant().clear());
    }

    @Test
    void compatibilityAllowsIdentityTransportWithoutClaimingCompleteAbilities() {
        IntegrationFeatureCompatibility.Requirement snapshot = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_BATTLE_TRAITS_SNAPSHOT);
        IntegrationFeatureCompatibility.Requirement bootstrap = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_BATTLE_TRAITS_BOOTSTRAP);

        assertFalse(snapshot.hasBlockingDependency());
        assertFalse(bootstrap.hasBlockingDependency());
        assertTrue(bootstrap.capabilities().contains(UpstreamCompatibilityMatrix.Capability.ABILITIES));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                UpstreamCompatibilityMatrix.entry(UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
    }

    private static BattleCoreGeometryBootstrapProjection geometryBootstrap() {
        BattleCoreBootstrapProjection combat = new BattleCoreBootstrapProjection(
                "reservation-1", 42L, Set.of("p1", "p2"), Map.of());
        BattleInitialPlacementSnapshot placement = new BattleInitialPlacementSnapshot(
                "reservation-1",
                Map.of(
                        "p1", new BattleCombatantInitialPlacement("p1", new BattleGridCoordinate(1, 1)),
                        "p2", new BattleCombatantInitialPlacement("p2", new BattleGridCoordinate(4, 4))
                ));
        BattleCorePlacedBootstrapProjection placed = new BattleCorePlacedBootstrapProjection(
                "reservation-1", combat, placement);
        BattleCoreMovementBootstrapProjection movement = new BattleCoreMovementBootstrapProjection(
                "reservation-1",
                placed,
                Map.of(
                        "p1", new BattleCombatantBaseMovementProjection("p1", 6, 2, 0, 2, 1),
                        "p2", new BattleCombatantBaseMovementProjection("p2", 5, 0, 3, 1, 1)
                ));
        return new BattleCoreGeometryBootstrapProjection(
                "reservation-1",
                movement,
                Map.of(
                        "p1", new BattleCombatantGeometryProjection("p1", "Large"),
                        "p2", new BattleCombatantGeometryProjection("p2", "Medium")
                ));
    }
}

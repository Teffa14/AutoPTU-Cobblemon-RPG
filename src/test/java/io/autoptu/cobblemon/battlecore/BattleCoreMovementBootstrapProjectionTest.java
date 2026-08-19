package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCoreMovementBootstrapProjectionTest {
    @Test
    void bindsCanonicalBaseMovementToPlacedAuthoritativeRoster() {
        BattleAuthoritySnapshot battle = battleWithMovement();
        BattleInitialPlacementSnapshot placement = BattleInitialPlacementSnapshot.from(
                battle,
                Map.of(
                        "pokemon-fire", new BattleGridCoordinate(2, 3),
                        "pokemon-water", new BattleGridCoordinate(6, 3)
                )
        );

        BattleCoreMovementBootstrapProjection projection = BattleCoreMovementBootstrapProjection.from(battle, placement);

        assertEquals("battle-movement", projection.reservationId());
        assertEquals(Set.of("pokemon-fire", "pokemon-water"), projection.baseMovementByCombatant().keySet());
        assertEquals(5, projection.baseMovementByCombatant().get("pokemon-fire").overland());
        assertEquals(0, projection.baseMovementByCombatant().get("pokemon-fire").swim());
        assertEquals(6, projection.baseMovementByCombatant().get("pokemon-fire").sky());
        assertEquals(2, projection.baseMovementByCombatant().get("pokemon-fire").longJump());
        assertEquals(1, projection.baseMovementByCombatant().get("pokemon-fire").highJump());
        assertEquals(4, projection.baseMovementByCombatant().get("pokemon-water").overland());
        assertEquals(6, projection.baseMovementByCombatant().get("pokemon-water").swim());
        assertEquals(
                new BattleGridCoordinate(2, 3),
                projection.placedBootstrap().initialPlacement().placementsByCombatant().get("pokemon-fire").anchor()
        );
        assertThrows(
                UnsupportedOperationException.class,
                () -> projection.baseMovementByCombatant().put(
                        "client-injected",
                        new BattleCombatantBaseMovementProjection("client-injected", 99, 99, 99, 99, 99)
                )
        );
    }

    @Test
    void rejectsBattleReadyBootstrapWhenCanonicalBaseMovementIsMissing() {
        BattlePokemonSnapshot legacy = new BattlePokemonSnapshot(
                "pokemon-legacy",
                "player-1",
                "eevee",
                12,
                Set.of("Overland 5"),
                Set.of(),
                new CanonicalCombatStats(20, 20, 20, 20, 20),
                new CanonicalHealth(30, 30),
                new CanonicalMoveLoadout(List.of("Tackle")),
                null,
                3
        );
        BattleAuthoritySnapshot battle = new BattleAuthoritySnapshot(
                "battle-missing-movement",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 1),
                List.of(legacy),
                List.of(),
                41L,
                arena()
        );
        BattleInitialPlacementSnapshot placement = BattleInitialPlacementSnapshot.from(
                battle,
                Map.of("pokemon-legacy", new BattleGridCoordinate(1, 1))
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BattleCoreMovementBootstrapProjection.from(battle, placement)
        );
        assertEquals("canonical base movement is required for combatant: pokemon-legacy", error.getMessage());
    }

    @Test
    void rejectsInjectedOrMismatchedBaseMovementState() {
        BattleAuthoritySnapshot battle = battleWithMovement();
        BattleInitialPlacementSnapshot placement = BattleInitialPlacementSnapshot.from(
                battle,
                Map.of(
                        "pokemon-fire", new BattleGridCoordinate(2, 3),
                        "pokemon-water", new BattleGridCoordinate(6, 3)
                )
        );
        BattleCorePlacedBootstrapProjection placed = BattleCorePlacedBootstrapProjection.from(battle, placement);

        IllegalArgumentException incomplete = assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCoreMovementBootstrapProjection(
                        battle.reservationId(),
                        placed,
                        Map.of("pokemon-fire", new BattleCombatantBaseMovementProjection("pokemon-fire", 5, 0, 6, 2, 1))
                )
        );
        assertEquals("canonical base movement must exactly cover the bootstrapped combatant roster", incomplete.getMessage());

        IllegalArgumentException mismatch = assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCoreMovementBootstrapProjection(
                        battle.reservationId(),
                        placed,
                        Map.of(
                                "pokemon-fire", new BattleCombatantBaseMovementProjection("pokemon-other", 5, 0, 6, 2, 1),
                                "pokemon-water", new BattleCombatantBaseMovementProjection("pokemon-water", 4, 6, 0, 1, 1)
                        )
                )
        );
        assertEquals("base movement map key must match embedded combatantId", mismatch.getMessage());
    }

    @Test
    void defensivelyCopiesBaseMovementMap() {
        BattleAuthoritySnapshot battle = battleWithMovement();
        BattleInitialPlacementSnapshot placement = BattleInitialPlacementSnapshot.from(
                battle,
                Map.of(
                        "pokemon-fire", new BattleGridCoordinate(2, 3),
                        "pokemon-water", new BattleGridCoordinate(6, 3)
                )
        );
        BattleCorePlacedBootstrapProjection placed = BattleCorePlacedBootstrapProjection.from(battle, placement);
        LinkedHashMap<String, BattleCombatantBaseMovementProjection> mutable = new LinkedHashMap<>();
        mutable.put("pokemon-fire", new BattleCombatantBaseMovementProjection("pokemon-fire", 5, 0, 6, 2, 1));
        mutable.put("pokemon-water", new BattleCombatantBaseMovementProjection("pokemon-water", 4, 6, 0, 1, 1));

        BattleCoreMovementBootstrapProjection projection = new BattleCoreMovementBootstrapProjection(
                battle.reservationId(), placed, mutable);
        mutable.clear();

        assertEquals(Set.of("pokemon-fire", "pokemon-water"), projection.baseMovementByCombatant().keySet());
    }

    @Test
    void compatibilityMatrixKeepsBaseMovementBootstrapOnVerifiedCoreMovementOnly() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.CANONICAL_BASE_MOVEMENT_BOOTSTRAP
        );
        assertEquals(
                Set.of(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY),
                requirement.capabilities()
        );
        assertFalse(requirement.hasBlockingDependency());
    }

    private static BattleAuthoritySnapshot battleWithMovement() {
        return new BattleAuthoritySnapshot(
                "battle-movement",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of("Ace Trainer"), Map.of("Command", 4), 9),
                List.of(
                        new BattlePokemonSnapshot(
                                "pokemon-fire",
                                "player-1",
                                "charizard",
                                30,
                                Set.of("Overland 5", "Sky 6"),
                                Set.of(),
                                new CanonicalCombatStats(70, 60, 90, 70, 85),
                                new CanonicalHealth(70, 70),
                                new CanonicalMoveLoadout(List.of("Ember")),
                                new CanonicalBaseMovement(5, 0, 6, 2, 1),
                                null,
                                4
                        ),
                        new BattlePokemonSnapshot(
                                "pokemon-water",
                                "player-1",
                                "wartortle",
                                25,
                                Set.of("Overland 4", "Swim 6"),
                                Set.of(),
                                new CanonicalCombatStats(55, 70, 60, 70, 50),
                                new CanonicalHealth(60, 60),
                                new CanonicalMoveLoadout(List.of("Water Gun")),
                                new CanonicalBaseMovement(4, 6, 0, 1, 1),
                                null,
                                5
                        )
                ),
                List.of(),
                987L,
                arena()
        );
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 100, 64, 200, 1, 0, 0, 1);
    }
}

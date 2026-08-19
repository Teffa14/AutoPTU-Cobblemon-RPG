package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.BattleAuthoritySnapshot;
import io.autoptu.cobblemon.authority.BattlePokemonSnapshot;
import io.autoptu.cobblemon.authority.BattleTrainerSnapshot;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCorePlacedBootstrapProjectionTest {
    @Test
    void bindsCanonicalBootstrapAndInitialPlacementToSameReservation() {
        BattleAuthoritySnapshot battle = battle("battle-placed");
        BattleInitialPlacementSnapshot placement = BattleInitialPlacementSnapshot.from(
                battle,
                Map.of(
                        "pokemon-a", new BattleGridCoordinate(1, 2),
                        "pokemon-b", new BattleGridCoordinate(4, 2)
                )
        );

        BattleCorePlacedBootstrapProjection projection = BattleCorePlacedBootstrapProjection.from(battle, placement);

        assertEquals("battle-placed", projection.reservationId());
        assertEquals(Set.of("pokemon-a", "pokemon-b"), projection.combatState().combatantIds());
        assertEquals(new BattleGridCoordinate(1, 2),
                projection.initialPlacement().placementsByCombatant().get("pokemon-a").anchor());
        assertEquals(new BattleGridCoordinate(4, 2),
                projection.initialPlacement().placementsByCombatant().get("pokemon-b").anchor());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.INITIAL_COMBATANT_PLACEMENT).hasBlockingDependency());
    }

    @Test
    void rejectsPlacementFromDifferentReservation() {
        BattleAuthoritySnapshot battle = battle("battle-a");
        BattleAuthoritySnapshot other = battle("battle-b");
        BattleInitialPlacementSnapshot placement = BattleInitialPlacementSnapshot.from(
                other,
                Map.of(
                        "pokemon-a", new BattleGridCoordinate(1, 2),
                        "pokemon-b", new BattleGridCoordinate(4, 2)
                )
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> BattleCorePlacedBootstrapProjection.from(battle, placement)
        );
        assertEquals("initial placement belongs to a different battle reservation", error.getMessage());
    }

    @Test
    void rejectsPlacementThatDoesNotCoverBootstrappedRoster() {
        BattleCoreBootstrapProjection bootstrap = BattleCoreBootstrapProjection.from(battle("battle-roster"));
        BattleInitialPlacementSnapshot forged = new BattleInitialPlacementSnapshot(
                "battle-roster",
                Map.of("pokemon-a", new BattleCombatantInitialPlacement(
                        "pokemon-a", new BattleGridCoordinate(0, 0)))
        );

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> new BattleCorePlacedBootstrapProjection("battle-roster", bootstrap, forged)
        );
        assertEquals("initial placements must exactly cover the bootstrapped combatant roster", error.getMessage());
    }

    private static BattleAuthoritySnapshot battle(String reservationId) {
        return new BattleAuthoritySnapshot(
                reservationId,
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of(), Map.of(), 3),
                List.of(
                        pokemon("pokemon-a", "Tackle"),
                        pokemon("pokemon-b", "Water Gun")
                ),
                List.of(),
                991L,
                new BattleArenaSnapshot("minecraft:overworld", 100, 70, 200, 1, 0, 0, 1)
        );
    }

    private static BattlePokemonSnapshot pokemon(String id, String moveId) {
        return new BattlePokemonSnapshot(
                id,
                "player-1",
                "cobblemon:test",
                10,
                Set.of("Overland 5"),
                Set.of(),
                new CanonicalCombatStats(10, 10, 10, 10, 10),
                new CanonicalHealth(20, 20),
                new CanonicalMoveLoadout(List.of(moveId)),
                null,
                1
        );
    }
}

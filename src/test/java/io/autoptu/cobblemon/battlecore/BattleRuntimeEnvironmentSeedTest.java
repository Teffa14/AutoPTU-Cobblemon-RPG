package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.CanonicalStatusEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleRuntimeEnvironmentSeedTest {
    @Test
    void bindsTrustedEnvironmentToExactReservationRosterAndTeams() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");
        BattleRuntimeEnvironmentSeed seed = new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, " Hail ", " Electric Terrain ", Set.of("team-1"), Map.of("mon-1", true));

        assertEquals("Hail", seed.weather());
        assertEquals("Electric Terrain", seed.terrainName());
        assertEquals(Set.of("team-1"), seed.tailwindTeams());
        assertEquals(Map.of("mon-1", true), seed.groundedByCombatant());
        assertThrows(UnsupportedOperationException.class, () -> seed.groundedByCombatant().clear());
    }

    @Test
    void rejectsMissingInjectedOrForeignEnvironmentAuthority() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");

        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-2", runtime, "Hail", "", Set.of(), Map.of("mon-1", true)));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "Hail", "", Set.of(), Map.of("other", true)));
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "Hail", "", Set.of("forged-team"), Map.of("mon-1", true)));
    }

    @Test
    void requiresExplicitGroundedStateForEveryReservedCombatant() {
        BattleRuntimePreparationEnvelope runtime = runtimePreparation("battle-1");
        assertThrows(IllegalArgumentException.class, () -> new BattleRuntimeEnvironmentSeed(
                "battle-1", runtime, "", "", Set.of(), Map.of()));
    }

    private static BattleRuntimePreparationEnvelope runtimePreparation(String reservationId) {
        String id = "mon-1";
        RuntimeCombatantMaterializationInput input = new RuntimeCombatantMaterializationInput(
                id,
                new BattleCombatantInitialPlacement(id, new BattleGridCoordinate(2, 3)),
                new BattleCombatantHealthProjection(id, 42, 50),
                new BattleCombatantStatProjection(id, 10, 11, 12, 13, 14),
                new BattleCombatantAccuracyEvasionProjection(id, 1, 2, 3, 4),
                new BattleCombatantTraitsProjection(id, List.of("Ice"), List.of("Slush Rush")),
                new BattleCombatantMoveLoadoutProjection(id, List.of("tackle")),
                new BattleCombatantAffiliationProjection(id, "team-1", true),
                new BattleCombatantGeometryProjection(id, "Small"),
                new BattleCombatantBaseMovementProjection(id, 5, 2, 0, 1, 1),
                Set.of("burned"));

        BattleCoreMaterializationInputProjection materialization =
                new BattleCoreMaterializationInputProjection(reservationId, 123L, Map.of(id, input));
        AuthoritativeMoveMetadata tackle = new AuthoritativeMoveMetadata(
                "tackle",
                new AuthoritativeMoveMetadata.Targeting("single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard", true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will");
        BattleCoreMoveCatalogProjection moves =
                new BattleCoreMoveCatalogProjection(reservationId, Map.of(id, List.of(tackle)));
        BattleCoreHeldItemBootstrapProjection heldItems =
                new BattleCoreHeldItemBootstrapProjection(reservationId, Map.of());
        BattleCoreBootstrapProjection bootstrap =
                new BattleCoreBootstrapProjection(reservationId, 123L, Map.of(id, Set.of("burned")));
        BattleCoreStatusStateBootstrapProjection statuses =
                new BattleCoreStatusStateBootstrapProjection(reservationId, bootstrap,
                        Map.of(id, new BattleCombatantStatusStateProjection(id, List.of(new CanonicalStatusEntry("burned")))));

        return BattleRuntimePreparationEnvelope.from(materialization, moves, heldItems, statuses);
    }
}

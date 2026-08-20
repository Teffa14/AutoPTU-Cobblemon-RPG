package io.autoptu.cobblemon.battlecore;

import io.autoptu.cobblemon.authority.CanonicalStatusEntry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleInjuryRuntimePreparationEnvelopeTest {
    @Test
    void bindsCanonicalInjuriesToExactPreparedRoster() {
        BattleInjuryRuntimePreparationEnvelope envelope = BattleInjuryRuntimePreparationEnvelope.from(
                runtimePreparation("battle-1"),
                injuryBootstrap("battle-1", Map.of("mon-1", new BattleCombatantInjuryProjection("mon-1", 2)))
        );

        assertEquals("battle-1", envelope.reservationId());
        assertEquals(2, envelope.injuriesByCombatant().get("mon-1").injuries());
        assertEquals(Set.of("mon-1"), envelope.runtimePreparation().combatants().keySet());
        assertThrows(UnsupportedOperationException.class, () -> envelope.injuriesByCombatant().clear());
    }

    @Test
    void rejectsCrossReservationInjuryBinding() {
        assertThrows(IllegalArgumentException.class, () -> BattleInjuryRuntimePreparationEnvelope.from(
                runtimePreparation("battle-1"),
                injuryBootstrap("battle-2", Map.of("mon-1", new BattleCombatantInjuryProjection("mon-1", 1)))
        ));
    }

    @Test
    void rejectsIncompleteOrInjectedInjuryRoster() {
        assertThrows(IllegalArgumentException.class, () -> new BattleInjuryRuntimePreparationEnvelope(
                "battle-1", runtimePreparation("battle-1"), Map.of()
        ));
        assertThrows(IllegalArgumentException.class, () -> new BattleInjuryRuntimePreparationEnvelope(
                "battle-1", runtimePreparation("battle-1"),
                Map.of("other", new BattleCombatantInjuryProjection("other", 1))
        ));
    }

    @Test
    void rejectsForgedInjuryMapKey() {
        assertThrows(IllegalArgumentException.class, () -> new BattleInjuryRuntimePreparationEnvelope(
                "battle-1", runtimePreparation("battle-1"),
                Map.of("mon-1", new BattleCombatantInjuryProjection("other", 1))
        ));
    }

    private static BattleCoreInjuryBootstrapProjection injuryBootstrap(
            String reservationId,
            Map<String, BattleCombatantInjuryProjection> injuries
    ) {
        BattleCoreBootstrapProjection bootstrap = new BattleCoreBootstrapProjection(
                reservationId, 123L, Map.of("mon-1", Set.of("burned"))
        );
        return new BattleCoreInjuryBootstrapProjection(reservationId, bootstrap, injuries);
    }

    private static BattleRuntimePreparationEnvelope runtimePreparation(String reservationId) {
        String id = "mon-1";
        RuntimeCombatantMaterializationInput input = new RuntimeCombatantMaterializationInput(
                id,
                new BattleCombatantInitialPlacement(id, new BattleGridCoordinate(2, 3)),
                new BattleCombatantHealthProjection(id, 42, 50),
                new BattleCombatantStatProjection(id, 10, 11, 12, 13, 14),
                new BattleCombatantAccuracyEvasionProjection(id, 1, 2, 3, 4),
                new BattleCombatantTraitsProjection(id, List.of("Fire"), List.of("Blaze")),
                new BattleCombatantMoveLoadoutProjection(id, List.of("tackle")),
                new BattleCombatantAffiliationProjection(id, "team-1", true),
                new BattleCombatantGeometryProjection(id, "Small"),
                new BattleCombatantBaseMovementProjection(id, 5, 2, 0, 1, 1),
                Set.of("burned")
        );
        AuthoritativeMoveMetadata tackle = new AuthoritativeMoveMetadata(
                "tackle",
                new AuthoritativeMoveMetadata.Targeting("single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard",
                true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will"
        );
        return new BattleRuntimePreparationEnvelope(
                reservationId,
                123L,
                Map.of(id, input),
                Map.of(id, List.of(tackle)),
                Map.of(id, new BattleCombatantHeldItemProjection(id, "item-1", "Leftovers")),
                Map.of(id, new BattleCombatantStatusStateProjection(
                        id, List.of(new CanonicalStatusEntry("burned", Map.of("source", "move:ember"))))),
                Set.of(RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE)
        );
    }
}

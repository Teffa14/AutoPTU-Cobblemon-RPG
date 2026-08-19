package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleTrainerRuntimePreparationEnvelopeTest {
    @Test
    void bindsCanonicalTrainerStateToPreparedBattleRoster() {
        BattleTrainerRuntimePreparationEnvelope envelope = BattleTrainerRuntimePreparationEnvelope.from(
                battle("battle-1", "mon-1"),
                new BattleCoreTrainerRuntimeBootstrapProjection(
                        "battle-1",
                        new BattleTrainerRuntimeProjection("trainer-1", Set.of("Attack Link"), 3, Set.of("mon-1")))
        );

        assertEquals("battle-1", envelope.reservationId());
        assertEquals(123L, envelope.rngSeed());
        assertEquals("trainer-1", envelope.trainer().trainerId());
        assertEquals(Set.of("Attack Link"), envelope.trainer().trainerFeatures());
        assertEquals(3, envelope.trainer().actionPoints());
        assertEquals(Set.of("mon-1"), envelope.trainer().controlledCombatantIds());
        assertFalse(envelope.readyForRuntimeMaterialization());
    }

    @Test
    void rejectsCrossReservationTrainerInjection() {
        assertThrows(IllegalArgumentException.class, () -> BattleTrainerRuntimePreparationEnvelope.from(
                battle("battle-1", "mon-1"),
                new BattleCoreTrainerRuntimeBootstrapProjection(
                        "battle-2",
                        new BattleTrainerRuntimeProjection("trainer-1", Set.of(), 1, Set.of("mon-1")))
        ));
    }

    @Test
    void rejectsTrainerControllerCoverageThatDoesNotMatchPreparedRoster() {
        assertThrows(IllegalArgumentException.class, () -> new BattleTrainerRuntimePreparationEnvelope(
                battle("battle-1", "mon-1"),
                new BattleTrainerRuntimeProjection("trainer-1", Set.of(), 1, Set.of("other-mon"))
        ));
    }

    private static BattleRuntimePreparationEnvelope battle(String reservationId, String combatantId) {
        RuntimeCombatantMaterializationInput input = new RuntimeCombatantMaterializationInput(
                combatantId,
                new BattleCombatantInitialPlacement(combatantId, new BattleGridCoordinate(1, 2)),
                new BattleCombatantHealthProjection(combatantId, 40, 50),
                new BattleCombatantStatProjection(combatantId, 10, 11, 12, 13, 14),
                new BattleCombatantAccuracyEvasionProjection(combatantId, 0, 1, 1, 1),
                new BattleCombatantTraitsProjection(combatantId, List.of("Normal"), List.of()),
                new BattleCombatantMoveLoadoutProjection(combatantId, List.of("tackle")),
                new BattleCombatantAffiliationProjection(combatantId, "trainer-1", true),
                new BattleCombatantGeometryProjection(combatantId, "Small"),
                new BattleCombatantBaseMovementProjection(combatantId, 5, 0, 0, 1, 1),
                Set.of()
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
                Map.of(combatantId, input),
                Map.of(combatantId, List.of(tackle)),
                Map.of(),
                Map.of(combatantId, new BattleCombatantStatusStateProjection(combatantId, List.of())),
                EnumSet.of(
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE,
                        RuntimeCombatantMaterializationReadiness.Requirement.ACTION_BUDGET_INITIALIZATION,
                        RuntimeCombatantMaterializationReadiness.Requirement.DYNAMIC_ACCURACY_EVASION_FLAGS,
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_DAMAGE_MODIFIERS)
        );
    }
}

package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleClientActionRequestGateTest {
    @Test
    void acceptsMinimalShiftAndCanonicalMoveIntentWithoutClaimingLegality() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleClientActionRequest.Shift shift = new BattleClientActionRequest.Shift(
                "battle-1", "mon-1", new BattleGridCoordinate(99, -42));
        BattleClientActionRequest.Move move = new BattleClientActionRequest.Move(
                "battle-1", "mon-1", "tackle", BattleClientActionRequest.Target.combatant("mon-2"));

        assertSame(shift, BattleClientActionRequestGate.accept(preparation, shift));
        assertSame(move, BattleClientActionRequestGate.accept(preparation, move));
    }

    @Test
    void rejectsReservationRosterMoveAndTargetForgery() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionRequestGate.accept(preparation,
                new BattleClientActionRequest.Shift("other-battle", "mon-1", new BattleGridCoordinate(1, 1))));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionRequestGate.accept(preparation,
                new BattleClientActionRequest.Shift("battle-1", "intruder", new BattleGridCoordinate(1, 1))));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionRequestGate.accept(preparation,
                new BattleClientActionRequest.Move("battle-1", "mon-1", "forged-move", BattleClientActionRequest.Target.self())));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionRequestGate.accept(preparation,
                new BattleClientActionRequest.Move("battle-1", "mon-1", "tackle", BattleClientActionRequest.Target.combatant("intruder"))));
    }

    @Test
    void targetIntentShapeFailsClosed() {
        assertThrows(IllegalArgumentException.class, () -> new BattleClientActionRequest.Target(
                BattleClientActionRequest.Target.Mode.COMBATANT, null, null));
        assertThrows(IllegalArgumentException.class, () -> new BattleClientActionRequest.Target(
                BattleClientActionRequest.Target.Mode.TILE, "mon-2", new BattleGridCoordinate(1, 1)));
        assertThrows(IllegalArgumentException.class, () -> new BattleClientActionRequest.Target(
                BattleClientActionRequest.Target.Mode.SELF, "mon-1", null));
        assertDoesNotThrow(BattleClientActionRequest.Target::field);
    }

    @Test
    void requestFeaturesConsumeNoBlockingUpstreamCapability() {
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_SHIFT_REQUEST).hasBlockingDependency());
        assertFalse(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.MOVE_SELECTION_REQUEST).hasBlockingDependency());
    }

    private static BattleRuntimePreparationEnvelope preparation() {
        RuntimeCombatantMaterializationInput mon1 = combatant("mon-1", List.of("tackle"));
        RuntimeCombatantMaterializationInput mon2 = combatant("mon-2", List.of("tackle"));
        AuthoritativeMoveMetadata tackle = tackle();
        return new BattleRuntimePreparationEnvelope(
                "battle-1",
                123L,
                Map.of("mon-1", mon1, "mon-2", mon2),
                Map.of("mon-1", List.of(tackle), "mon-2", List.of(tackle)),
                Map.of(),
                Map.of(
                        "mon-1", new BattleCombatantStatusStateProjection("mon-1", List.of()),
                        "mon-2", new BattleCombatantStatusStateProjection("mon-2", List.of())),
                Set.of(
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_MOVEMENT_PROFILE,
                        RuntimeCombatantMaterializationReadiness.Requirement.DYNAMIC_ACCURACY_EVASION_FLAGS,
                        RuntimeCombatantMaterializationReadiness.Requirement.RESOLVED_DAMAGE_MODIFIERS));
    }

    private static RuntimeCombatantMaterializationInput combatant(String id, List<String> moveIds) {
        return new RuntimeCombatantMaterializationInput(
                id,
                new BattleCombatantInitialPlacement(id, new BattleGridCoordinate(2, 3)),
                new BattleCombatantHealthProjection(id, 42, 50),
                new BattleCombatantStatProjection(id, 10, 11, 12, 13, 14),
                new BattleCombatantAccuracyEvasionProjection(id, 1, 2, 3, 4),
                new BattleCombatantTraitsProjection(id, List.of("Normal"), List.of()),
                new BattleCombatantMoveLoadoutProjection(id, moveIds),
                new BattleCombatantAffiliationProjection(id, id.equals("mon-1") ? "team-1" : "team-2", true),
                new BattleCombatantGeometryProjection(id, "Small"),
                new BattleCombatantBaseMovementProjection(id, 5, 2, 0, 1, 1),
                Set.of());
    }

    private static AuthoritativeMoveMetadata tackle() {
        return new AuthoritativeMoveMetadata(
                "tackle",
                new AuthoritativeMoveMetadata.Targeting("single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard",
                true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will");
    }
}

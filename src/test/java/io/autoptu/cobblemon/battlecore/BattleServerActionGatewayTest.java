package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleServerActionGatewayTest {
    @Test
    void fetchesCurrentCoreChoicesAndExecutesTheExactSelectedObject() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleCoreLegalChoice.Move legal = new BattleCoreLegalChoice.Move(
                "mon-1", "tackle", BattleClientActionRequest.Target.Mode.COMBATANT,
                "mon-2", new BattleGridCoordinate(8, 9), "standard",
                "move|mon-1|tackle|combatant|mon-2|8,9|standard");
        AtomicInteger sourceCalls = new AtomicInteger();
        AtomicReference<BattleCoreLegalChoice> executed = new AtomicReference<>();
        AtomicReference<String> executedReservation = new AtomicReference<>();

        BattleCoreLegalChoice selected = BattleServerActionGateway.execute(
                preparation,
                new BattleClientActionRequest.Move(
                        "battle-1", "mon-1", "tackle", BattleClientActionRequest.Target.combatant("mon-2")),
                (reservationId, actorId) -> {
                    sourceCalls.incrementAndGet();
                    assertEquals("battle-1", reservationId);
                    assertEquals("mon-1", actorId);
                    return new BattleCoreLegalChoiceSet(reservationId, actorId, List.of(legal));
                },
                (reservationId, choice) -> {
                    executedReservation.set(reservationId);
                    executed.set(choice);
                });

        assertEquals(1, sourceCalls.get());
        assertEquals("battle-1", executedReservation.get());
        assertSame(legal, selected);
        assertSame(legal, executed.get());
    }

    @Test
    void forgedRequestFailsBeforeAuthoritativeChoiceLookupOrExecution() {
        AtomicInteger sourceCalls = new AtomicInteger();
        AtomicInteger executionCalls = new AtomicInteger();

        assertThrows(IllegalArgumentException.class, () -> BattleServerActionGateway.execute(
                preparation(),
                new BattleClientActionRequest.Move(
                        "battle-1", "mon-1", "forged-move", BattleClientActionRequest.Target.combatant("mon-2")),
                (reservationId, actorId) -> {
                    sourceCalls.incrementAndGet();
                    return new BattleCoreLegalChoiceSet(reservationId, actorId, List.of());
                },
                (reservationId, choice) -> executionCalls.incrementAndGet()));

        assertEquals(0, sourceCalls.get());
        assertEquals(0, executionCalls.get());
    }

    @Test
    void actionAbsentFromCurrentCoreSpaceNeverReachesExecutor() {
        AtomicInteger executionCalls = new AtomicInteger();

        assertThrows(IllegalArgumentException.class, () -> BattleServerActionGateway.execute(
                preparation(),
                new BattleClientActionRequest.Shift("battle-1", "mon-1", new BattleGridCoordinate(9, 9)),
                (reservationId, actorId) -> new BattleCoreLegalChoiceSet(reservationId, actorId, List.of()),
                (reservationId, choice) -> executionCalls.incrementAndGet()));

        assertEquals(0, executionCalls.get());
    }

    @Test
    void staleOrCrossActorChoiceSnapshotNeverReachesExecutor() {
        AtomicInteger executionCalls = new AtomicInteger();
        BattleCoreLegalChoice.Shift otherActor = new BattleCoreLegalChoice.Shift(
                "mon-2", new BattleGridCoordinate(3, 4), "shift|mon-2|3,4");

        assertThrows(IllegalArgumentException.class, () -> BattleServerActionGateway.execute(
                preparation(),
                new BattleClientActionRequest.Shift("battle-1", "mon-1", new BattleGridCoordinate(3, 4)),
                (reservationId, actorId) -> new BattleCoreLegalChoiceSet("battle-1", "mon-2", List.of(otherActor)),
                (reservationId, choice) -> executionCalls.incrementAndGet()));

        assertEquals(0, executionCalls.get());
    }

    @Test
    void gatewayComposesOnlyExistingNonBlockingRequestAndLegalChoiceFeatures() {
        List<IntegrationFeatureCompatibility.Feature> features = List.of(
                IntegrationFeatureCompatibility.Feature.PLAYER_SHIFT_REQUEST,
                IntegrationFeatureCompatibility.Feature.MOVE_SELECTION_REQUEST,
                IntegrationFeatureCompatibility.Feature.AUTOBATTLER_LEGAL_CHOICE_INPUT);

        for (IntegrationFeatureCompatibility.Feature feature : features) {
            assertFalse(IntegrationFeatureCompatibility.requirement(feature).hasBlockingDependency());
        }
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.BLOCKING,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY).support());
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.MOVE_SELECTION_REQUEST).capabilities().contains(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR));
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

package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleServerActionRequestHandlerTest {
    @Test
    void resolvesPreparationFromAuthenticatedServerPrincipalBeforeExecutingCoreChoice() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleCoreLegalChoice.Move legal = legalMove();
        AtomicReference<String> principal = new AtomicReference<>();
        AtomicReference<BattleCoreLegalChoice> executed = new AtomicReference<>();

        BattleCoreLegalChoice selected = BattleServerActionRequestHandler.handle(
                "player-1",
                request(),
                (authenticatedPrincipalId, reservationId, actorId) -> {
                    principal.set(authenticatedPrincipalId);
                    assertEquals("battle-1", reservationId);
                    assertEquals("mon-1", actorId);
                    return preparation;
                },
                (reservationId, actorId) -> new BattleCoreLegalChoiceSet(
                        reservationId, actorId, List.of(legal)),
                (reservationId, choice) -> executed.set(choice));

        assertEquals("player-1", principal.get());
        assertSame(legal, selected);
        assertSame(legal, executed.get());
    }

    @Test
    void rejectedServerPrincipalStopsBeforeLegalChoiceLookupAndExecution() {
        AtomicInteger legalLookups = new AtomicInteger();
        AtomicInteger executions = new AtomicInteger();

        assertThrows(IllegalArgumentException.class, () -> BattleServerActionRequestHandler.handle(
                "intruder",
                request(),
                (principalId, reservationId, actorId) -> {
                    throw new IllegalArgumentException("principal does not own reserved actor");
                },
                (reservationId, actorId) -> {
                    legalLookups.incrementAndGet();
                    return new BattleCoreLegalChoiceSet(reservationId, actorId, List.of());
                },
                (reservationId, choice) -> executions.incrementAndGet()));

        assertEquals(0, legalLookups.get());
        assertEquals(0, executions.get());
    }

    @Test
    void clientCannotSupplyAuthenticatedPrincipalThroughBattleRequest() {
        var componentNames = java.util.Arrays.stream(BattleClientActionRequest.Move.class.getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .collect(java.util.stream.Collectors.toSet());

        assertEquals(Set.of("reservationId", "actorId", "moveId", "target"), componentNames);
    }

    @Test
    void nullPreparationFailsClosedBeforeLegalChoiceLookup() {
        AtomicInteger legalLookups = new AtomicInteger();

        assertThrows(NullPointerException.class, () -> BattleServerActionRequestHandler.handle(
                "player-1",
                request(),
                (principalId, reservationId, actorId) -> null,
                (reservationId, actorId) -> {
                    legalLookups.incrementAndGet();
                    return new BattleCoreLegalChoiceSet(reservationId, actorId, List.of());
                },
                (reservationId, choice) -> {}));

        assertEquals(0, legalLookups.get());
    }

    private static BattleClientActionRequest request() {
        return new BattleClientActionRequest.Move(
                "battle-1", "mon-1", "tackle", BattleClientActionRequest.Target.combatant("mon-2"));
    }

    private static BattleCoreLegalChoice.Move legalMove() {
        return new BattleCoreLegalChoice.Move(
                "mon-1", "tackle", BattleClientActionRequest.Target.Mode.COMBATANT,
                "mon-2", new BattleGridCoordinate(8, 9), "standard",
                "move|mon-1|tackle|combatant|mon-2|8,9|standard");
    }

    private static BattleRuntimePreparationEnvelope preparation() {
        RuntimeCombatantMaterializationInput mon1 = combatant("mon-1", List.of("tackle"));
        RuntimeCombatantMaterializationInput mon2 = combatant("mon-2", List.of("tackle"));
        AuthoritativeMoveMetadata tackle = new AuthoritativeMoveMetadata(
                "tackle",
                new AuthoritativeMoveMetadata.Targeting("single", "melee", 1, 1, null, null, "Melee, 1 Target"),
                "standard",
                true,
                new AuthoritativeMoveMetadata.Combat(2, 5, 20, "physical", "Normal"),
                "At-Will");
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
}

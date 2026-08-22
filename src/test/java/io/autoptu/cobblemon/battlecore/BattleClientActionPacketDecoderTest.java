package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleClientActionPacketDecoderTest {
    @Test
    void decodesShiftAndAllMoveTargetModesIntoMinimalIntent() {
        BattleClientActionRequest.Shift shift = assertInstanceOf(
                BattleClientActionRequest.Shift.class,
                BattleClientActionPacketDecoder.decode(new BattleClientActionPacket(
                        " battle-1 ", " mon-1 ", "shift", null, null, null, 4, -2)));
        assertEquals("battle-1", shift.reservationId());
        assertEquals("mon-1", shift.actorId());
        assertEquals(new BattleGridCoordinate(4, -2), shift.destination());

        BattleClientActionRequest.Move combatant = move(packet("combatant", "mon-2", null, null));
        assertEquals(BattleClientActionRequest.Target.Mode.COMBATANT, combatant.target().mode());
        assertEquals("mon-2", combatant.target().combatantId());

        BattleClientActionRequest.Move tile = move(packet("tile", null, 7, 8));
        assertEquals(BattleClientActionRequest.Target.Mode.TILE, tile.target().mode());
        assertEquals(new BattleGridCoordinate(7, 8), tile.target().tile());

        assertEquals(BattleClientActionRequest.Target.Mode.SELF, move(packet("self", null, null, null)).target().mode());
        assertEquals(BattleClientActionRequest.Target.Mode.FIELD, move(packet("field", null, null, null)).target().mode());
    }

    @Test
    void malformedOrContradictoryTransportPayloadsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionPacketDecoder.decode(
                new BattleClientActionPacket("battle-1", "mon-1", "unknown", null, null, null, null, null)));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionPacketDecoder.decode(
                new BattleClientActionPacket("battle-1", "mon-1", "shift", "tackle", null, null, 1, 2)));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionPacketDecoder.decode(
                new BattleClientActionPacket("battle-1", "mon-1", "shift", null, null, null, 1, null)));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionPacketDecoder.decode(
                new BattleClientActionPacket("battle-1", "mon-1", "move", "tackle", "combatant", "mon-2", 1, 2)));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionPacketDecoder.decode(
                new BattleClientActionPacket("battle-1", "mon-1", "move", "tackle", "tile", "mon-2", 1, 2)));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionPacketDecoder.decode(
                new BattleClientActionPacket("battle-1", "mon-1", "move", "tackle", "self", null, 1, 2)));
        assertThrows(IllegalArgumentException.class, () -> BattleClientActionPacketDecoder.decode(
                new BattleClientActionPacket("battle-1", "mon-1", "move", null, "field", null, null, null)));
    }

    @Test
    void packetSchemaCannotCarryPrincipalOrTrustedBattleState() {
        Set<String> components = Arrays.stream(BattleClientActionPacket.class.getRecordComponents())
                .map(RecordComponent::getName)
                .collect(Collectors.toSet());

        assertEquals(Set.of(
                "reservationId", "actorId", "actionKind", "moveId", "targetMode",
                "targetCombatantId", "targetX", "targetY"), components);
        for (String forbidden : Set.of(
                "authenticatedPrincipalId", "playerId", "stats", "modifiers", "inventory",
                "actionBudget", "moveFrequency", "moveMetadata", "legalChoices", "targetingResult",
                "accuracy", "damage", "targetHp", "outcome", "currentRound", "temporaryEffects",
                "followMeExpiry", "foresightExpiry", "expiryDecision")) {
            assertTrue(!components.contains(forbidden), forbidden + " must remain outside the client packet");
        }
    }

    @Test
    void packetHandlerKeepsServerPrincipalOutsidePacketAndExecutesExactCoreChoice() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleCoreLegalChoice.Move legal = legalMove();
        AtomicReference<String> principal = new AtomicReference<>();
        AtomicReference<BattleCoreLegalChoice> executed = new AtomicReference<>();

        BattleCoreLegalChoice selected = BattleServerActionPacketHandler.handle(
                "player-1",
                packet("combatant", "mon-2", null, null),
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
    void transportBoundaryConsumesOnlyExistingNonBlockingRequestAndLegalChoiceFeatures() {
        assertTrue(!IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_SHIFT_REQUEST).hasBlockingDependency());
        assertTrue(!IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.MOVE_SELECTION_REQUEST).hasBlockingDependency());
        assertTrue(!IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.AUTOBATTLER_LEGAL_CHOICE_INPUT).hasBlockingDependency());
    }

    private static BattleClientActionRequest.Move move(BattleClientActionPacket packet) {
        return assertInstanceOf(BattleClientActionRequest.Move.class, BattleClientActionPacketDecoder.decode(packet));
    }

    private static BattleClientActionPacket packet(
            String targetMode,
            String targetCombatantId,
            Integer targetX,
            Integer targetY
    ) {
        return new BattleClientActionPacket(
                "battle-1", "mon-1", "move", "tackle", targetMode,
                targetCombatantId, targetX, targetY);
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

package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleClientLegalChoiceMatcherTest {
    @Test
    void selectsOnlyShiftDestinationAlreadyOfferedByCore() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleCoreLegalChoice.Shift legal = new BattleCoreLegalChoice.Shift(
                "mon-1", new BattleGridCoordinate(3, 4), "shift|mon-1|3,4");
        BattleCoreLegalChoiceSet choices = new BattleCoreLegalChoiceSet(
                "battle-1", "mon-1", List.of(legal));

        BattleCoreLegalChoice selected = BattleClientLegalChoiceMatcher.select(
                preparation,
                new BattleClientActionRequest.Shift("battle-1", "mon-1", new BattleGridCoordinate(3, 4)),
                choices);

        assertSame(legal, selected);
        assertThrows(IllegalArgumentException.class, () -> BattleClientLegalChoiceMatcher.select(
                preparation,
                new BattleClientActionRequest.Shift("battle-1", "mon-1", new BattleGridCoordinate(4, 4)),
                choices));
    }

    @Test
    void selectsCombatantMoveButPreservesCoreOwnedAnchorActionTypeAndStableKey() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleCoreLegalChoice.Move legal = new BattleCoreLegalChoice.Move(
                "mon-1",
                "tackle",
                BattleClientActionRequest.Target.Mode.COMBATANT,
                "mon-2",
                new BattleGridCoordinate(8, 9),
                "standard",
                "move|mon-1|tackle|combatant|mon-2|8,9|standard");

        BattleCoreLegalChoice selected = BattleClientLegalChoiceMatcher.select(
                preparation,
                new BattleClientActionRequest.Move(
                        "battle-1", "mon-1", "tackle", BattleClientActionRequest.Target.combatant("mon-2")),
                new BattleCoreLegalChoiceSet("battle-1", "mon-1", List.of(legal)));

        assertSame(legal, selected);
        BattleCoreLegalChoice.Move move = (BattleCoreLegalChoice.Move) selected;
        assertEquals(new BattleGridCoordinate(8, 9), move.targetAnchor());
        assertEquals("standard", move.actionType());
        assertEquals("move|mon-1|tackle|combatant|mon-2|8,9|standard", move.stableKey());
    }

    @Test
    void canonicalMoveStillFailsWhenCurrentCoreActionSpaceDoesNotOfferIt() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleClientActionRequest.Move request = new BattleClientActionRequest.Move(
                "battle-1", "mon-1", "tackle", BattleClientActionRequest.Target.combatant("mon-2"));

        assertThrows(IllegalArgumentException.class, () -> BattleClientLegalChoiceMatcher.select(
                preparation,
                request,
                new BattleCoreLegalChoiceSet("battle-1", "mon-1", List.of())));
    }

    @Test
    void tileIntentMustMatchTheCoreProducedTargetAnchorExactly() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleCoreLegalChoice.Move legal = new BattleCoreLegalChoice.Move(
                "mon-1",
                "tackle",
                BattleClientActionRequest.Target.Mode.TILE,
                null,
                new BattleGridCoordinate(5, 6),
                "standard",
                "move|mon-1|tackle|tile||5,6|standard");
        BattleCoreLegalChoiceSet choices = new BattleCoreLegalChoiceSet(
                "battle-1", "mon-1", List.of(legal));

        assertSame(legal, BattleClientLegalChoiceMatcher.select(
                preparation,
                new BattleClientActionRequest.Move(
                        "battle-1", "mon-1", "tackle",
                        BattleClientActionRequest.Target.tile(new BattleGridCoordinate(5, 6))),
                choices));
        assertThrows(IllegalArgumentException.class, () -> BattleClientLegalChoiceMatcher.select(
                preparation,
                new BattleClientActionRequest.Move(
                        "battle-1", "mon-1", "tackle",
                        BattleClientActionRequest.Target.tile(new BattleGridCoordinate(5, 7))),
                choices));
    }

    @Test
    void ambiguousCoreProjectionFailsClosedInsteadOfChoosingForTheClient() {
        BattleRuntimePreparationEnvelope preparation = preparation();
        BattleCoreLegalChoice.Move first = new BattleCoreLegalChoice.Move(
                "mon-1", "tackle", BattleClientActionRequest.Target.Mode.SELF, null,
                new BattleGridCoordinate(2, 3), "standard", "choice-a");
        BattleCoreLegalChoice.Move second = new BattleCoreLegalChoice.Move(
                "mon-1", "tackle", BattleClientActionRequest.Target.Mode.SELF, null,
                new BattleGridCoordinate(2, 3), "swift", "choice-b");

        assertThrows(IllegalArgumentException.class, () -> BattleClientLegalChoiceMatcher.select(
                preparation,
                new BattleClientActionRequest.Move(
                        "battle-1", "mon-1", "tackle", BattleClientActionRequest.Target.self()),
                new BattleCoreLegalChoiceSet("battle-1", "mon-1", List.of(first, second))));
    }

    @Test
    void legalChoiceSnapshotRejectsCrossActorDuplicatesAndCallerMutation() {
        BattleCoreLegalChoice.Shift legal = new BattleCoreLegalChoice.Shift(
                "mon-1", new BattleGridCoordinate(3, 4), "shift|mon-1|3,4");
        ArrayList<BattleCoreLegalChoice> caller = new ArrayList<>(List.of(legal));
        BattleCoreLegalChoiceSet choices = new BattleCoreLegalChoiceSet("battle-1", "mon-1", caller);
        caller.clear();

        assertEquals(1, choices.choices().size());
        assertThrows(UnsupportedOperationException.class, () -> choices.choices().clear());
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreLegalChoiceSet(
                "battle-1", "mon-1", List.of(
                        legal,
                        new BattleCoreLegalChoice.Shift(
                                "mon-1", new BattleGridCoordinate(4, 4), "shift|mon-1|3,4"))));
        assertThrows(IllegalArgumentException.class, () -> new BattleCoreLegalChoiceSet(
                "battle-1", "mon-1", List.of(
                        new BattleCoreLegalChoice.Shift(
                                "mon-2", new BattleGridCoordinate(4, 4), "shift|mon-2|4,4"))));
    }

    @Test
    void legalChoiceSelectionConsumesVerifiedCoreActionSpaceWithoutUnlockingTacticalAi() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.AUTOBATTLER_LEGAL_CHOICE_INPUT);
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.capabilities().contains(
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE));
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.BLOCKING,
                UpstreamCompatibilityMatrix.entry(
                        UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY).support());
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

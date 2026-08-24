package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("1f9b721ef2f5e2e1dcc2c6d70a93b13875e5f0db", CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("a3ff5cc71adb080973522abd604b0248b4447e06", CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
    }

    @Test
    void currentInspectionCoversEveryPermanentCapabilityCategory() {
        assertEquals(UpstreamCompatibilityMatrix.Capability.values().length,
                CurrentUpstreamCompatibilityInspection.evidence().size());
        for (UpstreamCompatibilityMatrix.Capability capability : UpstreamCompatibilityMatrix.Capability.values()) {
            CurrentUpstreamCompatibilityInspection.Evidence evidence =
                    CurrentUpstreamCompatibilityInspection.evidence(capability);
            assertTrue(evidence.contracts() != null && !evidence.contracts().isBlank());
            assertTrue(evidence.limitation() != null && !evidence.limitation().isBlank());
        }
    }

    @Test
    void boundedForcedMovementDoesNotPromoteCompleteMovementBehavior() {
        CurrentUpstreamCompatibilityInspection.Evidence movement = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_MOVEMENT_BEHAVIOR);
        assertEquals(UpstreamCompatibilityMatrix.Support.BLOCKING, movement.support());
        assertTrue(movement.contracts().contains("Sway adjacent push"));
        assertTrue(movement.limitation().contains("must fail closed"));
        assertTrue(movement.limitation().contains("must not generalize"));
    }

    @Test
    void mergedPreDamageHooksStayPartialAtCategoryLevel() {
        CurrentUpstreamCompatibilityInspection.Evidence abilities = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertTrue(abilities.contracts().contains("Sway"));
        assertTrue(abilities.contracts().contains("Shell Shield"));
        assertTrue(abilities.contracts().contains("nested follow-up execution"));
        assertTrue(abilities.limitation().contains("do not establish full parity"));

        CurrentUpstreamCompatibilityInspection.Evidence statuses = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());
        assertTrue(statuses.contracts().contains("Withdrawn"));
        assertTrue(statuses.limitation().contains("remain partial"));
    }

    @Test
    void mergedStatusRuntimeIsPromotedWithoutInventingStatusEffects() {
        CurrentUpstreamCompatibilityInspection.Evidence statuses = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence moves = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);
        CurrentUpstreamCompatibilityInspection.Evidence adapter = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(statuses.contracts().contains("StatusMoveRuntimeResolution"));
        assertTrue(statuses.contracts().contains("crit is false"));
        assertTrue(statuses.contracts().contains("damage is zero"));
        assertTrue(statuses.contracts().contains("action/frequency resources are spent once"));
        assertTrue(statuses.limitation().contains("does not itself apply any status"));
        assertTrue(moves.contracts().contains("MoveResolvedEvent"));
        assertTrue(moves.contracts().contains("damage=0"));
        assertTrue(moves.limitation().contains("status application"));
        assertTrue(adapter.contracts().contains("zero-damage Status MoveResolvedEvent playback"));
        assertTrue(adapter.limitation().contains("must not derive effects"));
    }

    @Test
    void statusRuntimeStillDependsOnCoreTargetingActionEconomyAndLegalChoices() {
        CurrentUpstreamCompatibilityInspection.Evidence targeting = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING);
        CurrentUpstreamCompatibilityInspection.Evidence actions = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        CurrentUpstreamCompatibilityInspection.Evidence legalActions = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE);

        assertTrue(targeting.contracts().contains("StatusMoveRuntimeResolution"));
        assertTrue(targeting.contracts().contains("MoveChoiceRevalidation"));
        assertTrue(actions.contracts().contains("StatusMoveRuntimeResolution"));
        assertTrue(actions.contracts().contains("exactly once"));
        assertTrue(legalActions.contracts().contains("legal combatant-target MoveChoice"));
        assertTrue(legalActions.contracts().contains("MoveChoiceRevalidation"));
    }

    @Test
    void tacticalScoringRemainsBlockingWhileLegalActionInfrastructureIsVerified() {
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.AI_LEGAL_ACTION_INFRASTRUCTURE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.BLOCKING,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.AI_TACTICAL_SCORING_POLICY).support());
    }

    @Test
    void currentPythonInspectionDoesNotPromoteTrainerFeatures() {
        CurrentUpstreamCompatibilityInspection.Evidence perks = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS);
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, perks.support());
        assertTrue(perks.contracts().contains("a3ff5cc"));
        assertTrue(perks.contracts().contains("Career loan-return"));
    }
}

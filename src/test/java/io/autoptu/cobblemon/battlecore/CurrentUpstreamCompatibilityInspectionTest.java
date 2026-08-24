package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("f23f5aebe51f50d8aa32449878f13e5f4c644f1f", CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
        assertEquals("03321a2eba42437180fddf5c4b2570c50ba429a6", CurrentUpstreamCompatibilityInspection.AUTOPTU_PYTHON_SHA);
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
        assertTrue(statuses.limitation().contains("does not promote the whole status category"));
    }

    @Test
    void frozenStatusExecutionContractDoesNotPromoteOpenRuntimeWork() {
        CurrentUpstreamCompatibilityInspection.Evidence statuses = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE);
        CurrentUpstreamCompatibilityInspection.Evidence moves = CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, statuses.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, moves.support());
        assertTrue(statuses.contracts().contains("hit follows ordinary accuracy"));
        assertTrue(statuses.contracts().contains("damage and damage_roll are zero"));
        assertTrue(statuses.limitation().contains("open Java PR #182"));
        assertTrue(moves.contracts().contains("open PR #182"));
        assertTrue(moves.limitation().contains("status effects"));
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
        assertTrue(perks.contracts().contains("03321a2"));
        assertTrue(perks.contracts().contains("Career sponsor-market"));
    }
}

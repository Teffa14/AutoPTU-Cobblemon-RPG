package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamCompatibilityInspectionTest {
    @Test
    void pinsTheActuallyInspectedUpstreamHeads() {
        assertEquals("ab520743d8d99f06fa28fd4d6fa06a0c4ecd3fee", CurrentUpstreamCompatibilityInspection.AUTOPTU_JAVA_SHA);
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

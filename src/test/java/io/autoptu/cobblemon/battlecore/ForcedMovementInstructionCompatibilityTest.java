package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ForcedMovementInstructionCompatibilityTest {
    @Test
    void authoritativeReactionMovementDoesNotPromoteGenericForcedMovementPlayback() {
        assertTrue(ForcedMovementInstructionCompatibility.instructionDetectionIsAvailable());
        assertTrue(ForcedMovementInstructionCompatibility.reactionEscapeDestinationSelectionIsAvailable());
        assertTrue(ForcedMovementInstructionCompatibility.reactionMovementApplicationIsAuthoritative());
        assertTrue(ForcedMovementInstructionCompatibility.reactionMovementEmitsSemanticShiftEvent());
        assertFalse(ForcedMovementInstructionCompatibility.forcedMovementPlaybackIsAllowed());
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK
        ).hasBlockingDependency());
    }

    @Test
    void adapterPolicyConsumesSemanticReactionMovementButKeepsGenericSpatialRulesUpstream() {
        String policy = ForcedMovementInstructionCompatibility.adapterPolicy();
        assertTrue(policy.contains("ShiftResolvedEvent"));
        assertTrue(policy.contains("Do not parse move text"));
        assertTrue(policy.contains("collision/interception/reaction ordering"));
        assertTrue(policy.contains("partial generic forced-movement instructions"));
    }
}

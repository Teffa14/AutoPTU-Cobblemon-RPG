package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ForcedMovementInstructionCompatibilityTest {
    @Test
    void parserAvailabilityDoesNotPromoteForcedMovementPlayback() {
        assertTrue(ForcedMovementInstructionCompatibility.instructionDetectionIsAvailable());
        assertFalse(ForcedMovementInstructionCompatibility.forcedMovementPlaybackIsAllowed());
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK
        ).hasBlockingDependency());
    }

    @Test
    void adapterPolicyKeepsSpatialRulesUpstream() {
        String policy = ForcedMovementInstructionCompatibility.adapterPolicy();
        assertTrue(policy.contains("Do not parse move text"));
        assertTrue(policy.contains("collision/interception/reactions"));
        assertTrue(policy.contains("instruction alone"));
    }
}

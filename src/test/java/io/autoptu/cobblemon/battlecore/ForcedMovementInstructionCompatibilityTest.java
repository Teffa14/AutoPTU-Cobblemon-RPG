package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

final class ForcedMovementInstructionCompatibilityTest {
    @Test
    void partialMovementContractsDoNotPromoteForcedMovementPlayback() {
        assertTrue(ForcedMovementInstructionCompatibility.instructionDetectionIsAvailable());
        assertTrue(ForcedMovementInstructionCompatibility.reactionEscapeDestinationSelectionIsAvailable());
        assertFalse(ForcedMovementInstructionCompatibility.forcedMovementPlaybackIsAllowed());
        assertTrue(IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.FORCED_MOVEMENT_PLAYBACK
        ).hasBlockingDependency());
    }

    @Test
    void adapterPolicyKeepsSpatialRulesUpstream() {
        String policy = ForcedMovementInstructionCompatibility.adapterPolicy();
        assertTrue(policy.contains("Do not parse move text"));
        assertTrue(policy.contains("recompute reaction escape destinations"));
        assertTrue(policy.contains("collision/interception/reaction ordering"));
        assertTrue(policy.contains("partial movement primitives alone"));
    }
}

package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class WildClaimPreparationCompatibilityTest {
    @Test
    void dependsOnlyOnAdapterPlaybackBoundaryAndNoBlockingCapability() {
        var requirement = WildClaimPreparationCompatibility.requirement();

        assertEquals(
                Set.of(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                requirement.capabilities()
        );
        assertFalse(requirement.hasBlockingDependency());
    }
}

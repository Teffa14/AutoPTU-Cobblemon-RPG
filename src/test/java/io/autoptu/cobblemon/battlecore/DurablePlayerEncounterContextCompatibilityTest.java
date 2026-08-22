package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurablePlayerEncounterContextCompatibilityTest {
    @Test
    void durableContextStaysInsideExistingAuthenticatedContextBoundary() {
        IntegrationFeatureCompatibility.Requirement requirement =
                IntegrationFeatureCompatibility.requirement(
                        IntegrationFeatureCompatibility.Feature.AUTHENTICATED_PLAYER_CONTEXT_RESOLUTION);

        assertEquals(Set.of(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                requirement.capabilities());
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.boundedScope().contains("server-owned canonical encounter context"));
    }
}

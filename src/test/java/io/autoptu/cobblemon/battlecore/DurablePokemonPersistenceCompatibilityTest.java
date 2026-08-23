package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DurablePokemonPersistenceCompatibilityTest {
    @Test
    void durablePokemonPersistenceDoesNotDependOnBlockingUpstreamBehavior() {
        assertFalse(DurablePokemonPersistenceCompatibility.hasBlockingDependency());
        assertTrue(DurablePokemonPersistenceCompatibility.CAPABILITIES.contains(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE));
        assertTrue(DurablePokemonPersistenceCompatibility.CAPABILITIES.contains(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
    }
}

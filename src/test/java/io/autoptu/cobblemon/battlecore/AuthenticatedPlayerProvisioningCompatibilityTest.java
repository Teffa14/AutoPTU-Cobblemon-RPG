package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AuthenticatedPlayerProvisioningCompatibilityTest {
    @Test
    void provisioningConsumesOnlyExistingAuthenticatedAdapterBoundary() {
        IntegrationFeatureCompatibility.Requirement requirement =
                AuthenticatedPlayerProvisioningCompatibility.requirement();

        assertEquals(IntegrationFeatureCompatibility.Feature.AUTHENTICATED_PLAYER_CONTEXT_RESOLUTION,
                AuthenticatedPlayerProvisioningCompatibility.integrationFeature());
        assertEquals(Set.of(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                requirement.capabilities());
        assertFalse(requirement.hasBlockingDependency());
    }
}

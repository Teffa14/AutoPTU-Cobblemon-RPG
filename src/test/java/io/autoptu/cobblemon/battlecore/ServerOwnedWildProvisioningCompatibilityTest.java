package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerOwnedWildProvisioningCompatibilityTest {
    @Test
    void provisioningConsumesOnlyThePartialAdapterAuthorityBoundary() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.SERVER_OWNED_WILD_ENCOUNTER_PROVISIONING);

        assertEquals(
                EnumSet.of(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                EnumSet.copyOf(requirement.capabilities())
        );
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.boundedScope().contains("server-owned RPG encounter blueprints"));
        assertTrue(requirement.boundedScope().contains("external WILD actor UUID is correlation only"));
        assertTrue(requirement.boundedScope().contains("executes no movement, targeting, damage, status, ability, item or AI rules"));
        assertTrue(requirement.boundedScope().contains("never reads trusted values from PokemonEntity"));
    }
}

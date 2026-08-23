package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerOwnedWildBlueprintPublicationCompatibilityTest {
    @Test
    void publicationStaysInsideExistingWildProvisioningBoundary() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.SERVER_OWNED_WILD_ENCOUNTER_PROVISIONING
        );

        assertEquals(1, requirement.capabilities().size());
        assertTrue(requirement.capabilities().contains(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.boundedScope().contains("trusted server-owned RPG encounter blueprints"));
        assertTrue(requirement.boundedScope().contains("before Cobblemon battle-start interception"));
        assertTrue(requirement.boundedScope().contains("never reads trusted values from PokemonEntity"));
    }
}

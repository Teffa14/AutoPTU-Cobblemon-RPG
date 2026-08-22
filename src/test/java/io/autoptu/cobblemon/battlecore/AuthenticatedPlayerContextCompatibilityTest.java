package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthenticatedPlayerContextCompatibilityTest {
    @Test
    void authenticationConsumesOnlyTheBoundedMinecraftAdapterCapability() {
        IntegrationFeatureCompatibility.Requirement requirement = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.AUTHENTICATED_PLAYER_CONTEXT_RESOLUTION);

        assertEquals(
                EnumSet.of(UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK),
                EnumSet.copyOf(requirement.capabilities())
        );
        assertFalse(requirement.hasBlockingDependency());
        assertTrue(requirement.boundedScope().contains("currently connected ServerPlayerEntity"));
        assertTrue(requirement.boundedScope().contains("MinecraftServer PlayerManager"));
        assertTrue(requirement.boundedScope().contains("authentication only"));
        assertTrue(requirement.boundedScope().contains("never become PTU authority"));
    }

    @Test
    void newlyExpandedTrainerFeatureContractsDoNotBroadenAdapterAuthority() {
        UpstreamCompatibilityMatrix.Entry features = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS);

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, features.support());
        assertTrue(features.contracts().contains("TrainerFeatureExecutionService.executeAuthoritative"));
        assertTrue(features.contracts().contains("TrainerFeatureTargetResolution"));
        assertTrue(features.contracts().contains("only after an applied effect"));
        assertTrue(features.adapterPolicy().contains("may not grant Features"));
        assertTrue(features.adapterPolicy().contains("select or rewrite targets"));
        assertTrue(features.adapterPolicy().contains("invoke concrete Feature effects"));
        assertTrue(features.adapterPolicy().contains("AP-specific costs remain incomplete"));
        assertTrue(features.adapterPolicy().contains("stops before effect application"));
    }
}

package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.battlecore.IntegrationFeatureCompatibility;
import io.autoptu.cobblemon.battlecore.UpstreamCompatibilityMatrix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricBattleActionCompatibilityTest {
    @Test
    void c2sTransportComposesRequestContractsWithoutExpandingRuntimeTestedPlaybackScope() {
        var shift = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_SHIFT_REQUEST);
        var move = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.MOVE_SELECTION_REQUEST);
        var liveAdapter = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.LIVE_MINECRAFT_BATTLE_ADAPTER);
        var adapterCapability = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

        assertTrue(shift.capabilities().contains(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        assertTrue(shift.capabilities().contains(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        assertFalse(shift.hasBlockingDependency());

        assertTrue(move.capabilities().contains(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        assertTrue(move.capabilities().contains(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR));
        assertTrue(move.capabilities().contains(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        assertFalse(move.hasBlockingDependency());

        assertTrue(liveAdapter.capabilities().contains(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
        assertFalse(liveAdapter.hasBlockingDependency());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapterCapability.support());
        assertTrue(liveAdapter.boundedScope().contains("entity-bound authoritative relocation"));
        assertTrue(liveAdapter.boundedScope().contains("HP projection"));
        assertTrue(liveAdapter.boundedScope().contains("battle-trigger interception"));
        assertTrue(liveAdapter.boundedScope().contains("server-owned WILD encounter provisioning"));
        assertTrue(adapterCapability.contracts().contains("CanonicalWildEncounterBlueprintSource"));
        assertTrue(adapterCapability.contracts().contains("ServerOwnedWildEncounterPreparationService"));
        assertTrue(adapterCapability.contracts().contains("ServerOwnedWildEncounterIdentityBinder"));
        assertTrue(adapterCapability.contracts().contains("ServerOwnedWildEncounterProvisioningService"));
        assertTrue(adapterCapability.adapterPolicy().contains("world/campaign RPG generator"));
        assertTrue(adapterCapability.adapterPolicy().contains("neither the source contract nor provisioner derives species, level, HP, stats, moves, abilities"));
    }
}

package io.autoptu.cobblemon.fabric.network;

import io.autoptu.cobblemon.battlecore.IntegrationFeatureCompatibility;
import io.autoptu.cobblemon.battlecore.UpstreamCompatibilityMatrix;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricBattleActionCompatibilityTest {
    @Test
    void c2sTransportComposesExistingRequestContractsWithoutClaimingLiveAdapterSupport() {
        var shift = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.PLAYER_SHIFT_REQUEST);
        var move = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.MOVE_SELECTION_REQUEST);
        var liveAdapter = IntegrationFeatureCompatibility.requirement(
                IntegrationFeatureCompatibility.Feature.LIVE_MINECRAFT_BATTLE_ADAPTER);

        assertTrue(shift.capabilities().contains(UpstreamCompatibilityMatrix.Capability.CORE_MOVEMENT_LEGALITY));
        assertTrue(shift.capabilities().contains(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        assertFalse(shift.hasBlockingDependency());

        assertTrue(move.capabilities().contains(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        assertTrue(move.capabilities().contains(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR));
        assertTrue(move.capabilities().contains(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        assertFalse(move.hasBlockingDependency());

        assertTrue(liveAdapter.capabilities().contains(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
        assertTrue(liveAdapter.hasBlockingDependency(),
                "compile-tested Fabric networking must not be treated as exercised Minecraft/Cobblemon playback");
    }
}

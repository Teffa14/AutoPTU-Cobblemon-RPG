package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalyticCompatibilityGuardTest {
    @Test
    void pinsCurrentLiveAnalyticContractWithoutPromotingBroadSupport() {
        assertEquals("5eef0c0e8364a4f4a4f8bdb811107895e4cdbe7d", UpstreamCompatibilityMatrix.AUTOPTU_JAVA_SHA);
        assertEquals("cd2d31ab9438713629ad3fc65939e8cc622b5a1f", UpstreamCompatibilityMatrix.AUTOPTU_PYTHON_SHA);

        UpstreamCompatibilityMatrix.Entry actionEconomy = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED, actionEconomy.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, damage.support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, abilities.support());
        assertTrue(actionEconomy.contracts().contains("InitiativeProgressState"));
        assertTrue(damage.contracts().contains("Analytic is live-wired"));
        assertTrue(abilities.contracts().contains("Analytic is live-wired"));
    }

    @Test
    void forbidsMinecraftFromSupplyingAnalyticEligibilityOrBonus() {
        UpstreamCompatibilityMatrix.Entry actionEconomy = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE);
        UpstreamCompatibilityMatrix.Entry damage = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE);
        UpstreamCompatibilityMatrix.Entry abilities = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.ABILITIES);
        UpstreamCompatibilityMatrix.Entry adapter = UpstreamCompatibilityMatrix.entry(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK);

        assertTrue(actionEconomy.adapterPolicy().contains("decide Analytic eligibility"));
        assertTrue(damage.adapterPolicy().contains("decide Analytic eligibility"));
        assertTrue(damage.adapterPolicy().contains("add its +5 damage"));
        assertTrue(abilities.adapterPolicy().contains("independently alter damage/HP"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL, adapter.support());
        assertTrue(adapter.adapterPolicy().contains("populate the world-scoped WILD blueprint registry"));
        assertTrue(adapter.adapterPolicy().contains("neither the registry, source contract nor provisioner derives species, level, HP, stats, moves, abilities"));
    }
}

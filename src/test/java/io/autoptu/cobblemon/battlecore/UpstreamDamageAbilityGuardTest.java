package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UpstreamDamageAbilityGuardTest {
    @Test
    void rngAndAnalyticPostDamageAbilitiesRemainCoreOwned() {
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
        assertTrue(actionEconomy.contracts().contains("Analytic"));
        assertTrue(damage.contracts().contains("PythonRandom"));
        assertTrue(damage.contracts().contains("Adaptability [Errata] and Damp [Errata] are live-wired"));
        assertTrue(damage.contracts().contains("Analytic is live-wired"));
        assertTrue(abilities.contracts().contains("Adaptability [Errata] and Damp [Errata] are live post-damage abilities"));
        assertTrue(abilities.contracts().contains("Analytic is live-wired"));
        assertTrue(damage.adapterPolicy().contains("supply or advance the battle RNG"));
        assertTrue(damage.adapterPolicy().contains("decide Analytic eligibility"));
        assertTrue(damage.adapterPolicy().contains("add its +5 damage"));
        assertTrue(abilities.adapterPolicy().contains("independently alter damage/HP"));
    }
}

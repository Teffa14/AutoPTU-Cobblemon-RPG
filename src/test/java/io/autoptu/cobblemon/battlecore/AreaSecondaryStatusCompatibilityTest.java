package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("b66fcb4dac909c2f44bf6caf54a15f8da82e3e0a",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("231c50e4f2e7c4c0442123b1ba2221b7d07384eb",
                AreaSecondaryStatusCompatibility.AUTOPTU_PYTHON_MAIN_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                AreaSecondaryStatusCompatibility.PINNED_PYTHON_BATTLE_ORACLE_SHA);
    }

    @Test
    void mergedPr210PromotesOnlyAuthoritativeAreaSecondaryStatusProjection() {
        assertEquals(210, AreaSecondaryStatusCompatibility.MERGED_AREA_SECONDARY_STATUS_PR);
        assertTrue(AreaSecondaryStatusCompatibility.authoritativeAreaSecondaryStatusMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.delayedSecondaryStatusMayBeProjected());
    }

    @Test
    void mergedStageContractsRemainFailClosedWithoutLiveRuntimeWiring() {
        assertEquals(216, AreaSecondaryStatusCompatibility.MERGED_EFFECTIVE_ACCURACY_EVASION_PROJECTION_CONTRACT_PR);
        assertEquals(217, AreaSecondaryStatusCompatibility.MERGED_EFFECTIVE_ACCURACY_PROJECTION_PR);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #217 is merged"));
        assertTrue(boundary.contains("EffectiveAccuracyStageProjection"));
        assertTrue(boundary.contains("does not wire the primitive into live hit resolution"));
        assertTrue(boundary.contains("grants no adapter authority"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("231c50e4f2e7c4c0442123b1ba2221b7d07384eb"));
        assertTrue(observation.contains("rival-timeline validation"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages['accuracy']"));
        assertTrue(observation.contains("does not consume combat_stages['evasion']"));
    }
}

package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("38eb8966ecdc2295cabff932ad1f09d3e82ed6f5",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("ad9c202ec9e3982c6797bd38b14df8f647852fc9",
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
        assertEquals(217, AreaSecondaryStatusCompatibility.OPEN_EFFECTIVE_ACCURACY_PROJECTION_PR);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #216 is merged"));
        assertTrue(boundary.contains("Draft PR #217"));
        assertTrue(boundary.contains("does not change live hit resolution"));
        assertTrue(boundary.contains("grants no adapter authority"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("ad9c202ec9e3982c6797bd38b14df8f647852fc9"));
        assertTrue(observation.contains("club-transition validation"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages['accuracy']"));
        assertTrue(observation.contains("does not read combat_stages['evasion']"));
    }
}

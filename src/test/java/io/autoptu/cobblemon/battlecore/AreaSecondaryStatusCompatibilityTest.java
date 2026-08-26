package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("b35f09bbcc4246b1846e57c5c4f9bb5771d474e8",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("5de84c9168da64f0573ad83590d19fd36bf724e2",
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
    void mergedAccuracyContractsRemainFailClosedWithoutLiveHitResolution() {
        assertEquals(218, AreaSecondaryStatusCompatibility.MERGED_INTRINSIC_ACCURACY_OWNERSHIP_PR);
        assertEquals(219, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_CONTRACT_PR);
        assertEquals(220, AreaSecondaryStatusCompatibility.MERGED_RUNTIME_TEMPORARY_ACCURACY_INPUTS_PR);
        assertEquals(221, AreaSecondaryStatusCompatibility.OPEN_ACCURACY_HELPER_OWNERSHIP_PR);
        assertEquals("0746cd60d9be6612bba3712b0a2f08c1d9568abc",
                AreaSecondaryStatusCompatibility.OPEN_ACCURACY_HELPER_OWNERSHIP_PR_HEAD_SHA);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #218"));
        assertTrue(boundary.contains("PR #219"));
        assertTrue(boundary.contains("PR #220"));
        assertTrue(boundary.contains("Draft PR #221"));
        assertTrue(boundary.contains("failing/startup-failed CI"));
        assertTrue(boundary.contains("grants no adapter authority"));
        assertTrue(boundary.contains("live authoritative hit resolution"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("5de84c9168da64f0573ad83590d19fd36bf724e2"));
        assertTrue(observation.contains("Career-side changes"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages['accuracy']"));
        assertTrue(observation.contains("does not consume combat_stages['evasion']"));
    }
}

package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("55bdeb0cb9146054d4d80a0999bcd793275fe140",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("57ee50adfaf1739e1f5d167ce530f1b1a072fe76",
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
        assertEquals(221, AreaSecondaryStatusCompatibility.MERGED_ACCURACY_HELPER_OWNERSHIP_PR);
        assertEquals(222, AreaSecondaryStatusCompatibility.MERGED_FOCUSED_TRAINING_ACCURACY_PR);
        assertEquals(223, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_PROFILE_METADATA_PR);
        assertEquals(224, AreaSecondaryStatusCompatibility.SUPERSEDED_CHRONICLER_PROFILE_MATCH_DIAGNOSTIC_PR);
        assertEquals(225, AreaSecondaryStatusCompatibility.OPEN_CHRONICLER_PROFILE_MATCH_PR);
        assertEquals("d572a6e36866f8baf89eb05b4206c6e118ca24d1",
                AreaSecondaryStatusCompatibility.OPEN_CHRONICLER_PROFILE_MATCH_PR_HEAD_SHA);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("Merged PR #223"));
        assertTrue(boundary.contains("server-owned Chronicler profile metadata"));
        assertTrue(boundary.contains("PR #224 is superseded by draft PR #225"));
        assertTrue(boundary.contains("pure server-side resolver"));
        assertTrue(boundary.contains("targeted_profiling +2 Accuracy"));
        assertTrue(boundary.contains("grants no adapter authority"));
        assertTrue(boundary.contains("live authoritative hit resolution"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("57ee50adfaf1739e1f5d167ce530f1b1a072fe76"));
        assertTrue(observation.contains("Career rivalry-history modifiers"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages['accuracy']"));
        assertTrue(observation.contains("does not consume combat_stages['evasion']"));
    }
}

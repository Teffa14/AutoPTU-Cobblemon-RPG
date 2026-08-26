package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("c5ef1d72c8a997144d215423e2aab60d706905a9",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("2976b6047702d2e86d367fdad3d648e35ced4145",
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
    void mergedAccuracyInputsThroughPr226RemainFailClosedWithoutLiveHitWiring() {
        assertEquals(218, AreaSecondaryStatusCompatibility.MERGED_INTRINSIC_ACCURACY_OWNERSHIP_PR);
        assertEquals(219, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_CONTRACT_PR);
        assertEquals(220, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_RUNTIME_INPUTS_PR);
        assertEquals(221, AreaSecondaryStatusCompatibility.MERGED_FOCUSED_TRAINING_CHRONICLER_CONTRACT_PR);
        assertEquals(222, AreaSecondaryStatusCompatibility.MERGED_FOCUSED_TRAINING_RUNTIME_PR);
        assertEquals(223, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_METADATA_PR);
        assertEquals(225, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_PROFILE_MATCH_PR);
        assertEquals(226, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_ACCURACY_BONUS_PR);

        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.chroniclerAccuracyMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #226 is merged"));
        assertTrue(boundary.contains("server-owned profile matcher"));
        assertTrue(boundary.contains("live Accuracy preparation"));
        assertTrue(boundary.contains("must not run Chronicler profile matching"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("2976b6047702d2e86d367fdad3d648e35ced4145"));
        assertTrue(observation.contains("recovery decision-progress validation"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("_chronicler_profile_matches()"));
        assertTrue(observation.contains("_chronicler_accuracy_bonus()"));
    }
}

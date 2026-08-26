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
        assertEquals("3953a701e8b756fa0f5da7b568cb2fc278d866f7",
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
        assertEquals(224, AreaSecondaryStatusCompatibility.OPEN_CHRONICLER_PROFILE_MATCH_PR);
        assertEquals("42f115e58ec05454f3f5340a9fec02a004527e7e",
                AreaSecondaryStatusCompatibility.OPEN_CHRONICLER_PROFILE_MATCH_PR_HEAD_SHA);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("Merged PR #223"));
        assertTrue(boundary.contains("server-owned Chronicler profile metadata"));
        assertTrue(boundary.contains("Draft PR #224"));
        assertTrue(boundary.contains("runtime state and Accuracy behavior are unchanged"));
        assertTrue(boundary.contains("grants no adapter authority"));
        assertTrue(boundary.contains("live authoritative hit resolution"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("3953a701e8b756fa0f5da7b568cb2fc278d866f7"));
        assertTrue(observation.contains("separate from the pinned battle oracle"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages['accuracy']"));
        assertTrue(observation.contains("does not consume combat_stages['evasion']"));
    }
}
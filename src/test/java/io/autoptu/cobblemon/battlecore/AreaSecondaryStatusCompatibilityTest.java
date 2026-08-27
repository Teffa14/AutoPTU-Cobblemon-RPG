package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("8670b4bf2b423c5d9e43cc9e8d6c979e6c832909",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("c4aff1eb04e7bb27f72b6aaeb55937e7f6c71563",
                AreaSecondaryStatusCompatibility.AUTOPTU_PYTHON_MAIN_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                AreaSecondaryStatusCompatibility.PINNED_PYTHON_BATTLE_ORACLE_SHA);
        assertEquals("a84f924212d6890b3fa92df4d26438a3f54a365a",
                AreaSecondaryStatusCompatibility.DRAFT_EFFECTIVE_ACCURACY_LIVE_HEAD_SHA);
    }

    @Test
    void mergedPr210PromotesOnlyAuthoritativeAreaSecondaryStatusProjection() {
        assertEquals(210, AreaSecondaryStatusCompatibility.MERGED_AREA_SECONDARY_STATUS_PR);
        assertTrue(AreaSecondaryStatusCompatibility.authoritativeAreaSecondaryStatusMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.delayedSecondaryStatusMayBeProjected());
    }

    @Test
    void mergedAccuracyOwnershipThroughPr228AndDraftPr230RemainFailClosedForAdapterMath() {
        assertEquals(218, AreaSecondaryStatusCompatibility.MERGED_INTRINSIC_ACCURACY_OWNERSHIP_PR);
        assertEquals(219, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_CONTRACT_PR);
        assertEquals(220, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_RUNTIME_INPUTS_PR);
        assertEquals(221, AreaSecondaryStatusCompatibility.MERGED_FOCUSED_TRAINING_CHRONICLER_CONTRACT_PR);
        assertEquals(222, AreaSecondaryStatusCompatibility.MERGED_FOCUSED_TRAINING_RUNTIME_PR);
        assertEquals(223, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_METADATA_PR);
        assertEquals(225, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_PROFILE_MATCH_PR);
        assertEquals(226, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_ACCURACY_BONUS_PR);
        assertEquals(227, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_RUNTIME_IDENTITY_PR);
        assertEquals(228, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_RUNTIME_ACCURACY_PR);
        assertEquals(230, AreaSecondaryStatusCompatibility.DRAFT_EFFECTIVE_ACCURACY_LIVE_PR);

        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.chroniclerAccuracyMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #228 derives Chronicler"));
        assertTrue(boundary.contains("Draft PR #230"));
        assertTrue(boundary.contains("not merged"));
        assertTrue(boundary.contains("grants no adapter authority"));
        assertTrue(boundary.contains("must not calculate temporary Accuracy"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("c4aff1eb04e7bb27f72b6aaeb55937e7f6c71563"));
        assertTrue(observation.contains("rollback decision counters"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("stage clamping"));
        assertTrue(observation.contains("temporary Accuracy composition"));
    }
}

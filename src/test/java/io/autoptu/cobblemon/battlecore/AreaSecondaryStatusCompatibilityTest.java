package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("d2d232a4a5be9facbeaeea706081deb93b9c4b7c",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("e9c4173e066da999046818d9ca066bd013f26431",
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
    void mergedAccuracyInputsAndDraftChroniclerBonusRemainFailClosedWithoutLiveHitWiring() {
        assertEquals(218, AreaSecondaryStatusCompatibility.MERGED_INTRINSIC_ACCURACY_OWNERSHIP_PR);
        assertEquals(219, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_CONTRACT_PR);
        assertEquals(220, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_RUNTIME_INPUTS_PR);
        assertEquals(221, AreaSecondaryStatusCompatibility.MERGED_FOCUSED_TRAINING_CHRONICLER_CONTRACT_PR);
        assertEquals(222, AreaSecondaryStatusCompatibility.MERGED_FOCUSED_TRAINING_RUNTIME_PR);
        assertEquals(223, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_METADATA_PR);
        assertEquals(225, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_PROFILE_MATCH_PR);
        assertEquals(226, AreaSecondaryStatusCompatibility.DRAFT_CHRONICLER_ACCURACY_BONUS_PR);
        assertEquals("f3c9eab585a48aeec5fad27aa712dd1d38aa4b3a",
                AreaSecondaryStatusCompatibility.DRAFT_CHRONICLER_ACCURACY_BONUS_SHA);

        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.chroniclerAccuracyMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #225 is merged"));
        assertTrue(boundary.contains("Draft PR #226"));
        assertTrue(boundary.contains("live hit resolution"));
        assertTrue(boundary.contains("must not run Chronicler profile matching"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("e9c4173e066da999046818d9ca066bd013f26431"));
        assertTrue(observation.contains("ranked persistence"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("_chronicler_profile_matches()"));
        assertTrue(observation.contains("_chronicler_accuracy_bonus()"));
    }
}

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
        assertEquals("b77644e64596d40b5d712b261802bde19ae9d806",
                AreaSecondaryStatusCompatibility.AUTOPTU_PYTHON_MAIN_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                AreaSecondaryStatusCompatibility.PINNED_PYTHON_BATTLE_ORACLE_SHA);
        assertEquals("0c972201d5105fab5d5abc1c0ddc42e19b6db23b",
                AreaSecondaryStatusCompatibility.DRAFT_CHRONICLER_PROFILE_MATCH_PR_HEAD_SHA);
    }

    @Test
    void mergedPr210PromotesOnlyAuthoritativeAreaSecondaryStatusProjection() {
        assertEquals(210, AreaSecondaryStatusCompatibility.MERGED_AREA_SECONDARY_STATUS_PR);
        assertTrue(AreaSecondaryStatusCompatibility.authoritativeAreaSecondaryStatusMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.delayedSecondaryStatusMayBeProjected());
    }

    @Test
    void mergedAccuracyContractsRemainFailClosedWithoutLiveHitResolution() {
        assertEquals(217, AreaSecondaryStatusCompatibility.MERGED_EFFECTIVE_ACCURACY_PROJECTION_PR);
        assertEquals(218, AreaSecondaryStatusCompatibility.MERGED_INTRINSIC_ACCURACY_OWNERSHIP_PR);
        assertEquals(219, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_PARITY_PR);
        assertEquals(220, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_INPUTS_PR);
        assertEquals(221, AreaSecondaryStatusCompatibility.MERGED_ACCURACY_HELPER_OWNERSHIP_PR);
        assertEquals(222, AreaSecondaryStatusCompatibility.MERGED_FOCUSED_TRAINING_ACCURACY_PR);
        assertEquals(223, AreaSecondaryStatusCompatibility.MERGED_CHRONICLER_METADATA_PR);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #223 owns canonical Chronicler profile metadata"));
        assertTrue(boundary.contains("still do not compose mutable Accuracy stage"));
        assertTrue(boundary.contains("live hit resolution"));
    }

    @Test
    void draftChroniclerResolverDoesNotGrantAdapterAuthority() {
        assertEquals(224, AreaSecondaryStatusCompatibility.SUPERSEDED_CHRONICLER_MATCH_PR);
        assertEquals(225, AreaSecondaryStatusCompatibility.DRAFT_CHRONICLER_PROFILE_MATCH_PR);
        assertFalse(AreaSecondaryStatusCompatibility.chroniclerAccuracyMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.chroniclerBoundary();
        assertTrue(boundary.contains("PR #224 is superseded"));
        assertTrue(boundary.contains("draft PR #225"));
        assertTrue(boundary.contains("_chronicler_profile_matches()"));
        assertTrue(boundary.contains("does not bind Chronicler metadata into TrainerRuntimeState"));
        assertTrue(boundary.contains("must not perform profile matching"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("b77644e64596d40b5d712b261802bde19ae9d806"));
        assertTrue(observation.contains("timeline numeric evidence"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("_chronicler_profile_matches()"));
        assertTrue(observation.contains("combat_stages['accuracy']"));
        assertTrue(observation.contains("does not consume combat_stages['evasion']"));
    }
}

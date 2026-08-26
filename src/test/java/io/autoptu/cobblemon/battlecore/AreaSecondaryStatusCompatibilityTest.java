package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("cbb57447a387734301b4c9fcc2737c1ecb9c5b66",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("7d8c41d2b94ffbb4a43e832b7c321c36fc9ddf7d",
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
    void mergedAccuracyOwnershipAndDraftBonusContractRemainFailClosedWithoutLiveHitWiring() {
        assertEquals(216, AreaSecondaryStatusCompatibility.MERGED_EFFECTIVE_ACCURACY_EVASION_PROJECTION_CONTRACT_PR);
        assertEquals(217, AreaSecondaryStatusCompatibility.MERGED_EFFECTIVE_ACCURACY_PROJECTION_PR);
        assertEquals(218, AreaSecondaryStatusCompatibility.MERGED_INTRINSIC_ACCURACY_PROFILE_PR);
        assertEquals(219, AreaSecondaryStatusCompatibility.DRAFT_TEMPORARY_ACCURACY_BONUS_CONTRACT_PR);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #218 is merged"));
        assertTrue(boundary.contains("immutable trusted content"));
        assertTrue(boundary.contains("does not wire intrinsic Accuracy into RuntimeMoveResolution"));
        assertTrue(boundary.contains("Draft PR #219"));
        assertTrue(boundary.contains("does not wire them live"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("7d8c41d2b94ffbb4a43e832b7c321c36fc9ddf7d"));
        assertTrue(observation.contains("GitHub Pages deployment scope"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("temporary Accuracy modifiers server-side"));
        assertTrue(observation.contains("does not consume combat_stages['evasion']"));
    }
}

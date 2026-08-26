package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("55cd963b2eda46715b6aba3d1c2579ae1b75b501",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("6f2072d308ee777b5574eb69d08bd23c85af58da",
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
        assertEquals(222, AreaSecondaryStatusCompatibility.OPEN_FOCUSED_TRAINING_ACCURACY_PR);
        assertEquals("3d7509a3d934820293d29ebed1288b26767c1027",
                AreaSecondaryStatusCompatibility.OPEN_FOCUSED_TRAINING_ACCURACY_PR_HEAD_SHA);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("merged PR #221"));
        assertTrue(boundary.contains("Draft PR #222"));
        assertTrue(boundary.contains("canonical controller bindings"));
        assertTrue(boundary.contains("Chronicler remains unported"));
        assertTrue(boundary.contains("grants no adapter authority"));
        assertTrue(boundary.contains("live authoritative hit resolution"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("6f2072d308ee777b5574eb69d08bd23c85af58da"));
        assertTrue(observation.contains("separate from the pinned battle oracle"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages['accuracy']"));
        assertTrue(observation.contains("does not consume combat_stages['evasion']"));
    }
}
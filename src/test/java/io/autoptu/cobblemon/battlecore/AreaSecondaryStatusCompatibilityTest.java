package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("57c7c2a9751cf02facf5d176b9d0f95b996a9bd1",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("ab57fef84387759fa8b959b4bd024c78a7d349bb",
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
    void mergedPr230DoesNotPromoteEffectiveAccuracyWhilePublicMovePathsBypassIt() {
        assertEquals(230, AreaSecondaryStatusCompatibility.MERGED_EFFECTIVE_ACCURACY_RUNTIME_PREPARATION_PR);
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        String boundary = AreaSecondaryStatusCompatibility.accuracyBoundary();
        assertTrue(boundary.contains("PR #230 is merged"));
        assertTrue(boundary.contains("authoritativeStateBoundInput"));
        assertTrue(boundary.contains("actor.accuracyStage() only"));
        assertTrue(boundary.contains("remain fail-closed"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("ab57fef84387759fa8b959b4bd024c78a7d349bb"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
    }
}

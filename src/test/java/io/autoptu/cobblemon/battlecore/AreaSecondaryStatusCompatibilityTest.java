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
        assertEquals("011ba46379255dc2175c08a73c08a7b7e6200176",
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
    void mergedRuntimeAccuracyInputsRemainFailClosedWithoutLiveHitWiring() {
        assertEquals(216, AreaSecondaryStatusCompatibility.MERGED_EFFECTIVE_ACCURACY_EVASION_PROJECTION_CONTRACT_PR);
        assertEquals(217, AreaSecondaryStatusCompatibility.MERGED_EFFECTIVE_ACCURACY_PROJECTION_PR);
        assertEquals(218, AreaSecondaryStatusCompatibility.MERGED_INTRINSIC_ACCURACY_PROFILE_PR);
        assertEquals(219, AreaSecondaryStatusCompatibility.MERGED_TEMPORARY_ACCURACY_BONUS_CONTRACT_PR);
        assertEquals(220, AreaSecondaryStatusCompatibility.MERGED_RUNTIME_TEMPORARY_ACCURACY_INPUTS_PR);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #219 freezes temporary Accuracy bonus semantics"));
        assertTrue(boundary.contains("PR #220 materializes"));
        assertTrue(boundary.contains("BattleRuntimeState"));
        assertTrue(boundary.contains("Focused Training and Chronicler"));
        assertTrue(boundary.contains("does not wire"));
        assertTrue(boundary.contains("live hit resolution"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("011ba46379255dc2175c08a73c08a7b7e6200176"));
        assertTrue(observation.contains("Career leaderboard display-name validation"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("temporary Accuracy modifiers server-side"));
        assertTrue(observation.contains("does not consume combat_stages['evasion']"));
    }
}

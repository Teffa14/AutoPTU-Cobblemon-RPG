package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("a9fb0d81238e69a5263f074b4a8ad8ef1905325d",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("218f272e73acf54e0feb5ac2e8f304d53c0fb3c2",
                AreaSecondaryStatusCompatibility.AUTOPTU_PYTHON_MAIN_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                AreaSecondaryStatusCompatibility.PINNED_PYTHON_BATTLE_ORACLE_SHA);
    }

    @Test
    void mergedPr210PromotesOnlyAuthoritativeAreaSecondaryStatusProjection() {
        assertEquals(210, AreaSecondaryStatusCompatibility.MERGED_AREA_SECONDARY_STATUS_PR);
        assertTrue(AreaSecondaryStatusCompatibility.authoritativeAreaSecondaryStatusMayBeProjected());
        String boundary = AreaSecondaryStatusCompatibility.mergedAreaBoundary();
        assertTrue(boundary.contains("PR #210 is merged"));
        assertTrue(boundary.contains("RuntimeMoveSpecialHooks.standardRegistry"));
        assertTrue(boundary.contains("two Burst targets"));
        assertTrue(boundary.contains("Immunity status_block"));
        assertTrue(boundary.contains("exactly one declaration-level STANDARD/frequency consumption"));
        assertTrue(boundary.contains("every declared upstream dependency remains non-BLOCKING"));
        assertFalse(AreaSecondaryStatusCompatibility.delayedSecondaryStatusMayBeProjected());
    }

    @Test
    void mergedPr211ThroughPr215RemainFailClosedWithoutLiveRuntimeWiring() {
        assertEquals(211, AreaSecondaryStatusCompatibility.MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR);
        assertEquals(212, AreaSecondaryStatusCompatibility.MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR);
        assertEquals(213, AreaSecondaryStatusCompatibility.MERGED_ACCURACY_EVASION_COMBAT_STAGE_CONTRACT_PR);
        assertEquals(214, AreaSecondaryStatusCompatibility.MERGED_SEVEN_COMBAT_STAGE_STATE_PR);
        assertEquals(215, AreaSecondaryStatusCompatibility.MERGED_SEVEN_COMBAT_STAGE_HOOKS_PR);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());
    }

    @Test
    void draftPr216CannotPromoteEffectiveStageArithmetic() {
        assertEquals(216, AreaSecondaryStatusCompatibility.OPEN_EFFECTIVE_ACCURACY_EVASION_PROJECTION_PR);
        assertFalse(AreaSecondaryStatusCompatibility.effectiveAccuracyEvasionArithmeticMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("Draft PR #216"));
        assertTrue(boundary.contains("combat_stages['accuracy']"));
        assertTrue(boundary.contains("does not project combat_stages['evasion']"));
        assertTrue(boundary.contains("changes only the oracle/exported contract"));
        assertTrue(boundary.contains("grants no adapter authority"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("218f272e73acf54e0feb5ac2e8f304d53c0fb3c2"));
        assertTrue(observation.contains("sponsor-history validation"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages"));
        assertTrue(observation.contains("-6..+6"));
        assertTrue(observation.contains("combat_stages['accuracy']"));
        assertTrue(observation.contains("does not read combat_stages['evasion']"));
    }
}

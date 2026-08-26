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
        assertEquals("9fedd2efa5d0f2dc3229617e665533f2f2555897",
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

        assertEquals(8, AreaSecondaryStatusCompatibility.dependencies().size());
        assertTrue(AreaSecondaryStatusCompatibility.dependencies().contains(
                UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        assertTrue(AreaSecondaryStatusCompatibility.dependencies().contains(
                UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        assertTrue(AreaSecondaryStatusCompatibility.dependencies().contains(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE));
        assertTrue(AreaSecondaryStatusCompatibility.dependencies().contains(
                UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE));
        assertTrue(AreaSecondaryStatusCompatibility.dependencies().contains(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR));
        assertTrue(AreaSecondaryStatusCompatibility.dependencies().contains(
                UpstreamCompatibilityMatrix.Capability.ABILITIES));
        assertTrue(AreaSecondaryStatusCompatibility.dependencies().contains(
                UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS));
        assertTrue(AreaSecondaryStatusCompatibility.dependencies().contains(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
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

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #211 is merged"));
        assertTrue(boundary.contains("ordered semantic stage-change requests"));
        assertTrue(boundary.contains("PR #212 is merged"));
        assertTrue(boundary.contains("CombatStageMutationService"));
        assertTrue(boundary.contains("PR #213"));
        assertTrue(boundary.contains("PR #214"));
        assertTrue(boundary.contains("PR #215 is merged"));
        assertTrue(boundary.contains("all seven stages"));
        assertTrue(boundary.contains("Accuracy mutation"));
        assertTrue(boundary.contains("Evasion prevention"));
        assertTrue(boundary.contains("Mirror Armor reflection"));
        assertTrue(boundary.contains("without establishing live BattleRuntime secondary Combat Stage execution"));
        assertTrue(boundary.contains("must not parse stage text"));
        assertTrue(boundary.contains("calculate effective Evasion"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("9fedd2efa5d0f2dc3229617e665533f2f2555897"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages"));
        assertTrue(observation.contains("-6..+6"));
        assertTrue(observation.contains("Accuracy and Evasion"));
        assertTrue(observation.contains("without a five-stat allowlist"));
    }
}

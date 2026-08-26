package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("6b7ceb94001bb4228db81500bedba378ba2b6335",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("b4f3d895bca569e69bf50756694eb65c74e07de7",
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
    void mergedPr211ThroughPr214AndDraftPr215RemainFailClosedWithoutLiveRuntimeWiring() {
        assertEquals(211, AreaSecondaryStatusCompatibility.MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR);
        assertEquals(212, AreaSecondaryStatusCompatibility.MERGED_SECONDARY_COMBAT_STAGE_APPLICATION_PR);
        assertEquals(213, AreaSecondaryStatusCompatibility.MERGED_ACCURACY_EVASION_COMBAT_STAGE_CONTRACT_PR);
        assertEquals(214, AreaSecondaryStatusCompatibility.MERGED_SEVEN_COMBAT_STAGE_STATE_PR);
        assertEquals(215, AreaSecondaryStatusCompatibility.OPEN_SEVEN_COMBAT_STAGE_HOOKS_PR);
        assertEquals("a977d4e55e0f4e84cb3e86afda401be5425ab97b",
                AreaSecondaryStatusCompatibility.OPEN_SEVEN_COMBAT_STAGE_HOOKS_HEAD_SHA);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        assertFalse(AreaSecondaryStatusCompatibility.accuracyEvasionCombatStageMayBeProjected());

        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #211 is merged"));
        assertTrue(boundary.contains("ordered semantic stage-change requests"));
        assertTrue(boundary.contains("PR #212 is merged"));
        assertTrue(boundary.contains("CombatStageMutationService"));
        assertTrue(boundary.contains("PR #213 is merged"));
        assertTrue(boundary.contains("Accuracy and Evasion"));
        assertTrue(boundary.contains("PR #214 is merged"));
        assertTrue(boundary.contains("canonical seven-stage server-owned state"));
        assertTrue(boundary.contains("Draft PR #215"));
        assertTrue(boundary.contains("prevention/post-apply hook contexts"));
        assertTrue(boundary.contains("does not establish live BattleRuntime secondary Combat Stage execution"));
        assertTrue(boundary.contains("must not parse stage text"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("b4f3d895bca569e69bf50756694eb65c74e07de7"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("combat_stages"));
        assertTrue(observation.contains("-6..+6"));
        assertTrue(observation.contains("Accuracy and Evasion"));
        assertTrue(observation.contains("without a five-stat allowlist"));
    }
}

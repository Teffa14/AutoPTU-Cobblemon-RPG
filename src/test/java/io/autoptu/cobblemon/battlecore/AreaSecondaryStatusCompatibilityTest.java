package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("7e1115df0c7937699f179dc1f23040c06c78f719",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("f0b8f2a31ac3626dfb5c51f9ee8195780fd3c560",
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

        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.CORE_TARGETING).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.VERIFIED,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE).support());
        for (UpstreamCompatibilityMatrix.Capability capability : AreaSecondaryStatusCompatibility.dependencies()) {
            assertTrue(UpstreamCompatibilityMatrix.mayProjectAuthoritativeBehavior(capability));
        }
    }

    @Test
    void mergedPr211ParserAndDraftPr212ApplicationRemainFailClosedForProjection() {
        assertEquals(211, AreaSecondaryStatusCompatibility.MERGED_SECONDARY_COMBAT_STAGE_PARSER_PR);
        assertEquals(212, AreaSecondaryStatusCompatibility.OPEN_SECONDARY_COMBAT_STAGE_APPLICATION_PR);
        assertEquals("0b3f005dded417f59175c9424b545580cf654e2e",
                AreaSecondaryStatusCompatibility.OPEN_SECONDARY_COMBAT_STAGE_APPLICATION_HEAD_SHA);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        String boundary = AreaSecondaryStatusCompatibility.combatStageBoundary();
        assertTrue(boundary.contains("PR #211 is merged"));
        assertTrue(boundary.contains("ordered semantic stage-change requests"));
        assertTrue(boundary.contains("does not mutate combat-stage state"));
        assertTrue(boundary.contains("draft PR #212"));
        assertTrue(boundary.contains("CombatStageMutationService"));
        assertTrue(boundary.contains("Accuracy and Evasion fail closed"));
        assertTrue(boundary.contains("does not establish live BattleRuntime execution"));
        assertTrue(boundary.contains("must not parse stage text"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("f0b8f2a31ac3626dfb5c51f9ee8195780fd3c560"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("_generic_post_damage_from_text"));
    }
}

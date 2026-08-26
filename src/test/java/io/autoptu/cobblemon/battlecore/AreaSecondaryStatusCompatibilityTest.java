package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AreaSecondaryStatusCompatibilityTest {
    @Test
    void pinsFreshReadOnlyUpstreamHeadsAndFrozenOracle() {
        assertEquals("453210d46a04ebc52babc675ce7824f83991da5d",
                AreaSecondaryStatusCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("68427feebcc1728fd6bcb53b6520a82595ab956b",
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
        assertTrue(boundary.contains("must not calculate per-target effect rolls"));
        assertFalse(AreaSecondaryStatusCompatibility.delayedSecondaryStatusMayBeProjected());

        assertEquals(6, AreaSecondaryStatusCompatibility.dependencies().size());
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

        for (UpstreamCompatibilityMatrix.Capability capability : AreaSecondaryStatusCompatibility.dependencies()) {
            assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                    CurrentUpstreamCompatibilityInspection.evidence(capability).support());
        }
    }

    @Test
    void draftPr211RemainsFailClosedBecauseItOnlyFreezesSemanticRequests() {
        assertEquals(211, AreaSecondaryStatusCompatibility.OPEN_SECONDARY_COMBAT_STAGE_PR);
        assertEquals("cb2eabaf7d9874c0a4738ddaecc5eb04eb8a4e00",
                AreaSecondaryStatusCompatibility.OPEN_SECONDARY_COMBAT_STAGE_HEAD_SHA);
        assertFalse(AreaSecondaryStatusCompatibility.secondaryCombatStageMayBeProjected());
        String boundary = AreaSecondaryStatusCompatibility.draftCombatStageBoundary();
        assertTrue(boundary.contains("draft PR #211"));
        assertTrue(boundary.contains("ordered semantic stage-change requests"));
        assertTrue(boundary.contains("does not mutate combat-stage state"));
        assertTrue(boundary.contains("CombatStageMutationService"));
        assertTrue(boundary.contains("reference evidence only"));
        assertTrue(boundary.contains("must not parse stage text"));
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = AreaSecondaryStatusCompatibility.pythonOracleObservation();
        assertTrue(observation.contains("68427feebcc1728fd6bcb53b6520a82595ab956b"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("_generic_post_damage_from_text"));
    }
}

package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamDevelopmentWatchTest {
    @Test
    void pinsCurrentReadOnlyUpstreamHeads() {
        assertEquals("b0dc8cc2fc6fba5d1fa3799485545d0c48b6f18a",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("b7f8fbba1221222a61af2fe6a23d047d8b61bcbb",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_PYTHON_MAIN_SHA);
    }

    @Test
    void mergedLivePostDamagePromotesOnlyJavaOwnedPhaseExecution() {
        assertEquals(189, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_POST_DAMAGE_TIMING_PR);
        assertEquals(190, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_REACTION_HANDOFF_PR);
        assertEquals(191, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_LIVE_POST_DAMAGE_PR);
        assertTrue(CurrentUpstreamDevelopmentWatch.postDamageRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.postDamageRuntimeBoundary().contains("#191 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.postDamageRuntimeBoundary().contains("BattleRuntime"));
        assertTrue(CurrentUpstreamDevelopmentWatch.postDamageRuntimeBoundary().contains("after the authoritative move outcome"));
        assertTrue(CurrentUpstreamDevelopmentWatch.postDamageRuntimeBoundary().contains("END_ACTION"));

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE)
                .contracts().contains("POST_DAMAGE"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE)
                .limitation().contains("END_ACTION"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK)
                .contracts().contains("POST_DAMAGE"));
    }

    @Test
    void openEndActionBridgeRemainsFailClosedForAdapterAuthority() {
        assertEquals(192, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_OPEN_END_ACTION_BRIDGE_PR);
        assertFalse(CurrentUpstreamDevelopmentWatch.endActionRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("#192 is open and draft"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("defender is absent"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("last per-target result"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("total action damage"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("does not wire END_ACTION"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("failing workflow checks"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR)
                .limitation().contains("END_ACTION"));
    }

    @Test
    void currentPythonCareerChangesDoNotReplaceFrozenBattleOracle() {
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("Career save-isolation"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("defender=None"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("result=last_result"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("damage_dealt=total_damage_dealt"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR)
                .contracts().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
    }
}

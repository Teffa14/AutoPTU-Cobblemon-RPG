package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamDevelopmentWatchTest {
    @Test
    void pinsCurrentReadOnlyUpstreamHeads() {
        assertEquals("2c83099de0f558a6e387f39174c0223f8e1668e6",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("05363c11b0a174ef8ffee89e94ceb6273766f3d9",
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
    void mergedEndActionBridgeRemainsFailClosedUntilRuntimeWiringExists() {
        assertEquals(192, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_BRIDGE_PR);
        assertEquals(193, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_OPEN_END_ACTION_ACCUMULATOR_PR);
        assertEquals("1f8b2d677f6c257aed3c1821199adeb549dc7fbb",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_OPEN_END_ACTION_ACCUMULATOR_HEAD_SHA);
        assertFalse(CurrentUpstreamDevelopmentWatch.endActionRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("#192 is merged on main"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("Draft PR #193"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("package-private action accumulator"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("current CI is green"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("does not wire END_ACTION"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("must not dispatch it once per defender"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionDevelopmentBoundary().contains("aggregate damage itself"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR)
                .limitation().contains("END_ACTION"));
    }

    @Test
    void currentPythonCareerChangesDoNotReplaceFrozenBattleOracle() {
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("Career leaderboard hardening"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("defender=None"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("result=last_result"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("damage_dealt=total_damage_dealt"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("immutable_mind:true"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("initial total_damage_dealt=0"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR)
                .contracts().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
    }
}

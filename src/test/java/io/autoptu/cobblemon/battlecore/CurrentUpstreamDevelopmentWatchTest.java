package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamDevelopmentWatchTest {
    @Test
    void pinsCurrentReadOnlyUpstreamHeads() {
        assertEquals("ebdbcdc58c41bae72e9264e8f508338be95e2295",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("0bff7521ecb8b1163cbd5f366dea4651de83c353",
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
    void mergedEndActionContractsRemainFailClosedWithoutRuntimeCallSite() {
        assertEquals(192, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_BRIDGE_PR);
        assertEquals(193, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_ACCUMULATOR_PR);
        assertEquals(194, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_FINALIZATION_PR);
        assertEquals(195, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_TARGET_RESULT_TRANSPORT_PR);
        assertEquals(196, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_ORACLE_PR);
        assertFalse(CurrentUpstreamDevelopmentWatch.endActionRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("MoveSpecialActionFinalization"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("target-loop aggregation oracle"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("exactly once"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("share the target loop"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("No authoritative BattleRuntime call site"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("must not aggregate target results"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("dispatch END_ACTION"));

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE)
                .limitation().contains("END_ACTION"));
    }

    @Test
    void openEffectRollResolverCannotPromoteRuntimeAuthority() {
        assertEquals(197, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_OPEN_EFFECT_ROLL_RESOLVER_PR);
        assertEquals("baaf16fc90fa2008841ee282b39fe70268488267",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_OPEN_EFFECT_ROLL_RESOLVER_HEAD_SHA);
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.openEffectRollResolverBoundary().contains("draft PR #197"));
        assertTrue(CurrentUpstreamDevelopmentWatch.openEffectRollResolverBoundary().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(CurrentUpstreamDevelopmentWatch.openEffectRollResolverBoundary().contains("resolver-only"));
        assertTrue(CurrentUpstreamDevelopmentWatch.openEffectRollResolverBoundary().contains("runtime state derivation"));
        assertTrue(CurrentUpstreamDevelopmentWatch.openEffectRollResolverBoundary().contains("must not calculate or supply final effect rolls"));

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_PERKS).support());
    }

    @Test
    void currentPythonCareerChangesDoNotReplaceFrozenBattleOracle() {
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("Career Web presentation and outcome-handling commits"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("PRE_DAMAGE, POST_DAMAGE and END_ACTION"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("once after target processing"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR)
                .contracts().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
    }
}

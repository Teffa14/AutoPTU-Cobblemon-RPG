package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamDevelopmentWatchTest {
    @Test
    void pinsCurrentReadOnlyUpstreamHeads() {
        assertEquals("412ec8f82c7dd4cb89e58e4db80b3e9d957b5bb4",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("df327530562ce4315f523316239d80a917111078",
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
    }

    @Test
    void mergedEndActionContractsRemainFailClosedWithoutRuntimeCallSite() {
        assertEquals(192, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_BRIDGE_PR);
        assertEquals(193, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_ACCUMULATOR_PR);
        assertEquals(194, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_FINALIZATION_PR);
        assertEquals(195, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_TARGET_RESULT_TRANSPORT_PR);
        assertEquals(196, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_END_ACTION_ORACLE_PR);
        assertFalse(CurrentUpstreamDevelopmentWatch.endActionRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("No authoritative BattleRuntime call site"));
        assertTrue(CurrentUpstreamDevelopmentWatch.endActionRuntimeBoundary().contains("must not aggregate target results"));
    }

    @Test
    void mergedEffectRollResolverAndTemporaryStateRemainFailClosed() {
        assertEquals(197, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_RESOLVER_PR);
        assertEquals(198, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_TEMP_STATE_PR);
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.effectRollResolverBoundary().contains("PR #197 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.effectRollResolverBoundary().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(CurrentUpstreamDevelopmentWatch.effectRollResolverBoundary().contains("must not calculate final effect rolls"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollTemporaryStateBoundary().contains("PR #198 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollTemporaryStateBoundary().contains("does not give Minecraft authority"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.ABILITIES).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.TRAINER_FEATURES_AND_PERKS).support());
    }

    @Test
    void mergedCanonicalEffectsTextRemainsServerOwnedAndDoesNotPromoteRuntime() {
        assertEquals(199, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_MOVE_EFFECTS_TEXT_PR);
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedMoveEffectsTextBoundary().contains("PR #199 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedMoveEffectsTextBoundary().contains("server-owned MoveSpec"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedMoveEffectsTextBoundary().contains("must not send rules text"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void mergedStackedStatStratagemParityStillDoesNotPromoteAdapterAuthority() {
        assertEquals(200, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_STAT_STRATAGEM_STACKS_PR);
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollStatStratagemStacksBoundary().contains("PR #200 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollStatStratagemStacksBoundary().contains("must not count Stat Stratagem effects"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void mergedRollPenaltyStateRemainsFailClosedWithoutLiveEffectRollConsumer() {
        assertEquals(201, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_PENALTY_STATE_PR);
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollPenaltyStateBoundary().contains("PR #201 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollPenaltyStateBoundary().contains("must not read, expire, sum or clamp"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void mergedHardenedCritEffectBonusRemainsServerOwnedAndFailClosed() {
        assertEquals(202, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_HARDENED_CRIT_EFFECT_BONUS_PR);
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("PR #202 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("server-owned semantic state"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("must not infer injuries"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void mergedRuntimeEffectRollInputsRemoveCallerModifierAuthorityButDoNotPromoteConsumer() {
        assertEquals(203, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_RUNTIME_INPUTS_PR);
        String boundary = CurrentUpstreamDevelopmentWatch.mergedEffectRollRuntimeInputsBoundary();
        assertTrue(boundary.contains("PR #203 is merged"));
        assertTrue(boundary.contains("BattleRuntimeState"));
        assertTrue(boundary.contains("server-owned"));
        assertTrue(boundary.contains("must not reconstruct these modifiers"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void mergedSecondaryStatusParserRemainsRequestOnlyAndFailClosed() {
        assertEquals(204, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_SECONDARY_STATUS_CONTRACT_PR);
        String boundary = CurrentUpstreamDevelopmentWatch.mergedSecondaryStatusContractBoundary();
        assertTrue(boundary.contains("PR #204 is merged"));
        assertTrue(boundary.contains("7cd765e87fa4254789eb40e8d14f91e1251631ad"));
        assertTrue(boundary.contains("_generic_post_damage_from_text"));
        assertTrue(boundary.contains("returns ordered status requests without applying them"));
        assertTrue(boundary.contains("must not parse effects text"));
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).support());
    }

    @Test
    void mergedSecondaryStatusApplicationStillDoesNotPromoteLiveAdapterAuthority() {
        assertEquals(205, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_SECONDARY_STATUS_APPLICATION_PR);
        String boundary = CurrentUpstreamDevelopmentWatch.mergedSecondaryStatusApplicationBoundary();
        assertTrue(boundary.contains("PR #205 is merged"));
        assertTrue(boundary.contains("d64d6417dc89c1aca878d0a8fd6b526921b8e193"));
        assertTrue(boundary.contains("StatusApplicationResolution"));
        assertTrue(boundary.contains("BuiltinStatusApplicationHooks"));
        assertTrue(boundary.contains("must not apply requested statuses"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).support());
    }

    @Test
    void mergedAccuracyRollTransportRemainsServerOwned() {
        assertEquals(206, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_ACCURACY_ROLL_TRANSPORT_PR);
        String boundary = CurrentUpstreamDevelopmentWatch.mergedAccuracyRollTransportBoundary();
        assertTrue(boundary.contains("PR #206 is merged"));
        assertTrue(boundary.contains("f6115543da34bae91353c302a635913906656c2a"));
        assertTrue(boundary.contains("server-owned accuracy d20"));
        assertTrue(boundary.contains("must not generate, inject or infer"));
    }

    @Test
    void mergedRuntimeSecondaryStatusBridgeRemainsFailClosedWithoutLivePostDamageInvocation() {
        assertEquals(207, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_RUNTIME_SECONDARY_STATUS_BRIDGE_PR);
        String boundary = CurrentUpstreamDevelopmentWatch.mergedRuntimeSecondaryStatusBridgeBoundary();
        assertTrue(boundary.contains("PR #207 is merged"));
        assertTrue(boundary.contains("d365642c74b43592073a7cc07bb3e011aaa503a9"));
        assertTrue(boundary.contains("package-private runtime bridge"));
        assertTrue(boundary.contains("Serene Grace"));
        assertTrue(boundary.contains("Immunity"));
        assertTrue(boundary.contains("not yet invoked by BattleRuntime POST_DAMAGE"));
        assertTrue(boundary.contains("must not call an equivalent bridge"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.COMPLETE_STATUS_LIFECYCLE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
    }

    @Test
    void mergedLiveAccuracyRollClosesTransportButNotSecondaryStatusRuntime() {
        assertEquals(208, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_LIVE_ACCURACY_ROLL_PR);
        String boundary = CurrentUpstreamDevelopmentWatch.mergedLiveAccuracyRollBoundary();
        assertTrue(boundary.contains("PR #208 is merged"));
        assertTrue(boundary.contains("412ec8f82c7dd4cb89e58e4db80b3e9d957b5bb4"));
        assertTrue(boundary.contains("BattleRuntime"));
        assertTrue(boundary.contains("authoritative accuracy d20"));
        assertTrue(boundary.contains("closes roll transport only"));
        assertTrue(boundary.contains("no secondary-status POST_DAMAGE invocation"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void currentPythonHeadDoesNotReplaceFrozenBattleOracle() {
        String observation = CurrentUpstreamDevelopmentWatch.pythonMainObservation();
        assertTrue(observation.contains("df327530562ce4315f523316239d80a917111078"));
        assertTrue(observation.contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(observation.contains("battle._apply_status"));
        assertTrue(observation.contains("shared move-result roll"));
        assertTrue(observation.contains("reference-only"));
    }
}

package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamDevelopmentWatchTest {
    @Test
    void pinsCurrentReadOnlyUpstreamHeads() {
        assertEquals("215967c224e3dcd73e06d47e9e4bad3153a96d8c",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("e91fc167dd2e9b5f7c94a22da76dfba5e103d7da",
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
    void mergedEffectRollResolverAndTemporaryStateRemainFailClosed() {
        assertEquals(197, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_RESOLVER_PR);
        assertEquals(198, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_TEMP_STATE_PR);
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.effectRollResolverBoundary().contains("PR #197 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.effectRollResolverBoundary().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(CurrentUpstreamDevelopmentWatch.effectRollResolverBoundary().contains("resolver-only"));
        assertTrue(CurrentUpstreamDevelopmentWatch.effectRollResolverBoundary().contains("runtime state derivation"));
        assertTrue(CurrentUpstreamDevelopmentWatch.effectRollResolverBoundary().contains("must not calculate or supply final effect rolls"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollTemporaryStateBoundary().contains("PR #198 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollTemporaryStateBoundary().contains("3d9be13bfd3c89361e58c35e2df6a3265b57f93b"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollTemporaryStateBoundary().contains("immutable_mind_block"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollTemporaryStateBoundary().contains("early blocks can prevent later cleanup"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollTemporaryStateBoundary().contains("must continue to fail closed"));

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
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedMoveEffectsTextBoundary().contains("3825f32490c405a3d541c5eddf4b04097b4d1e69"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedMoveEffectsTextBoundary().contains("server-owned MoveSpec"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedMoveEffectsTextBoundary().contains("server-owned canonical fallback"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedMoveEffectsTextBoundary().contains("must not send rules text"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void mergedStackedStatStratagemParityStillDoesNotPromoteAdapterAuthority() {
        assertEquals(200, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_STAT_STRATAGEM_STACKS_PR);
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollStatStratagemStacksBoundary().contains("PR #200 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollStatStratagemStacksBoundary().contains("3dbfc85605c03e4f8e6aeb1f4195e0fdb412556a"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollStatStratagemStacksBoundary().contains("every matching stat_stratagem"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollStatStratagemStacksBoundary().contains("live Stat Stratagem state"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollStatStratagemStacksBoundary().contains("must not count Stat Stratagem effects"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void mergedRollPenaltyStateRemainsFailClosedWithoutLiveEffectRollConsumer() {
        assertEquals(201, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_EFFECT_ROLL_PENALTY_STATE_PR);
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollPenaltyStateBoundary().contains("PR #201 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollPenaltyStateBoundary().contains("eb34ad6b3e2691e6192e8f489611bec0bb144f0d"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollPenaltyStateBoundary().contains("all_roll_penalty"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollPenaltyStateBoundary().contains("same-round entries remain"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollPenaltyStateBoundary().contains("clamped at zero"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedEffectRollPenaltyStateBoundary().contains("must not read, expire, sum or clamp"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());
    }

    @Test
    void mergedHardenedCritEffectBonusRemainsServerOwnedAndFailClosed() {
        assertEquals(202, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MERGED_HARDENED_CRIT_EFFECT_BONUS_PR);
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("PR #202 is merged"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("215967c224e3dcd73e06d47e9e4bad3153a96d8c"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("Press On!"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("Intimidate rank"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("server-owned semantic state"));
        assertTrue(CurrentUpstreamDevelopmentWatch.mergedHardenedCritEffectBonusBoundary().contains("must not infer injuries"));
        assertFalse(CurrentUpstreamDevelopmentWatch.effectRollRuntimeMayBePromoted());

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
    void currentPythonHeadDoesNotReplaceFrozenEffectRollOracle() {
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("e91fc167dd2e9b5f7c94a22da76dfba5e103d7da"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("_roll_penalty"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("Hardened/Press On!"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("reference-only"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR)
                .contracts().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
    }
}

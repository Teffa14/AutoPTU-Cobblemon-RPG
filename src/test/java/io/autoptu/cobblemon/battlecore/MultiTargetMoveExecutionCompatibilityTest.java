package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class MultiTargetMoveExecutionCompatibilityTest {
    @Test
    void mergedRuntimePromotesProjectionButNotAdapterSideExecution() {
        assertTrue(MultiTargetMoveExecutionCompatibility.ownershipContractIsParityBacked());
        assertTrue(MultiTargetMoveExecutionCompatibility.javaExecutesAuthoritativeMultiTargetDamage());
        assertFalse(MultiTargetMoveExecutionCompatibility.minecraftMayExecuteMultiTargetDamage());
        assertTrue(MultiTargetMoveExecutionCompatibility.minecraftMayProjectResolvedMultiTargetEvents());
    }

    @Test
    void pinsMergedRuntimeAndCurrentReadOnlyPythonInspection() {
        assertEquals("edf8db216ab88a10b896f2bb144cf5d08de49d8e",
                MultiTargetMoveExecutionCompatibility.INSPECTED_AUTOPTU_JAVA_SHA);
        assertEquals("0d56ea7b5a2b99a96f7ac4ca40b405e0ffbf83b8",
                MultiTargetMoveExecutionCompatibility.INSPECTED_AUTOPTU_PYTHON_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                MultiTargetMoveExecutionCompatibility.PYTHON_ORACLE_PIN_SHA);
    }

    @Test
    void featureMapsToConcreteUpstreamCapabilityContracts() {
        Map<UpstreamCompatibilityMatrix.Capability, String> dependencies =
                MultiTargetMoveExecutionCompatibility.dependencies();

        assertEquals(5, dependencies.size());
        assertTrue(dependencies.containsKey(UpstreamCompatibilityMatrix.Capability.CORE_TARGETING));
        assertTrue(dependencies.containsKey(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE));
        assertTrue(dependencies.containsKey(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE));
        assertTrue(dependencies.containsKey(UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR));
        assertTrue(dependencies.containsKey(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK));
        assertTrue(dependencies.get(UpstreamCompatibilityMatrix.Capability.ACTION_ECONOMY_AND_INITIATIVE)
                .contains("exactly once"));
        assertTrue(dependencies.get(UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE)
                .contains("applyAreaUsingAuthoritativeCombatState"));
    }

    @Test
    void adapterPolicyKeepsEveryPtuDecisionUpstream() {
        String policy = MultiTargetMoveExecutionCompatibility.adapterPolicy();
        assertTrue(policy.contains("authoritative multi-target TILE execution"));
        assertTrue(policy.contains("may project the ordered semantic BattleEvents"));
        assertTrue(policy.contains("must not loop targets to execute PTU effects"));
        assertTrue(policy.contains("roll accuracy or damage"));
        assertTrue(policy.contains("mutate HP/history"));
        assertTrue(policy.contains("Telepathy"));
        assertTrue(policy.contains("forced-movement semantics remain disabled"));
    }
}

package io.autoptu.cobblemon.battlecore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

final class MultiTargetMoveExecutionCompatibilityTest {
    @Test
    void frozenOwnershipContractDoesNotPromoteRuntimeExecution() {
        assertTrue(MultiTargetMoveExecutionCompatibility.ownershipContractIsParityBacked());
        assertFalse(MultiTargetMoveExecutionCompatibility.javaExecutesAuthoritativeMultiTargetDamage());
        assertFalse(MultiTargetMoveExecutionCompatibility.minecraftMayExecuteMultiTargetDamage());
        assertFalse(MultiTargetMoveExecutionCompatibility.minecraftMayProjectResolvedMultiTargetEvents());
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
                .contains("once"));
    }

    @Test
    void adapterPolicyExplicitlyDefersPtuRules() {
        String policy = MultiTargetMoveExecutionCompatibility.adapterPolicy();
        assertTrue(policy.contains("does not yet provide completed authoritative multi-target damage execution"));
        assertTrue(policy.contains("must not loop over affected targets"));
        assertTrue(policy.contains("check or record move frequency"));
        assertTrue(policy.contains("Keep AoE execution disabled"));
        assertTrue(policy.contains("without re-running PTU legality or bookkeeping"));
    }
}

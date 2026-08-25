package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CurrentUpstreamDevelopmentWatchTest {
    @Test
    void pinsCurrentReadOnlyUpstreamHeads() {
        assertEquals("3caac611a987322a70dbdc34c56d613b96dadb92",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("6affb828cc72ab76e6952847e2275d14d166d216",
                CurrentUpstreamDevelopmentWatch.AUTOPTU_PYTHON_MAIN_SHA);
    }

    @Test
    void openPostDamageTimingWorkCannotPromoteMinecraftAuthority() {
        assertEquals(189, CurrentUpstreamDevelopmentWatch.AUTOPTU_JAVA_POST_DAMAGE_TIMING_PR);
        assertFalse(CurrentUpstreamDevelopmentWatch.postDamageRuntimeMayBePromoted());
        assertTrue(CurrentUpstreamDevelopmentWatch.postDamageRuntimeBlocker().contains("open"));
        assertTrue(CurrentUpstreamDevelopmentWatch.postDamageRuntimeBlocker().contains("not part of main"));
        assertTrue(CurrentUpstreamDevelopmentWatch.postDamageRuntimeBlocker().contains("fail-closed"));

        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE).support());
        assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                CurrentUpstreamCompatibilityInspection.evidence(
                        UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR).support());
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.FULL_STATEFUL_DAMAGE_PIPELINE)
                .limitation().contains("POST_DAMAGE and END_ACTION runtime parity remain incomplete"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MINECRAFT_COBBLEMON_CRAFTICS_ADAPTER_PLAYBACK)
                .limitation().contains("POST_DAMAGE/END_ACTION"));
    }

    @Test
    void currentPythonCareerChangesDoNotReplaceFrozenBattleOracle() {
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("Career sponsor-settlement"));
        assertTrue(CurrentUpstreamDevelopmentWatch.pythonMainObservation().contains("does not replace"));
        assertTrue(CurrentUpstreamCompatibilityInspection.evidence(
                UpstreamCompatibilityMatrix.Capability.MOVE_SPECIFIC_BEHAVIOR)
                .contracts().contains("16d228efa63aabecb67fa788959a359aac7f8f03"));
    }
}

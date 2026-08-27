package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusControllerPhaseEnvelopeCompatibilityTest {
    @Test
    void mergedOrderingAndDraftDispatcherDoNotPromoteMinecraftExecutionAuthority() {
        assertEquals(231, StatusControllerPhaseEnvelopeCompatibility.MERGED_ORDERING_PR);
        assertEquals(232, StatusControllerPhaseEnvelopeCompatibility.DRAFT_DISPATCHER_PR);
        assertEquals("84505214d4bca41610f36f0a178e675ef0ab26ba",
                StatusControllerPhaseEnvelopeCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("fcbded4c966095c24a4f6124a435edcb790f8581",
                StatusControllerPhaseEnvelopeCompatibility.AUTOPTU_JAVA_DRAFT_HEAD_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                StatusControllerPhaseEnvelopeCompatibility.PINNED_PYTHON_ORACLE_SHA);
        assertFalse(StatusControllerPhaseEnvelopeCompatibility.mayProjectOrExecutePhaseEnvelope());

        String boundary = StatusControllerPhaseEnvelopeCompatibility.boundary();
        assertTrue(boundary.contains("PR #231 is merged"));
        assertTrue(boundary.contains("Draft PR #232"));
        assertTrue(boundary.contains("START held-item start -> food regen -> food buff start -> combatant phase effects"));
        assertTrue(boundary.contains("END combatant phase effects -> held-item end"));
        assertTrue(boundary.contains("COMMAND/ACTION combatant phase effects only"));
        assertTrue(boundary.contains("may not coordinate PTU phase order"));
        assertTrue(boundary.contains("concrete held-item and food effects unported"));
    }

    @Test
    void phaseEnvelopeDependenciesStayPartialRatherThanImplyingCompleteLibraries() {
        assertEquals(6, StatusControllerPhaseEnvelopeCompatibility.dependencies().size());
        for (UpstreamCompatibilityMatrix.Capability capability : StatusControllerPhaseEnvelopeCompatibility.dependencies()) {
            assertEquals(UpstreamCompatibilityMatrix.Support.PARTIAL,
                    UpstreamCompatibilityMatrix.entry(capability).support(), capability.name());
        }
    }
}

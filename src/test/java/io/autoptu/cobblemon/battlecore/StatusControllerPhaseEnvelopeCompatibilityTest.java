package io.autoptu.cobblemon.battlecore;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatusControllerPhaseEnvelopeCompatibilityTest {
    @Test
    void draftOrderingContractDoesNotPromoteMinecraftExecutionAuthority() {
        assertEquals(231, StatusControllerPhaseEnvelopeCompatibility.AUTOPTU_JAVA_PR);
        assertEquals("57c7c2a9751cf02facf5d176b9d0f95b996a9bd1",
                StatusControllerPhaseEnvelopeCompatibility.AUTOPTU_JAVA_MAIN_SHA);
        assertEquals("74d9e31fcb390531f8837f41985f81923506bcc9",
                StatusControllerPhaseEnvelopeCompatibility.AUTOPTU_JAVA_PR_HEAD_SHA);
        assertEquals("16d228efa63aabecb67fa788959a359aac7f8f03",
                StatusControllerPhaseEnvelopeCompatibility.PINNED_PYTHON_ORACLE_SHA);
        assertFalse(StatusControllerPhaseEnvelopeCompatibility.mayProjectOrExecutePhaseEnvelope());

        String boundary = StatusControllerPhaseEnvelopeCompatibility.boundary();
        assertTrue(boundary.contains("draft/open"));
        assertTrue(boundary.contains("START held-item start -> food regen -> food buff start -> combatant phase effects"));
        assertTrue(boundary.contains("END combatant phase effects -> held-item end"));
        assertTrue(boundary.contains("COMMAND/ACTION combatant phase effects only"));
        assertTrue(boundary.contains("may not coordinate PTU phase order"));
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

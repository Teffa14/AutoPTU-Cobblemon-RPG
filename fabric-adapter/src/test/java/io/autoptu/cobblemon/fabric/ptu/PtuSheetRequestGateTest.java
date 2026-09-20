package io.autoptu.cobblemon.fabric.ptu;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class PtuSheetRequestGateTest {
    @Test void limitsEachPlayerIndependentlyWithoutExtendingPenaltyOnSpam() {
        var gate = new PtuSheetRequestGate();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        assertTrue(gate.allow(a, 0));
        assertFalse(gate.allow(a, 199));
        assertTrue(gate.allow(b, 199));
        assertTrue(gate.allow(a, 200));
    }
    @Test void disconnectAndServerStopReleaseMemoryAndLimits() {
        var gate = new PtuSheetRequestGate();
        UUID a = UUID.randomUUID();
        assertTrue(gate.allow(a, 0));
        gate.remove(a);
        assertTrue(gate.allow(a, 1));
        gate.clear();
        assertTrue(gate.allow(a, 2));
    }
}

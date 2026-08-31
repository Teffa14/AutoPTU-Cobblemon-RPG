package io.autoptu.cobblemon.fabric.presentation;

import io.autoptu.cobblemon.battlecore.BattleEventPlaybackEnvelope;
import io.autoptu.cobblemon.battlecore.BattlePlaybackBatch;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class FabricSemanticBattleTraceTest {
    @AfterEach
    void clear() {
        FabricSemanticBattleTrace.clearAllForTests();
    }

    @Test
    void preservesAuthoritativeOrderAndDeduplicatesOverlappingReplay() {
        BattleEventPlaybackEnvelope first = event(4, "move_resolved", "move_resolved|actor=a|move=tackle");
        BattleEventPlaybackEnvelope second = event(5, "shift_resolved", "shift_resolved|actor=b|to=2,1");

        FabricSemanticBattleTrace.record(new BattlePlaybackBatch("reservation-1", List.of(first, second)));
        FabricSemanticBattleTrace.record(new BattlePlaybackBatch("reservation-1", List.of(second)));

        assertEquals(List.of(first, second), FabricSemanticBattleTrace.snapshot("reservation-1"));
        assertEquals(2, FabricSemanticBattleTrace.eventCount("reservation-1"));
    }

    @Test
    void rejectsConflictingHistoryAtSameAuthoritativeSequence() {
        FabricSemanticBattleTrace.record(new BattlePlaybackBatch(
                "reservation-2",
                List.of(event(7, "turn_start", "turn_start|actor=a"))));

        IllegalStateException error = assertThrows(IllegalStateException.class, () ->
                FabricSemanticBattleTrace.record(new BattlePlaybackBatch(
                        "reservation-2",
                        List.of(event(7, "turn_start", "turn_start|actor=b")))));

        assertEquals("conflicting authoritative battle event at sequence 7", error.getMessage());
    }

    @Test
    void releaseRemovesReservationScopedEvidence() {
        FabricSemanticBattleTrace.record(new BattlePlaybackBatch(
                "reservation-3",
                List.of(event(1, "phase", "phase|name=round_start"))));

        FabricSemanticBattleTrace.release("reservation-3");

        assertEquals(List.of(), FabricSemanticBattleTrace.snapshot("reservation-3"));
    }

    private static BattleEventPlaybackEnvelope event(long sequence, String kind, String stableKey) {
        return new BattleEventPlaybackEnvelope(sequence, kind, stableKey, Map.of("source", "autoptu-java"));
    }
}

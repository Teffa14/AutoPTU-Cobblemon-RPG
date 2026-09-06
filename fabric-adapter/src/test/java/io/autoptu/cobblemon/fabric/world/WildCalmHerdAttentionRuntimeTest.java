package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildCalmHerdAttentionRuntimeTest {
    @Test
    void deterministicAnchorUsesStableServerActorIdentity() {
        UUID high = UUID.fromString("00000000-0000-0000-0000-0000000000ff");
        UUID low = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID middle = UUID.fromString("00000000-0000-0000-0000-000000000080");

        assertEquals(low, WildCalmHerdAttentionRuntime.deterministicAnchorIdentity(
                List.of(high, low, middle)).orElseThrow());
        assertEquals(low, WildCalmHerdAttentionRuntime.deterministicAnchorIdentity(
                List.of(middle, high, low)).orElseThrow());
    }

    @Test
    void emptyPopulationHasNoPresentationAnchor() {
        assertTrue(WildCalmHerdAttentionRuntime.deterministicAnchorIdentity(List.of()).isEmpty());
        assertTrue(WildCalmHerdAttentionRuntime.deterministicAnchorIdentity(null).isEmpty());
    }
}

package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildInteractionFocusMarkerRuntimeTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Test
    void markerAppearsOnlyWhenServerFocusEntersOrChangesActor() {
        assertTrue(WildInteractionFocusMarkerRuntime.shouldMark(null, FIRST));
        assertFalse(WildInteractionFocusMarkerRuntime.shouldMark(FIRST, FIRST));
        assertTrue(WildInteractionFocusMarkerRuntime.shouldMark(FIRST, SECOND));
        assertFalse(WildInteractionFocusMarkerRuntime.shouldMark(FIRST, null));
        assertFalse(WildInteractionFocusMarkerRuntime.shouldMark(null, null));
    }
}

package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildInteractionFocusMarkerRuntimeTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Test
    void markerAppearsImmediatelyThenRepeatsWhileServerFocusRemainsValid() {
        assertTrue(WildInteractionFocusMarkerRuntime.shouldMark(null, FIRST, 100L));

        var focused = new WildInteractionFocusMarkerRuntime.FocusMarkerState(FIRST, 100L);
        assertFalse(WildInteractionFocusMarkerRuntime.shouldMark(focused, FIRST, 130L));
        assertTrue(WildInteractionFocusMarkerRuntime.shouldMark(
                focused,
                FIRST,
                100L + WildInteractionFocusMarkerRuntime.REPEAT_MARKER_INTERVAL_TICKS));
        assertTrue(WildInteractionFocusMarkerRuntime.shouldMark(focused, SECOND, 110L));
        assertFalse(WildInteractionFocusMarkerRuntime.shouldMark(focused, null, 200L));
    }

    @Test
    void markerStateKeepsOriginalPulseTimeUntilAnotherPulseActuallyEmits() {
        var focused = new WildInteractionFocusMarkerRuntime.FocusMarkerState(FIRST, 100L);
        assertSame(focused, WildInteractionFocusMarkerRuntime.nextState(focused, FIRST, 120L, false));

        var repeated = WildInteractionFocusMarkerRuntime.nextState(focused, FIRST, 140L, true);
        assertEquals(FIRST, repeated.actorId());
        assertEquals(140L, repeated.lastMarkerTick());

        var changed = WildInteractionFocusMarkerRuntime.nextState(focused, SECOND, 125L, true);
        assertEquals(SECOND, changed.actorId());
        assertEquals(125L, changed.lastMarkerTick());
        assertNull(WildInteractionFocusMarkerRuntime.nextState(focused, null, 160L, false));
    }

    @Test
    void authoredAlphaRoleUsesDistinctPresentationStyleWithoutCombatInference() {
        assertEquals(
                WildInteractionFocusMarkerRuntime.MarkerStyle.MEMBER,
                WildInteractionFocusMarkerRuntime.markerStyle(WildSocialRole.MEMBER));
        assertEquals(
                WildInteractionFocusMarkerRuntime.MarkerStyle.ALPHA,
                WildInteractionFocusMarkerRuntime.markerStyle(WildSocialRole.ALPHA));
        assertThrows(IllegalArgumentException.class, () -> WildInteractionFocusMarkerRuntime.markerStyle(null));
    }

    @Test
    void markerStateRejectsInvalidAuthorityBookkeeping() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new WildInteractionFocusMarkerRuntime.FocusMarkerState(null, 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new WildInteractionFocusMarkerRuntime.FocusMarkerState(FIRST, -1L));
    }
}

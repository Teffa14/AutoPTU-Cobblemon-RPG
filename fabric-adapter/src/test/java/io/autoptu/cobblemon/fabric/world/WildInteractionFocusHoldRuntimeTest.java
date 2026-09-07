package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildInteractionFocusHoldRuntimeTest {
    @Test
    void safeInteractionBandHoldsFocusedWildActor() {
        assertTrue(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(25.0D, 4.0D));
    }

    @Test
    void authoredAlarmBandKeepsFleePresentationAuthoritative() {
        assertFalse(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(16.0D, 4.0D));
        assertFalse(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(9.0D, 4.0D));
    }

    @Test
    void encounterRangeBoundaryIsSharedWithVisibleInteraction() {
        assertTrue(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(36.0D, 4.0D));
        assertFalse(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(36.0001D, 4.0D));
    }

    @Test
    void invalidDistancesFailClosed() {
        assertFalse(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(Double.NaN, 4.0D));
        assertFalse(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(-1.0D, 4.0D));
        assertFalse(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(25.0D, Double.NaN));
        assertFalse(WildInteractionFocusHoldRuntime.shouldHoldInteractionFocus(25.0D, 0.0D));
    }
}

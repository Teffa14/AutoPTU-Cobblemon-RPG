package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildInteractionFocusAttentionRuntimeTest {
    @Test
    void safeInteractionBandFacesFocusedWildActorTowardPlayer() {
        assertTrue(WildInteractionFocusAttentionRuntime.shouldFaceInteractionFocus(25.0D, 4.0D));
    }

    @Test
    void authoredAlarmBandKeepsAlarmedPresentationAuthoritative() {
        assertFalse(WildInteractionFocusAttentionRuntime.shouldFaceInteractionFocus(16.0D, 4.0D));
        assertFalse(WildInteractionFocusAttentionRuntime.shouldFaceInteractionFocus(9.0D, 4.0D));
    }

    @Test
    void encounterRangeBoundaryMatchesVisibleInteraction() {
        assertTrue(WildInteractionFocusAttentionRuntime.shouldFaceInteractionFocus(36.0D, 4.0D));
        assertFalse(WildInteractionFocusAttentionRuntime.shouldFaceInteractionFocus(36.0001D, 4.0D));
    }

    @Test
    void deterministicPlayerSelectionUsesNearestThenUuid() {
        UUID lower = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID higher = UUID.fromString("00000000-0000-0000-0000-000000000002");

        assertEquals(lower, WildInteractionFocusAttentionRuntime.preferredPlayerIdentity(9.0D, lower, 16.0D, higher));
        assertEquals(higher, WildInteractionFocusAttentionRuntime.preferredPlayerIdentity(16.0D, lower, 9.0D, higher));
        assertEquals(lower, WildInteractionFocusAttentionRuntime.preferredPlayerIdentity(9.0D, higher, 9.0D, lower));
    }
}

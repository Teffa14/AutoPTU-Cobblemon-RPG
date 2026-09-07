package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildInteractionFocusContextRuntimeTest {
    private static final UUID FIRST = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private static final UUID SECOND = UUID.fromString("00000000-0000-0000-0000-000000000202");

    @Test
    void stableFocusedActorRepeatsCanonicalContextAtQuietCadence() {
        var snapshot = snapshot(FIRST, WildSocialRole.MEMBER, "fletchling", "Lower Shelf");
        var state = new WildInteractionFocusContextRuntime.FocusContextState(snapshot, 100L);

        assertFalse(WildInteractionFocusContextRuntime.shouldRepeat(state, snapshot, 170L));
        assertTrue(WildInteractionFocusContextRuntime.shouldRepeat(
                state,
                snapshot,
                100L + WildInteractionFocusContextRuntime.REPEAT_CONTEXT_INTERVAL_TICKS));
    }

    @Test
    void authoredIdentityChangeResetsCadenceAndReliesOnImmediateHabitatCue() {
        var lowerShelf = snapshot(FIRST, WildSocialRole.MEMBER, "fletchling", "Lower Shelf");
        var seasonalCrossing = snapshot(FIRST, WildSocialRole.MEMBER, "fletchling", "Seasonal Crossing");
        var alpha = snapshot(FIRST, WildSocialRole.ALPHA, "fletchling", "Lower Shelf");
        var otherActor = snapshot(SECOND, WildSocialRole.MEMBER, "fletchling", "Lower Shelf");
        var state = new WildInteractionFocusContextRuntime.FocusContextState(lowerShelf, 100L);

        assertFalse(WildInteractionFocusContextRuntime.shouldRepeat(state, seasonalCrossing, 200L));
        assertFalse(WildInteractionFocusContextRuntime.shouldRepeat(state, alpha, 200L));
        assertFalse(WildInteractionFocusContextRuntime.shouldRepeat(state, otherActor, 200L));

        var migrated = WildInteractionFocusContextRuntime.nextState(state, seasonalCrossing, 200L, false);
        assertEquals(seasonalCrossing, migrated.snapshot());
        assertEquals(200L, migrated.lastContextCueTick());
    }

    @Test
    void unchangedContextKeepsOriginalCadenceUntilRepeatActuallyEmits() {
        var snapshot = snapshot(FIRST, WildSocialRole.MEMBER, "fletchling", "Lower Shelf");
        var state = new WildInteractionFocusContextRuntime.FocusContextState(snapshot, 100L);

        assertSame(state, WildInteractionFocusContextRuntime.nextState(state, snapshot, 150L, false));
        var repeated = WildInteractionFocusContextRuntime.nextState(state, snapshot, 180L, true);
        assertEquals(snapshot, repeated.snapshot());
        assertEquals(180L, repeated.lastContextCueTick());
    }

    @Test
    void losingVisibleInteractionFocusClearsRepeatState() {
        var snapshot = snapshot(FIRST, WildSocialRole.MEMBER, "fletchling", "Lower Shelf");
        var state = new WildInteractionFocusContextRuntime.FocusContextState(snapshot, 100L);

        assertFalse(WildInteractionFocusContextRuntime.shouldRepeat(state, null, 200L));
        assertNull(WildInteractionFocusContextRuntime.nextState(state, null, 200L, false));
    }

    @Test
    void invalidBookkeepingFailsClosed() {
        var snapshot = snapshot(FIRST, WildSocialRole.MEMBER, "fletchling", "Lower Shelf");
        assertThrows(IllegalArgumentException.class,
                () -> new WildInteractionFocusContextRuntime.FocusContextState(null, 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new WildInteractionFocusContextRuntime.FocusContextState(snapshot, -1L));
        assertFalse(WildInteractionFocusContextRuntime.shouldRepeat(
                new WildInteractionFocusContextRuntime.FocusContextState(snapshot, 0L), snapshot, -1L));
        assertThrows(IllegalArgumentException.class,
                () -> WildInteractionFocusContextRuntime.nextState(null, snapshot, -1L, false));
    }

    private static WildHabitatCueRuntime.NearbyInteractionSnapshot snapshot(
            UUID actorId,
            WildSocialRole role,
            String speciesId,
            String habitat) {
        return new WildHabitatCueRuntime.NearbyInteractionSnapshot(actorId, role, speciesId, habitat);
    }
}

package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class AmbientPokemonBehaviorControllerTest {
    private static final AmbientPokemonBehaviorController.Profile PROFILE =
            new AmbientPokemonBehaviorController.Profile(14.0D, 7.0D, 3, 5);

    @Test
    void reactsOnlyToServerObservedPresenceAndDistance() {
        AmbientPokemonBehaviorController controller = new AmbientPokemonBehaviorController(PROFILE);

        assertEquals(AmbientPokemonBehaviorController.State.CALM, controller.state());
        assertEquals(AmbientPokemonBehaviorController.State.WATCHING, controller.update(12.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.ALARMED, controller.update(6.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.RECOVERING, controller.update(18.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.RECOVERING, controller.update(18.0D, true));
        assertEquals(AmbientPokemonBehaviorController.State.CALM, controller.update(18.0D, true));
    }

    @Test
    void abandonedAlarmEventuallyReturnsToCalmWithoutPokemonGameplayState() {
        AmbientPokemonBehaviorController controller = new AmbientPokemonBehaviorController(PROFILE);
        controller.update(4.0D, true);

        for (int i = 0; i < 4; i++) {
            assertEquals(AmbientPokemonBehaviorController.State.ALARMED, controller.update(Double.POSITIVE_INFINITY, false));
        }
        assertEquals(AmbientPokemonBehaviorController.State.CALM, controller.update(Double.POSITIVE_INFINITY, false));
    }

    @Test
    void calmRoamingTargetIsStableWithinClockSegmentAndInsideAuthoredLeash() {
        UUID actor = UUID.fromString("8e3d8854-0f85-4d7d-844d-a58c91079345");
        double[] first = MareaWildAmbientBehaviorRuntime.calmRoamingTarget(actor, 320L, 10.5D, -4.5D, 12);
        double[] sameSegment = MareaWildAmbientBehaviorRuntime.calmRoamingTarget(actor, 399L, 10.5D, -4.5D, 12);

        assertArrayEquals(first, sameSegment, 0.0D);
        assertTrue(MareaWildAmbientBehaviorRuntime.insideHorizontalLeash(
                first[0], first[1], 10.5D, -4.5D, 12));
    }

    @Test
    void calmRoamingTargetChangesByActorOrClockSegmentWithoutExternalRng() {
        UUID actor = UUID.fromString("8e3d8854-0f85-4d7d-844d-a58c91079345");
        UUID other = UUID.fromString("0ddcb831-f81c-44e8-a906-d677be11d465");
        double[] first = MareaWildAmbientBehaviorRuntime.calmRoamingTarget(actor, 320L, 0.5D, 0.5D, 10);
        double[] nextSegment = MareaWildAmbientBehaviorRuntime.calmRoamingTarget(actor, 400L, 0.5D, 0.5D, 10);
        double[] otherActor = MareaWildAmbientBehaviorRuntime.calmRoamingTarget(other, 320L, 0.5D, 0.5D, 10);

        assertFalse(first[0] == nextSegment[0] && first[1] == nextSegment[1]);
        assertFalse(first[0] == otherActor[0] && first[1] == otherActor[1]);
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.calmRoamingTarget(null, 0L, 0.0D, 0.0D, 10));
    }

    @Test
    void calmRoamingCadenceIncludesDeterministicRestWindow() {
        assertTrue(MareaWildAmbientBehaviorRuntime.calmWanderActive(0L));
        assertTrue(MareaWildAmbientBehaviorRuntime.calmWanderActive(59L));
        assertFalse(MareaWildAmbientBehaviorRuntime.calmWanderActive(60L));
        assertFalse(MareaWildAmbientBehaviorRuntime.calmWanderActive(79L));
        assertTrue(MareaWildAmbientBehaviorRuntime.calmWanderActive(80L));
        assertFalse(MareaWildAmbientBehaviorRuntime.calmWanderActive(-1L));
    }

    @Test
    void calmPopulationSeparationPushesOnlyNearbyCanonicalSiblingsApart() {
        UUID actor = UUID.fromString("8e3d8854-0f85-4d7d-844d-a58c91079345");
        UUID sibling = UUID.fromString("0ddcb831-f81c-44e8-a906-d677be11d465");

        double[] nearby = MareaWildAmbientBehaviorRuntime.calmSeparationImpulse(
                actor, 5.0D, 5.0D, sibling, 6.0D, 5.0D, 2.5D, 0.018D);
        assertTrue(nearby[0] < 0.0D);
        assertEquals(0.0D, nearby[1], 0.0000001D);
        assertTrue(Math.sqrt(nearby[0] * nearby[0] + nearby[1] * nearby[1]) <= 0.018D);

        double[] distant = MareaWildAmbientBehaviorRuntime.calmSeparationImpulse(
                actor, 5.0D, 5.0D, sibling, 8.0D, 5.0D, 2.5D, 0.018D);
        assertArrayEquals(new double[] {0.0D, 0.0D}, distant, 0.0D);

        double[] overlap = MareaWildAmbientBehaviorRuntime.calmSeparationImpulse(
                actor, 5.0D, 5.0D, sibling, 5.0D, 5.0D, 2.5D, 0.018D);
        assertEquals(0.018D, Math.sqrt(overlap[0] * overlap[0] + overlap[1] * overlap[1]), 0.0000001D);
        assertArrayEquals(overlap, MareaWildAmbientBehaviorRuntime.calmSeparationImpulse(
                actor, 5.0D, 5.0D, sibling, 5.0D, 5.0D, 2.5D, 0.018D), 0.0D);

        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.calmSeparationImpulse(
                        null, 0.0D, 0.0D, sibling, 0.0D, 0.0D, 2.5D, 0.018D));
    }

    @Test
    void calmPopulationCohesionPullsOnlyDispersedCanonicalSiblingsTogether() {
        UUID actor = UUID.fromString("8e3d8854-0f85-4d7d-844d-a58c91079345");
        UUID sibling = UUID.fromString("0ddcb831-f81c-44e8-a906-d677be11d465");

        double[] dispersed = MareaWildAmbientBehaviorRuntime.calmCohesionImpulse(
                actor, 2.0D, 4.0D, sibling, 10.0D, 4.0D, 6.0D, 0.012D);
        assertTrue(dispersed[0] > 0.0D);
        assertEquals(0.0D, dispersed[1], 0.0000001D);
        assertTrue(Math.sqrt(dispersed[0] * dispersed[0] + dispersed[1] * dispersed[1]) <= 0.012D);

        double[] withinFlock = MareaWildAmbientBehaviorRuntime.calmCohesionImpulse(
                actor, 2.0D, 4.0D, sibling, 7.5D, 4.0D, 6.0D, 0.012D);
        assertArrayEquals(new double[] {0.0D, 0.0D}, withinFlock, 0.0D);

        double[] sameActor = MareaWildAmbientBehaviorRuntime.calmCohesionImpulse(
                actor, 2.0D, 4.0D, actor, 10.0D, 4.0D, 6.0D, 0.012D);
        assertArrayEquals(new double[] {0.0D, 0.0D}, sameActor, 0.0D);

        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.calmCohesionImpulse(
                        actor, 0.0D, 0.0D, sibling, Double.NaN, 0.0D, 6.0D, 0.012D));
    }

    @Test
    void recoveryImpulsePointsBackToAuthoredHabitatAnchor() {
        double[] impulse = MareaWildAmbientBehaviorRuntime.recoveryImpulse(10.0D, 10.0D, 13.0D, 14.0D, 0.05D);

        assertEquals(0.03D, impulse[0], 0.0000001D);
        assertEquals(0.04D, impulse[1], 0.0000001D);
    }

    @Test
    void recoveryImpulseIsZeroAtAnchorAndRejectsInvalidInputs() {
        double[] atAnchor = MareaWildAmbientBehaviorRuntime.recoveryImpulse(5.0D, 7.0D, 5.0D, 7.0D, 0.04D);
        assertEquals(0.0D, atAnchor[0], 0.0D);
        assertEquals(0.0D, atAnchor[1], 0.0D);

        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.recoveryImpulse(0.0D, 0.0D, 1.0D, 1.0D, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.recoveryImpulse(Double.NaN, 0.0D, 1.0D, 1.0D, 0.04D));
    }

    @Test
    void ambientVelocityIsCappedInsteadOfAccumulatingAcrossTicks() {
        double[] bounded = MareaWildAmbientBehaviorRuntime.boundedHorizontalVelocity(0.12D, 0.16D, 0.08D);
        assertEquals(0.048D, bounded[0], 0.0000001D);
        assertEquals(0.064D, bounded[1], 0.0000001D);

        double[] alreadyBounded = MareaWildAmbientBehaviorRuntime.boundedHorizontalVelocity(0.03D, 0.04D, 0.08D);
        assertEquals(0.03D, alreadyBounded[0], 0.0000001D);
        assertEquals(0.04D, alreadyBounded[1], 0.0000001D);

        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.boundedHorizontalVelocity(Double.NaN, 0.0D, 0.08D));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.boundedHorizontalVelocity(0.01D, 0.01D, 0.0D));
    }

    @Test
    void authoredHabitatLeashDetectsNativeRoamingEscape() {
        assertTrue(MareaWildAmbientBehaviorRuntime.insideHorizontalLeash(10.0D, 10.0D, 10.0D, 10.0D, 8));
        assertTrue(MareaWildAmbientBehaviorRuntime.insideHorizontalLeash(18.0D, 10.0D, 10.0D, 10.0D, 8));
        assertFalse(MareaWildAmbientBehaviorRuntime.insideHorizontalLeash(18.01D, 10.0D, 10.0D, 10.0D, 8));

        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.insideHorizontalLeash(Double.NaN, 0.0D, 0.0D, 0.0D, 8));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildAmbientBehaviorRuntime.insideHorizontalLeash(0.0D, 0.0D, 0.0D, 0.0D, 0));
    }

    @Test
    void rejectsInvalidAuthoredProfilesAndInvalidObservedDistance() {
        assertThrows(IllegalArgumentException.class,
                () -> new AmbientPokemonBehaviorController.Profile(0.0D, 1.0D, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> new AmbientPokemonBehaviorController.Profile(5.0D, 6.0D, 1, 1));

        AmbientPokemonBehaviorController controller = new AmbientPokemonBehaviorController(PROFILE);
        assertThrows(IllegalArgumentException.class, () -> controller.update(Double.NaN, true));
        assertThrows(IllegalArgumentException.class, () -> controller.update(-1.0D, true));
    }
}

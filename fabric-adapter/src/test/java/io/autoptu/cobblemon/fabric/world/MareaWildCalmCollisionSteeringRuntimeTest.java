package io.autoptu.cobblemon.fabric.world;

import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.entity.ai.pathing.PathNode;
import net.minecraft.util.math.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MareaWildCalmCollisionSteeringRuntimeTest {
    @Test
    void rotatedVelocityPreservesSpeedAcrossDetourAngles() {
        double speed = 0.025D;
        for (double angle : new double[] {-135.0D, -90.0D, -45.0D, 45.0D, 90.0D, 135.0D}) {
            double[] rotated = MareaWildCalmCollisionSteeringRuntime.rotate(speed, 0.0D, angle);
            assertEquals(speed, Math.sqrt(rotated[0] * rotated[0] + rotated[1] * rotated[1]), 0.0000001D);
        }
    }

    @Test
    void actorIdentityDeterministicallyChoosesDetourHandedness() {
        UUID clockwise = new UUID(0L, 0L);
        UUID counterclockwise = new UUID(0L, 1L);

        assertTrue(MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(clockwise));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(counterclockwise));
        assertEquals(
                MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(clockwise),
                MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(clockwise));
    }

    @Test
    void nativeNavigationTargetMustRemainInsideAuthoredLeash() {
        assertTrue(MareaWildCalmCollisionSteeringRuntime.navigationTargetInsideLeash(
                10.5D, 20.5D, 8, 16.0D, 20.5D));
        assertTrue(MareaWildCalmCollisionSteeringRuntime.navigationTargetInsideLeash(
                10.5D, 20.5D, 8, 10.5D, 28.5D));
        assertTrue(MareaWildCalmCollisionSteeringRuntime.navigationTargetInsideLeash(
                10.5D, 20.5D, 8, 2.5D, 20.5D));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.navigationTargetInsideLeash(
                10.5D, 20.5D, 8, 18.6D, 20.5D));
    }

    @Test
    void nativeNavigationRetriesExactTargetColumnAtMinecraftSurfaceHeight() {
        assertArrayEquals(
                new int[] {64, 67},
                MareaWildCalmCollisionSteeringRuntime.navigationTargetYCandidates(64, 67));
        assertArrayEquals(
                new int[] {64, 61},
                MareaWildCalmCollisionSteeringRuntime.navigationTargetYCandidates(64, 61));
        assertArrayEquals(
                new int[] {64},
                MareaWildCalmCollisionSteeringRuntime.navigationTargetYCandidates(64, 64));
    }

    @Test
    void calmNativeNavigationRequiresLocallyContinuousMinecraftSurface() {
        assertTrue(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceNeighborhood(
                64, 64, 65, 63, 64));
        assertTrue(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceNeighborhood(
                64, 63, 63, 65, 65));

        assertFalse(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceNeighborhood(
                64, 64, 64, 61, 64));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceNeighborhood(
                64, 68, 64, 64, 64));
    }

    @Test
    void calmNativeNavigationRequiresContinuousSurfaceAcrossEntirePath() {
        assertTrue(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceProfile(64));
        assertTrue(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceProfile(64, 65, 65, 66, 65, 64));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceProfile(64, 65, 68, 67));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceProfile(64, 61));
    }

    @Test
    void nativeNavigationPathMustRemainInsideAuthoredLeashAcrossTerrainHeightChanges() {
        Path safe = new Path(
                List.of(new PathNode(10, 64, 20), new PathNode(14, 65, 20), new PathNode(16, 67, 20)),
                new BlockPos(16, 67, 20),
                true);
        Path escapes = new Path(
                List.of(new PathNode(10, 64, 20), new PathNode(19, 66, 20), new PathNode(16, 67, 20)),
                new BlockPos(16, 67, 20),
                true);

        assertTrue(MareaWildCalmCollisionSteeringRuntime.navigationPathInsideLeash(
                10.5D, 20.5D, 8, safe));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.navigationPathInsideLeash(
                10.5D, 20.5D, 8, escapes));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.navigationPathInsideLeash(
                10.5D, 20.5D, 8, null));
    }

    @Test
    void initialNativeRouteRejectsOccupiedPresentationNodeBeforeMovementStarts() {
        assertFalse(MareaWildCalmCollisionSteeringRuntime.navigationPresentationProfileClear(
                true, true, false, true));
    }

    @Test
    void initialNativeRouteRejectsBlockedPresentationNodeBeforeMovementStarts() {
        assertFalse(MareaWildCalmCollisionSteeringRuntime.navigationPresentationProfileClear(
                true, false, true));
    }

    @Test
    void initialNativeRouteAcceptsOnlyFullyClearPresentationProfile() {
        assertTrue(MareaWildCalmCollisionSteeringRuntime.navigationPresentationProfileClear(
                true, true, true));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.navigationPresentationProfileClear());
        assertFalse(MareaWildCalmCollisionSteeringRuntime.navigationPresentationProfileClear((boolean[]) null));
    }

    @Test
    void calmSteeringProbeRequiresBothBlockAndActiveWildClearance() {
        assertTrue(MareaWildCalmCollisionSteeringRuntime.steeringProbePresentationClear(true, false));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.steeringProbePresentationClear(false, false));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.steeringProbePresentationClear(true, true));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.steeringProbePresentationClear(false, true));
    }

    @Test
    void invalidSteeringInputsFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.clockwiseFirst(null));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.rotate(Double.NaN, 0.0D, 45.0D));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.rotate(0.01D, 0.0D, Double.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.navigationTargetInsideLeash(
                        0.0D, 0.0D, 0, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.navigationPathInsideLeash(
                        0.0D, 0.0D, 0, null));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceNeighborhood(64, 64, 64, 64));
        assertThrows(IllegalArgumentException.class,
                () -> MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceProfile());
    }
}

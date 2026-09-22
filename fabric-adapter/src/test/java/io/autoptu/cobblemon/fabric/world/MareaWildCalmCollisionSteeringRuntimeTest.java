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
    void globalNavigationOwnsDetourRotation() {
        double speed = 0.025D;
        for (double angle : new double[] {-135.0D, -90.0D, -45.0D, 45.0D, 90.0D, 135.0D}) {
            double[] rotated = WildCalmCollisionNavigationRuntime.rotate(speed, 0.0D, angle);
            assertEquals(speed, Math.sqrt(rotated[0] * rotated[0] + rotated[1] * rotated[1]), 0.0000001D);
        }
    }

    @Test
    void globalNavigationOwnsDetourHandedness() {
        UUID clockwise = new UUID(0L, 0L);
        UUID counterclockwise = new UUID(0L, 1L);
        assertTrue(WildCalmCollisionNavigationRuntime.clockwiseFirst(clockwise));
        assertFalse(WildCalmCollisionNavigationRuntime.clockwiseFirst(counterclockwise));
    }

    @Test
    void compatibilityFacadeDelegatesOnlyRemainingMareaContinuitySurface() {
        assertTrue(MareaWildCalmCollisionSteeringRuntime.navigationTargetInsideLeash(
                10.5D, 20.5D, 8, 16.0D, 20.5D));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.navigationTargetInsideLeash(
                10.5D, 20.5D, 8, 18.6D, 20.5D));
        assertTrue(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceNeighborhood(
                64, 64, 65, 63, 64));
        assertFalse(MareaWildCalmCollisionSteeringRuntime.stableCalmSurfaceNeighborhood(
                64, 64, 64, 61, 64));
    }

    @Test
    void globalNavigationOwnsSurfaceAndPathValidation() {
        assertArrayEquals(new int[] {64, 67}, WildCalmCollisionNavigationRuntime.navigationTargetYCandidates(64, 67));
        assertArrayEquals(new int[] {64}, WildCalmCollisionNavigationRuntime.navigationTargetYCandidates(64, 64));
        assertTrue(WildCalmCollisionNavigationRuntime.stableCalmSurfaceProfile(64, 65, 65, 66, 65, 64));
        assertFalse(WildCalmCollisionNavigationRuntime.stableCalmSurfaceProfile(64, 65, 68, 67));

        Path safe = new Path(
                List.of(new PathNode(10, 64, 20), new PathNode(14, 65, 20), new PathNode(16, 67, 20)),
                new BlockPos(16, 67, 20), true);
        Path escapes = new Path(
                List.of(new PathNode(10, 64, 20), new PathNode(19, 66, 20), new PathNode(16, 67, 20)),
                new BlockPos(16, 67, 20), true);
        assertTrue(WildCalmCollisionNavigationRuntime.navigationPathInsideLeash(10.5D, 20.5D, 8, safe));
        assertFalse(WildCalmCollisionNavigationRuntime.navigationPathInsideLeash(10.5D, 20.5D, 8, escapes));
        assertTrue(WildCalmCollisionNavigationRuntime.navigationPresentationProfileClear(true, true, true));
        assertFalse(WildCalmCollisionNavigationRuntime.navigationPresentationProfileClear(true, false, true));
        assertTrue(WildCalmCollisionNavigationRuntime.presentationNodeClear(true, false));
        assertFalse(WildCalmCollisionNavigationRuntime.presentationNodeClear(true, true));
    }

    @Test
    void invalidGlobalNavigationInputsFailClosed() {
        assertThrows(IllegalArgumentException.class, () -> WildCalmCollisionNavigationRuntime.clockwiseFirst(null));
        assertThrows(IllegalArgumentException.class,
                () -> WildCalmCollisionNavigationRuntime.rotate(Double.NaN, 0.0D, 45.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildCalmCollisionNavigationRuntime.navigationTargetInsideLeash(0.0D, 0.0D, 0, 1.0D, 1.0D));
        assertThrows(IllegalArgumentException.class,
                () -> WildCalmCollisionNavigationRuntime.stableCalmSurfaceProfile());
    }
}

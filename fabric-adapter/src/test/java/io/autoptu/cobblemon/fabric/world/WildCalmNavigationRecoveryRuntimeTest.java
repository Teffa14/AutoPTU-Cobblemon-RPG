package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildCalmNavigationRecoveryRuntimeTest {
    @Test
    void stationaryNavigationBecomesRecoverableAfterThreeSamples() {
        WildCalmNavigationRecoveryRuntime.NavigationProgress progress = null;
        progress = WildCalmNavigationRecoveryRuntime.sample(progress, 10.0D, 20.0D);
        assertFalse(WildCalmNavigationRecoveryRuntime.shouldRecover(progress));

        progress = WildCalmNavigationRecoveryRuntime.sample(progress, 10.02D, 20.01D);
        assertEquals(1, progress.stalledSamples());
        progress = WildCalmNavigationRecoveryRuntime.sample(progress, 10.03D, 20.02D);
        assertEquals(2, progress.stalledSamples());
        progress = WildCalmNavigationRecoveryRuntime.sample(progress, 10.04D, 20.03D);

        assertEquals(3, progress.stalledSamples());
        assertTrue(WildCalmNavigationRecoveryRuntime.shouldRecover(progress));
    }

    @Test
    void meaningfulPhysicalProgressResetsTheStallCounter() {
        var progress = new WildCalmNavigationRecoveryRuntime.NavigationProgress(10.0D, 20.0D, 2);
        var moved = WildCalmNavigationRecoveryRuntime.sample(progress, 10.25D, 20.0D);

        assertEquals(0, moved.stalledSamples());
        assertFalse(WildCalmNavigationRecoveryRuntime.shouldRecover(moved));
    }

    @Test
    void invalidNavigationSamplesFailClosed() {
        assertThrows(IllegalArgumentException.class,
                () -> WildCalmNavigationRecoveryRuntime.sample(null, Double.NaN, 0.0D));
        assertThrows(IllegalArgumentException.class,
                () -> new WildCalmNavigationRecoveryRuntime.NavigationProgress(0.0D, 0.0D, -1));
    }
}

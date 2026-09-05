package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildHabitatCueRuntimeTest {
    @Test
    void horizontalHabitatBoundaryIsInclusiveAndRegionAgnostic() {
        var habitat = new WildHabitatCueRuntime.HabitatCue("ouros.any.region.population", 10.0D, -4.0D, 6, 3);

        assertTrue(WildHabitatCueRuntime.containsHorizontal(16.0D, -4.0D, habitat));
        assertTrue(WildHabitatCueRuntime.containsHorizontal(10.0D, 2.0D, habitat));
        assertFalse(WildHabitatCueRuntime.containsHorizontal(16.01D, -4.0D, habitat));
    }

    @Test
    void habitatCueDoesNotDependOnMareaNaming() {
        var cave = new WildHabitatCueRuntime.HabitatCue("ouros.cave.deep-chamber", 0.0D, 0.0D, 12, 2);
        var coast = new WildHabitatCueRuntime.HabitatCue("ouros.coast.tide-pool", 40.0D, 40.0D, 8, 5);

        assertTrue(WildHabitatCueRuntime.containsHorizontal(3.0D, 4.0D, cave));
        assertTrue(WildHabitatCueRuntime.containsHorizontal(44.0D, 44.0D, coast));
        assertFalse(WildHabitatCueRuntime.containsHorizontal(20.0D, 20.0D, cave));
    }
}

package io.autoptu.cobblemon.fabric.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildHabitatCueRuntimeTest {
    @Test
    void horizontalHabitatBoundaryIsInclusiveAndRegionAgnostic() {
        var habitat = new WildHabitatCueRuntime.HabitatCue(
                "ouros.any.region.population",
                "Any Region",
                List.of(new WildHabitatCueRuntime.HabitatCircle(10.0D, -4.0D, 6)),
                3,
                1);

        assertTrue(WildHabitatCueRuntime.containsHorizontal(16.0D, -4.0D, habitat));
        assertTrue(WildHabitatCueRuntime.containsHorizontal(10.0D, 2.0D, habitat));
        assertFalse(WildHabitatCueRuntime.containsHorizontal(16.01D, -4.0D, habitat));
        assertEquals("Any Region", habitat.displayName());
    }

    @Test
    void onePopulationCanExposeMultipleActorAnchorsWithoutSplittingItsCue() {
        var habitat = new WildHabitatCueRuntime.HabitatCue(
                "ouros.any.multi-anchor-population",
                "Multi Anchor Habitat",
                List.of(
                        new WildHabitatCueRuntime.HabitatCircle(0.0D, 0.0D, 5),
                        new WildHabitatCueRuntime.HabitatCircle(20.0D, 0.0D, 5)),
                2,
                1);

        assertTrue(WildHabitatCueRuntime.containsHorizontal(3.0D, 4.0D, habitat));
        assertTrue(WildHabitatCueRuntime.containsHorizontal(23.0D, 4.0D, habitat));
        assertFalse(WildHabitatCueRuntime.containsHorizontal(10.0D, 0.0D, habitat));
    }

    @Test
    void habitatCueDoesNotDependOnMareaNaming() {
        var cave = new WildHabitatCueRuntime.HabitatCue(
                "ouros.cave.deep-chamber",
                "Deep Chamber",
                List.of(new WildHabitatCueRuntime.HabitatCircle(0.0D, 0.0D, 12)),
                2,
                0);
        var coast = new WildHabitatCueRuntime.HabitatCue(
                "ouros.coast.tide-pool",
                "Tide Pool",
                List.of(new WildHabitatCueRuntime.HabitatCircle(40.0D, 40.0D, 8)),
                5,
                1);

        assertTrue(WildHabitatCueRuntime.containsHorizontal(3.0D, 4.0D, cave));
        assertTrue(WildHabitatCueRuntime.containsHorizontal(44.0D, 44.0D, coast));
        assertFalse(WildHabitatCueRuntime.containsHorizontal(20.0D, 20.0D, cave));
    }

    @Test
    void announcementReportsOnlyServerAuthoredAlphaPresence() {
        var herd = new WildHabitatCueRuntime.HabitatCue(
                "ouros.any.herd",
                "Windbreak",
                List.of(new WildHabitatCueRuntime.HabitatCircle(0.0D, 0.0D, 8)),
                4,
                1);
        var ordinary = new WildHabitatCueRuntime.HabitatCue(
                "ouros.any.members",
                "Lower Shelf",
                List.of(new WildHabitatCueRuntime.HabitatCircle(20.0D, 0.0D, 8)),
                2,
                0);

        assertEquals("Wild habitat — Windbreak · 4 roaming Pokemon · Alpha present",
                WildHabitatCueRuntime.announcementText(herd));
        assertEquals("Wild habitat — Lower Shelf · 2 roaming Pokemon",
                WildHabitatCueRuntime.announcementText(ordinary));
    }

    @Test
    void alphaCountCannotExceedVisibleServerProjection() {
        assertThrows(IllegalArgumentException.class, () -> new WildHabitatCueRuntime.HabitatCue(
                "ouros.invalid",
                "Invalid",
                List.of(new WildHabitatCueRuntime.HabitatCircle(0.0D, 0.0D, 5)),
                1,
                2));
    }
}

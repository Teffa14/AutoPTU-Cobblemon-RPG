package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalWorldExplorationProgressServiceTest {
    private final CanonicalWorldExplorationProgressService service =
            new CanonicalWorldExplorationProgressService(CanonicalWorldHierarchyCatalogue.DEFAULT);

    @Test
    void mareaProgressUsesDurableDiscoveriesAcrossItsAuthoredChildren() {
        var progress = service.progress(
                CanonicalWorldHierarchyCatalogue.MAREA_TERRITORY_ID,
                Set.of("ouros.marea.puerto_bruma", "ouros.marea.sendero_vidrio")
        );

        assertEquals("Marea", progress.displayName());
        assertEquals(2, progress.discoveredCount());
        assertEquals(4, progress.discoverableCount());
        assertFalse(progress.complete());
        assertEquals("Marea 2/4", progress.compactLabel());
    }

    @Test
    void mareaCompletesOnlyWhenEveryRepresentedLocalityAndRouteIsDiscovered() {
        var progress = service.progress(
                CanonicalWorldHierarchyCatalogue.MAREA_TERRITORY_ID,
                Set.of(
                        "ouros.marea.puerto_bruma",
                        "ouros.marea.sendero_vidrio",
                        "ouros.marea.loma_clara",
                        "ouros.marea.estacion_mirador"
                )
        );

        assertEquals(4, progress.discoveredCount());
        assertEquals(4, progress.discoverableCount());
        assertTrue(progress.complete());
    }

    @Test
    void locationProgressResolvesToItsImmediateAuthoredScope() {
        var progress = service.nearestProgressForLocation(
                "ouros.marea.loma_clara",
                Set.of("ouros.marea.loma_clara")
        );

        assertEquals(CanonicalWorldHierarchyCatalogue.MAREA_TERRITORY_ID, progress.nodeId());
        assertEquals(1, progress.discoveredCount());
        assertEquals(4, progress.discoverableCount());
    }
}

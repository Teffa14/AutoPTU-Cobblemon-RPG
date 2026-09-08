package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.ecology.MigrationPhase;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WildHabitatMigrationContextRuntimeTest {
    @Test
    void firstObservationIsEntryContextAndLaterPhaseChangeIsMigrationContext() {
        assertTrue(WildHabitatMigrationContextRuntime.shouldAnnounceEntry(null, MigrationPhase.PREPARING));
        assertFalse(WildHabitatMigrationContextRuntime.shouldAnnounceEntry(
                MigrationPhase.PREPARING, MigrationPhase.PREPARING));
        assertFalse(WildHabitatMigrationContextRuntime.shouldAnnounceEntry(
                MigrationPhase.PREPARING, MigrationPhase.DEPARTING));

        assertFalse(WildHabitatMigrationContextRuntime.shouldAnnounce(null, MigrationPhase.PREPARING));
        assertFalse(WildHabitatMigrationContextRuntime.shouldAnnounce(
                MigrationPhase.PREPARING, MigrationPhase.PREPARING));
        assertTrue(WildHabitatMigrationContextRuntime.shouldAnnounce(
                MigrationPhase.PREPARING, MigrationPhase.DEPARTING));
        assertTrue(WildHabitatMigrationContextRuntime.shouldAnnounce(
                MigrationPhase.STOPOVER, MigrationPhase.ARRIVING));
    }

    @Test
    void unprojectedContextOnlyAnnouncesExplicitAuthoredTransit() {
        assertTrue(WildHabitatMigrationContextRuntime.shouldAnnounceUnprojected(
                MigrationPhase.DEPARTING, MigrationPhase.IN_TRANSIT));
        assertFalse(WildHabitatMigrationContextRuntime.shouldAnnounceUnprojected(
                MigrationPhase.DEPARTING, MigrationPhase.SEASONAL_RESIDENCE));
        assertFalse(WildHabitatMigrationContextRuntime.shouldAnnounceUnprojected(
                MigrationPhase.IN_TRANSIT, MigrationPhase.IN_TRANSIT));
        assertFalse(WildHabitatMigrationContextRuntime.shouldAnnounceUnprojected(null, MigrationPhase.IN_TRANSIT));
    }

    @Test
    void habitatBoundaryAndMessagesUseOnlyAuthoredWorldContext() {
        var context = new WildHabitatMigrationContextRuntime.HabitatMigrationContext(
                "ouros.marea.lower_shelf",
                "Sendero Seasonal Crossing",
                MigrationPhase.ARRIVING,
                List.of(new WildHabitatMigrationContextRuntime.HabitatCircle(10.0D, -4.0D, 6)));

        assertTrue(WildHabitatMigrationContextRuntime.containsHorizontal(16.0D, -4.0D, context));
        assertFalse(WildHabitatMigrationContextRuntime.containsHorizontal(16.01D, -4.0D, context));
        assertEquals(
                "Wild habitat — Sendero Seasonal Crossing · Arriving",
                WildHabitatMigrationContextRuntime.entryContextText(context));
        assertEquals(
                "Wild habitat no longer nearby — Sendero Seasonal Crossing",
                WildHabitatMigrationContextRuntime.departureContextText(context));
        assertEquals(
                "Wild habitat migration — Sendero Seasonal Crossing · In transit",
                WildHabitatMigrationContextRuntime.unprojectedMigrationText(context, MigrationPhase.IN_TRANSIT));
        assertEquals(
                "Wild habitat migration — Sendero Seasonal Crossing · Arriving",
                WildHabitatMigrationContextRuntime.announcementText(context));
    }
}

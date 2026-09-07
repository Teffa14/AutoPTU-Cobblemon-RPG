package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.ecology.MigrationPhase;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    void cueRefreshesOnlyWhenVisibleAuthoredPopulationStateChanges() {
        var original = new WildHabitatCueRuntime.HabitatCue(
                "ouros.any.herd",
                "Windbreak",
                List.of(new WildHabitatCueRuntime.HabitatCircle(0.0D, 0.0D, 8)),
                4,
                1);
        var samePopulationStateAtAnotherAnchor = new WildHabitatCueRuntime.HabitatCue(
                "ouros.any.herd",
                "Windbreak",
                List.of(new WildHabitatCueRuntime.HabitatCircle(4.0D, 0.0D, 8)),
                4,
                1);
        var oneActorGone = new WildHabitatCueRuntime.HabitatCue(
                "ouros.any.herd",
                "Windbreak",
                List.of(new WildHabitatCueRuntime.HabitatCircle(0.0D, 0.0D, 8)),
                3,
                1);
        var alphaGone = new WildHabitatCueRuntime.HabitatCue(
                "ouros.any.herd",
                "Windbreak",
                List.of(new WildHabitatCueRuntime.HabitatCircle(0.0D, 0.0D, 8)),
                4,
                0);

        var previous = WildHabitatCueRuntime.HabitatSnapshot.from(original);
        assertFalse(WildHabitatCueRuntime.shouldAnnounce(previous, samePopulationStateAtAnotherAnchor));
        assertTrue(WildHabitatCueRuntime.shouldAnnounce(previous, oneActorGone));
        assertTrue(WildHabitatCueRuntime.shouldAnnounce(previous, alphaGone));
        assertTrue(WildHabitatCueRuntime.shouldAnnounce(null, original));
    }

    @Test
    void engagementCueUsesTheExactServerInteractionDistanceBoundary() {
        assertTrue(VisibleWildPokemonEncounterRuntime.isWithinInteractionDistanceSquared(0.0D));
        assertTrue(VisibleWildPokemonEncounterRuntime.isWithinInteractionDistanceSquared(36.0D));
        assertFalse(VisibleWildPokemonEncounterRuntime.isWithinInteractionDistanceSquared(36.0001D));
        assertFalse(VisibleWildPokemonEncounterRuntime.isWithinInteractionDistanceSquared(Double.NaN));
        assertFalse(VisibleWildPokemonEncounterRuntime.isWithinInteractionDistanceSquared(-1.0D));
    }

    @Test
    void engagementCueAndPhysicalClickRequireMinecraftVisibilityAtTheSameBoundary() {
        assertTrue(VisibleWildPokemonEncounterRuntime.isEligibleInteractionTarget(0.0D, true));
        assertTrue(VisibleWildPokemonEncounterRuntime.isEligibleInteractionTarget(36.0D, true));
        assertFalse(VisibleWildPokemonEncounterRuntime.isEligibleInteractionTarget(36.0001D, true));
        assertFalse(VisibleWildPokemonEncounterRuntime.isEligibleInteractionTarget(1.0D, false));
        assertFalse(VisibleWildPokemonEncounterRuntime.isEligibleInteractionTarget(Double.NaN, true));
    }

    @Test
    void engagementCueAnnouncesOnActorOrAuthoredIdentityChange() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000101");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000202");
        var firstMember = new WildHabitatCueRuntime.NearbyInteractionSnapshot(first, WildSocialRole.MEMBER, "fletchling", "Lower Shelf");
        var firstAlpha = new WildHabitatCueRuntime.NearbyInteractionSnapshot(first, WildSocialRole.ALPHA, "fletchling", "Lower Shelf");
        var migratedMember = new WildHabitatCueRuntime.NearbyInteractionSnapshot(first, WildSocialRole.MEMBER, "fletchling", "Seasonal Crossing");
        var secondMember = new WildHabitatCueRuntime.NearbyInteractionSnapshot(second, WildSocialRole.MEMBER, "fletchling", "Lower Shelf");

        assertTrue(WildHabitatCueRuntime.shouldAnnounceNearbyInteraction(null, firstMember));
        assertFalse(WildHabitatCueRuntime.shouldAnnounceNearbyInteraction(firstMember, firstMember));
        assertTrue(WildHabitatCueRuntime.shouldAnnounceNearbyInteraction(firstMember, firstAlpha));
        assertTrue(WildHabitatCueRuntime.shouldAnnounceNearbyInteraction(firstMember, migratedMember));
        assertTrue(WildHabitatCueRuntime.shouldAnnounceNearbyInteraction(firstMember, secondMember));
        assertFalse(WildHabitatCueRuntime.shouldAnnounceNearbyInteraction(firstMember, null));
    }

    @Test
    void engagementCueUsesCanonicalSpeciesAndAuthoredHabitatIdentity() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000101");
        var member = new WildHabitatCueRuntime.NearbyInteractionSnapshot(
                actor, WildSocialRole.MEMBER, "cobblemon:fletchling", "Sendero Seasonal Crossing");
        var alpha = new WildHabitatCueRuntime.NearbyInteractionSnapshot(
                actor, WildSocialRole.ALPHA, "fletchling", "Loma Windbreak");

        assertEquals("Fletchling · Sendero Seasonal Crossing · interact to inspect encounter",
                WildHabitatCueRuntime.nearbyInteractionText(member));
        assertEquals("Alpha Fletchling · Loma Windbreak · interact to inspect encounter",
                WildHabitatCueRuntime.nearbyInteractionText(alpha));
        assertEquals("Mr Mime", WildHabitatCueRuntime.displaySpeciesName("cobblemon:mr_mime"));
    }

    @Test
    void engagementCueShowsAuthoredMigrationPhaseAndRefreshesWhenItChanges() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000101");
        var stopover = new WildHabitatCueRuntime.NearbyInteractionSnapshot(
                actor,
                WildSocialRole.ALPHA,
                "cobblemon:fletchling",
                "Sendero Seasonal Crossing",
                Optional.of(MigrationPhase.STOPOVER));
        var arriving = new WildHabitatCueRuntime.NearbyInteractionSnapshot(
                actor,
                WildSocialRole.ALPHA,
                "cobblemon:fletchling",
                "Sendero Seasonal Crossing",
                Optional.of(MigrationPhase.ARRIVING));

        assertEquals("Alpha Fletchling · Sendero Seasonal Crossing · Stopover · interact to inspect encounter",
                WildHabitatCueRuntime.nearbyInteractionText(stopover));
        assertEquals("Seasonal residence", WildHabitatCueRuntime.displayMigrationPhase(MigrationPhase.SEASONAL_RESIDENCE));
        assertFalse(WildHabitatCueRuntime.shouldAnnounceNearbyInteraction(stopover, stopover));
        assertTrue(WildHabitatCueRuntime.shouldAnnounceNearbyInteraction(stopover, arriving));
    }

    @Test
    void legacyEngagementCueLabelsOnlyTheServerAuthoredAlphaRole() {
        assertEquals("Wild Pokemon within reach · interact to inspect encounter",
                WildHabitatCueRuntime.nearbyInteractionText(WildSocialRole.MEMBER));
        assertEquals("Alpha wild Pokemon within reach · interact to inspect encounter",
                WildHabitatCueRuntime.nearbyInteractionText(WildSocialRole.ALPHA));
    }

    @Test
    void nearbySnapshotRequiresCanonicalActorRoleSpeciesAndHabitat() {
        UUID actor = UUID.fromString("00000000-0000-0000-0000-000000000101");
        assertThrows(IllegalArgumentException.class,
                () -> new WildHabitatCueRuntime.NearbyInteractionSnapshot(null, WildSocialRole.MEMBER, "fletchling", "Lower Shelf"));
        assertThrows(IllegalArgumentException.class,
                () -> new WildHabitatCueRuntime.NearbyInteractionSnapshot(actor, null, "fletchling", "Lower Shelf"));
        assertThrows(IllegalArgumentException.class,
                () -> new WildHabitatCueRuntime.NearbyInteractionSnapshot(actor, WildSocialRole.MEMBER, " ", "Lower Shelf"));
        assertThrows(IllegalArgumentException.class,
                () -> new WildHabitatCueRuntime.NearbyInteractionSnapshot(actor, WildSocialRole.MEMBER, "fletchling", " "));
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

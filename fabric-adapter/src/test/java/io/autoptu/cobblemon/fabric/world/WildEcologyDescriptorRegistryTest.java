package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WildEcologyDescriptorRegistryTest {
    @Test
    void mareaDescriptorCarriesAllServerOwnedEcologyInputsTogether() {
        var descriptor = MareaWildEcologyContent.descriptors().getFirst();
        var migrating = CanonicalWildPopulationCatalogue.DEFAULT.population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID).orElseThrow();
        var resident = CanonicalWildPopulationCatalogue.DEFAULT.population(CanonicalWildPopulationCatalogue.MAREA_LOMA_WINDBREAK_POPULATION_ID).orElseThrow();
        var alpha = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID).orElseThrow();
        var member = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_SECOND_FLETCHLING_ID).orElseThrow();

        assertEquals("fixture.ouros.marea", descriptor.sourceId());
        assertTrue(descriptor.populationSelector().test(migrating));
        assertFalse(descriptor.worldEligibility().accepts(null));
        assertEquals(1, descriptor.projectionProfiles().size());
        assertEquals("ouros.marea.sendero_vidrio", descriptor.projectedSiteId(migrating, 0L).orElseThrow());
        assertTrue(descriptor.projectedSiteId(migrating, 85_000L).isEmpty());
        assertEquals("ouros.marea.sendero_crossing", descriptor.projectedSiteId(migrating, 100_000L).orElseThrow());
        assertEquals(resident.siteId(), descriptor.projectedSiteId(resident, 100_000L).orElseThrow());
        assertNotNull(descriptor.blueprintSource());
        assertNotNull(descriptor.projectionEligibility());
        assertNotNull(descriptor.behaviorProfile());
        assertEquals(WildSocialRole.ALPHA, descriptor.socialRole(alpha));
        assertEquals(WildSocialRole.MEMBER, descriptor.socialRole(member));
        assertEquals(WildEcologyDescriptorRegistry.PresentationCapabilities.ALPHA_HERD_LEADER, descriptor.presentationCapabilities(alpha));
        assertEquals(WildEcologyDescriptorRegistry.PresentationCapabilities.NONE, descriptor.presentationCapabilities(member));
    }

    @Test
    void everyMareaPopulationAuthorsExactlyOneAlphaWithExplicitPresentationCapability() {
        var descriptor = MareaWildEcologyContent.descriptors().getFirst();
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!descriptor.populationSelector().test(population)) continue;
            var members = CanonicalWildPopulationCatalogue.DEFAULT.members(population);
            long alphaCount = members.stream().filter(encounter -> descriptor.socialRole(encounter) == WildSocialRole.ALPHA).count();
            long alphaVisualCount = members.stream().filter(encounter -> descriptor.presentationCapabilities(encounter).nativeAlphaVisual()).count();
            long leaderCount = members.stream().filter(encounter -> descriptor.presentationCapabilities(encounter).herdLeaderPresentation()).count();
            assertEquals(1L, alphaCount, population.populationId());
            assertEquals(1L, alphaVisualCount, population.populationId());
            assertEquals(1L, leaderCount, population.populationId());
        }
    }

    @Test
    void compatibilityDescriptorDoesNotImplicitlyGrantPresentationCapabilities() {
        var base = MareaWildEcologyContent.descriptors().getFirst();
        var descriptor = new WildEcologyDescriptorRegistry.Descriptor(
                "test.no-implicit-presentation",
                base.populationSelector(), base.worldEligibility(), base.projectionProfiles(), base.blueprintSource(),
                base.projectionEligibility(), base.behaviorProfile(), encounter -> WildSocialRole.ALPHA);
        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID).orElseThrow();
        assertEquals(WildSocialRole.ALPHA, descriptor.socialRole(encounter));
        assertEquals(WildEcologyDescriptorRegistry.PresentationCapabilities.NONE, descriptor.presentationCapabilities(encounter));
    }

    @Test
    void nonAlphaRoleCannotAcquireAlphaPresentationCapabilities() {
        var base = MareaWildEcologyContent.descriptors().getFirst();
        var descriptor = new WildEcologyDescriptorRegistry.Descriptor(
                "test.member-alpha-capability",
                base.populationSelector(), base.worldEligibility(), base.projectionProfiles(), base.blueprintSource(),
                base.projectionEligibility(), base.behaviorProfile(), encounter -> WildSocialRole.MEMBER,
                encounter -> WildEcologyDescriptorRegistry.PresentationCapabilities.ALPHA_HERD_LEADER);
        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(CanonicalWildEncounterCatalogue.MAREA_FIRST_FLETCHLING_ID).orElseThrow();
        assertEquals(WildSocialRole.MEMBER, descriptor.socialRole(encounter));
        assertEquals(WildEcologyDescriptorRegistry.PresentationCapabilities.NONE, descriptor.presentationCapabilities(encounter));
    }

    @Test
    void descriptorRejectsDuplicateProjectionCalendarsForOnePopulation() {
        var profile = MareaWildEcologyContent.projectionProfiles().getFirst();
        var base = MareaWildEcologyContent.descriptors().getFirst();
        boolean rejected = false;
        try {
            new WildEcologyDescriptorRegistry.Descriptor(
                    "test.duplicate-calendar", base.populationSelector(), base.worldEligibility(), java.util.List.of(profile, profile),
                    base.blueprintSource(), base.projectionEligibility(), base.behaviorProfile());
        } catch (IllegalArgumentException expected) {
            rejected = expected.getMessage().contains("multiple projection profiles");
        }
        assertTrue(rejected);
    }
}

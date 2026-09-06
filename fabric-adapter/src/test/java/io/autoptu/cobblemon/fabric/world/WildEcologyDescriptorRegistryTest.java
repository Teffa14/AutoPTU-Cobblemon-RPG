package io.autoptu.cobblemon.fabric.world;

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
        var migrating = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOWER_SHELF_POPULATION_ID)
                .orElseThrow();
        var resident = CanonicalWildPopulationCatalogue.DEFAULT
                .population(CanonicalWildPopulationCatalogue.MAREA_LOMA_WINDBREAK_POPULATION_ID)
                .orElseThrow();

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
    }

    @Test
    void descriptorRejectsDuplicateProjectionCalendarsForOnePopulation() {
        var profile = MareaWildEcologyContent.projectionProfiles().getFirst();
        var base = MareaWildEcologyContent.descriptors().getFirst();

        boolean rejected = false;
        try {
            new WildEcologyDescriptorRegistry.Descriptor(
                    "test.duplicate-calendar",
                    base.populationSelector(),
                    base.worldEligibility(),
                    java.util.List.of(profile, profile),
                    base.blueprintSource(),
                    base.projectionEligibility(),
                    base.behaviorProfile());
        } catch (IllegalArgumentException expected) {
            rejected = expected.getMessage().contains("multiple projection profiles");
        }
        assertTrue(rejected);
    }
}

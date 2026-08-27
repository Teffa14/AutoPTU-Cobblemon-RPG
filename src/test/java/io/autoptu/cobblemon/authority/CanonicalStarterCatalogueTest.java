package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalStarterCatalogueTest {
    @Test
    void exposesOnlyConfiguredStarterChoicesInStableOrder() {
        CanonicalStarterCatalogue catalogue = new CanonicalStarterCatalogue();

        assertEquals(
                List.of("bulbasaur", "charmander", "squirtle"),
                catalogue.configuredStarters().stream()
                        .map(CanonicalStarterCatalogue.StarterOption::speciesId)
                        .toList()
        );
    }

    @Test
    void resolvesConfiguredSpeciesCaseInsensitivelyAndRejectsUnknownSpecies() {
        CanonicalStarterCatalogue catalogue = new CanonicalStarterCatalogue();

        assertEquals("charmander", catalogue.findConfigured(" Charmander ").orElseThrow().speciesId());
        assertTrue(catalogue.findConfigured("mewtwo").isEmpty());
    }
}

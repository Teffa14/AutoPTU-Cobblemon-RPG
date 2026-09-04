package io.autoptu.cobblemon.fabric.platform;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

final class CobblemonPokemonActorPlatformTest {
    @Test
    void normalizesServerAuthoredIdentifiersWithoutReadingPokemonState() {
        assertEquals("fletchling", CobblemonPokemonActorPlatform.requireIdentifier("  fletchling  ", "speciesId"));
    }

    @Test
    void rejectsMissingServerAuthoredIdentifiers() {
        assertThrows(IllegalArgumentException.class,
                () -> CobblemonPokemonActorPlatform.requireIdentifier("   ", "speciesId"));
        assertThrows(IllegalArgumentException.class,
                () -> CobblemonPokemonActorPlatform.requireIdentifier(null, "speciesId"));
    }
}

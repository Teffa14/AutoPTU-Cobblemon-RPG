package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalPokemonAdminInspectionServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void readsCanonicalPokemonDirectlyByServerOwnedIdWithoutInventingMissingPtuState() {
        FileCanonicalPokemonRepository repository = new FileCanonicalPokemonRepository(tempDir);
        repository.createPokemonIfAbsent(new CanonicalPokemonState(
                "pokemon-42",
                "player-7",
                "bulbasaur",
                8,
                Set.of("overland", "cut"),
                3L));

        var inspection = new CanonicalPokemonAdminInspectionService(repository)
                .inspect(" pokemon-42 ")
                .orElseThrow();

        assertEquals("pokemon-42", inspection.pokemonId());
        assertEquals("player-7", inspection.ownerPlayerId());
        assertEquals("bulbasaur", inspection.speciesId());
        assertEquals(8, inspection.level());
        assertEquals(3L, inspection.revision());
        assertEquals(java.util.List.of("cut", "overland"), inspection.capabilities());
        assertTrue(inspection.statuses().isEmpty());
        assertEquals(null, inspection.health());
        assertEquals(null, inspection.combatStats());
        assertEquals(null, inspection.moveLoadout());
        assertEquals(null, inspection.baseMovement());
        assertEquals(null, inspection.battleTraits());
        assertEquals(null, inspection.accuracyEvasion());
        assertEquals(null, inspection.injuryState());
        assertEquals(null, inspection.heldItemInstanceId());
    }

    @Test
    void returnsEmptyForUnknownOrBlankPokemonId() {
        var service = new CanonicalPokemonAdminInspectionService(new FileCanonicalPokemonRepository(tempDir));

        assertTrue(service.inspect("missing").isEmpty());
        assertTrue(service.inspect("  ").isEmpty());
        assertTrue(service.inspect(null).isEmpty());
    }
}

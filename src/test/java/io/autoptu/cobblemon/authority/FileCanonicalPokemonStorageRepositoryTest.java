package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCanonicalPokemonStorageRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void persistsStorageAcrossRepositoryRecreationAndRejectsStaleWrites() {
        FileCanonicalPokemonStorageRepository first = new FileCanonicalPokemonStorageRepository(tempDir);
        CanonicalPokemonStorageState empty = first.findOrCreate("player-1");
        assertEquals(List.of(), empty.pokemonIds());
        assertEquals(0L, empty.revision());

        CanonicalPokemonStorageState boxed = new CanonicalPokemonStorageState("player-1", List.of("poke-a", "poke-b"), 1L);
        assertTrue(first.replaceIfRevision("player-1", 0L, boxed));

        FileCanonicalPokemonStorageRepository reopened = new FileCanonicalPokemonStorageRepository(tempDir);
        assertEquals(boxed, reopened.findStorage("player-1").orElseThrow());
        assertFalse(reopened.replaceIfRevision(
                "player-1",
                0L,
                new CanonicalPokemonStorageState("player-1", List.of("poke-a"), 1L)
        ));
    }
}

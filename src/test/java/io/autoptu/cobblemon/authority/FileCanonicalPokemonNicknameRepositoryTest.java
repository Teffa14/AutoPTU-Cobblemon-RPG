package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCanonicalPokemonNicknameRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void nicknameSurvivesRepositoryRecreationAndRevisionCas() {
        FileCanonicalPokemonNicknameRepository first = new FileCanonicalPokemonNicknameRepository(tempDir);
        CanonicalPokemonNicknameState initial = new CanonicalPokemonNicknameState("pkmn-1", "player-1", "Sparky", 0);
        assertTrue(first.createNicknameIfAbsent(initial));
        assertFalse(first.createNicknameIfAbsent(initial));

        FileCanonicalPokemonNicknameRepository reopened = new FileCanonicalPokemonNicknameRepository(tempDir);
        assertEquals(initial, reopened.findNickname("pkmn-1").orElseThrow());

        CanonicalPokemonNicknameState replacement = new CanonicalPokemonNicknameState("pkmn-1", "player-1", "Volt", 1);
        assertTrue(reopened.replaceNicknameIfRevision("pkmn-1", 0, replacement));
        assertFalse(first.replaceNicknameIfRevision("pkmn-1", 0, replacement));
        assertEquals(replacement, new FileCanonicalPokemonNicknameRepository(tempDir).findNickname("pkmn-1").orElseThrow());
    }
}

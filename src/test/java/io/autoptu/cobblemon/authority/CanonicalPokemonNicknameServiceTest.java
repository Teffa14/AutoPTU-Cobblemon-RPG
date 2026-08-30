package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPokemonNicknameServiceTest {
    @TempDir Path tempDir;

    @Test
    void resolvesPartySlotAndOwnershipServerSideThenPersistsNickname() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        FileCanonicalPokemonNicknameRepository nicknames = new FileCanonicalPokemonNicknameRepository(tempDir);
        parties.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                "player-1", List.of("pkmn-1"), Map.of(),
                new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1), 0));
        pokemon.createPokemonIfAbsent(new CanonicalPokemonState("pkmn-1", "player-1", "pikachu", 5, Set.of(), 0));

        CanonicalPokemonNicknameService service = new CanonicalPokemonNicknameService(parties, pokemon, nicknames);
        CanonicalPokemonNicknameService.Decision applied = service.setNickname("player-1", 1, "  Sparky  ");
        assertEquals(CanonicalPokemonNicknameService.Outcome.APPLIED, applied.outcome());
        assertEquals("Sparky", applied.nickname());
        assertEquals("Sparky", new FileCanonicalPokemonNicknameRepository(tempDir).findNickname("pkmn-1").orElseThrow().nickname());

        CanonicalPokemonNicknameService.Decision repeat = service.setNickname("player-1", 1, "Sparky");
        assertEquals(CanonicalPokemonNicknameService.Outcome.ALREADY_SET, repeat.outcome());
        assertEquals(0, repeat.revision());
    }

    @Test
    void rejectsInvalidNameAndOwnerMismatchWithoutMutation() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        FileCanonicalPokemonNicknameRepository nicknames = new FileCanonicalPokemonNicknameRepository(tempDir);
        parties.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                "player-1", List.of("pkmn-foreign"), Map.of(),
                new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1), 0));
        pokemon.createPokemonIfAbsent(new CanonicalPokemonState("pkmn-foreign", "player-2", "eevee", 5, Set.of(), 0));
        CanonicalPokemonNicknameService service = new CanonicalPokemonNicknameService(parties, pokemon, nicknames);

        assertEquals(CanonicalPokemonNicknameService.Outcome.INVALID_NAME,
                service.setNickname("player-1", 1, "\n").outcome());
        assertEquals(CanonicalPokemonNicknameService.Outcome.NOT_OWNER,
                service.setNickname("player-1", 1, "Buddy").outcome());
        assertTrue(nicknames.findNickname("pkmn-foreign").isEmpty());
    }
}

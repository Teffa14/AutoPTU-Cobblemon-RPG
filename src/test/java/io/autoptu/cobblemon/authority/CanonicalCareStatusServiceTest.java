package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalCareStatusServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void projectsOnlyPersistedCareStateInCanonicalPartyOrder() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        String playerId = "minecraft-player:test";

        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "poke-a", playerId, "bulbasaur", 8,
                Set.of(), Set.of("poisoned", "burned"), CanonicalStatusState.fromNames(Set.of("poisoned", "burned")),
                null, new CanonicalHealth(7, 29), null, null, null, null,
                new CanonicalInjuryState(2), null, 4L
        ));
        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "poke-b", playerId, "squirtle", 7, Set.of(), 1L
        ));
        parties.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                playerId, List.of("poke-a", "poke-b"), Map.of(), arena(), 6L
        ));

        CanonicalCareStatusService.Summary summary = new CanonicalCareStatusService(parties, pokemon)
                .findStatus(playerId).orElseThrow();

        assertEquals(6L, summary.partyRevision());
        assertEquals(2, summary.members().size());
        CanonicalCareStatusService.Member first = summary.members().get(0);
        assertEquals(1, first.slot());
        assertEquals("bulbasaur", first.speciesId());
        assertEquals(7, first.currentHp());
        assertEquals(29, first.maxHp());
        assertEquals(List.of("burned", "poisoned"), first.statuses());
        assertEquals(2, first.injuries());
        assertEquals(4L, first.pokemonRevision());

        CanonicalCareStatusService.Member second = summary.members().get(1);
        assertFalse(second.hasHealth());
        assertTrue(second.statuses().isEmpty());
        assertFalse(second.hasInjuryState());
    }

    @Test
    void survivesRepositoryRecreationWithoutDerivingMissingCareValues() {
        String playerId = "minecraft-player:test";
        new FileCanonicalPokemonRepository(tempDir).createPokemonIfAbsent(new CanonicalPokemonState(
                "poke-a", playerId, "pikachu", 5, Set.of(), Set.of("asleep"),
                CanonicalStatusState.fromNames(Set.of("asleep")), null, new CanonicalHealth(11, 20),
                null, null, null, null, new CanonicalInjuryState(1), null, 3L
        ));
        new FileCanonicalPlayerEncounterProfileRepository(tempDir).createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                playerId, List.of("poke-a"), Map.of(), arena(), 2L
        ));

        CanonicalCareStatusService.Summary reloaded = new CanonicalCareStatusService(
                new FileCanonicalPlayerEncounterProfileRepository(tempDir),
                new FileCanonicalPokemonRepository(tempDir)
        ).findStatus(playerId).orElseThrow();

        assertEquals(11, reloaded.members().get(0).currentHp());
        assertEquals(List.of("asleep"), reloaded.members().get(0).statuses());
        assertEquals(1, reloaded.members().get(0).injuries());
    }

    @Test
    void returnsEmptyWithoutPersistentParty() {
        CanonicalCareStatusService service = new CanonicalCareStatusService(
                new FileCanonicalPlayerEncounterProfileRepository(tempDir),
                new FileCanonicalPokemonRepository(tempDir)
        );
        assertTrue(service.findStatus("minecraft-player:none").isEmpty());
    }

    @Test
    void failsClosedForForeignOwnedPartyReference() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "foreign", "minecraft-player:other", "eevee", 5, Set.of(), 0L
        ));
        parties.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                "minecraft-player:test", List.of("foreign"), Map.of(), arena(), 0L
        ));

        CanonicalCareStatusService service = new CanonicalCareStatusService(parties, pokemon);
        assertThrows(IllegalStateException.class, () -> service.findStatus("minecraft-player:test"));
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1);
    }
}

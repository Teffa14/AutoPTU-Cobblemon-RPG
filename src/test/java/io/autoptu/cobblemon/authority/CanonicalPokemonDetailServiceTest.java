package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPokemonDetailServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void returnsDetailedCanonicalStateForOwnedPartySlot() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        pokemon.createPokemonIfAbsent(detailedPokemon("pokemon-1", "player-1"));
        parties.createProfileIfAbsent(profile("player-1", List.of("pokemon-1")));

        CanonicalPokemonDetail detail = new CanonicalPokemonDetailService(parties, pokemon)
                .findPokemon("player-1", 1)
                .orElseThrow();

        assertEquals(1, detail.slot());
        assertEquals("pokemon-1", detail.pokemonId());
        assertEquals("bulbasaur", detail.speciesId());
        assertEquals(8, detail.level());
        assertEquals(new CanonicalHealth(20, 30), detail.health());
        assertEquals(List.of("burn"), detail.statuses());
        assertEquals(new CanonicalCombatStats(10, 11, 12, 13, 14), detail.combatStats());
        assertEquals(List.of("tackle", "vine-whip"), detail.moveLoadout().moveIds());
        assertEquals(new CanonicalBaseMovement(5, 2, 0, 1, 1), detail.baseMovement());
        assertEquals(List.of("grass", "poison"), detail.battleTraits().types());
        assertEquals(List.of("overgrow"), detail.battleTraits().abilities());
        assertEquals(new CanonicalAccuracyEvasion(1, 2, 3, 4), detail.accuracyEvasion());
        assertEquals(new CanonicalInjuryState(1), detail.injuryState());
        assertTrue(detail.heldItemEquipped());
        assertEquals(List.of("cut", "overland"), detail.capabilities());
        assertEquals(4L, detail.revision());
    }

    @Test
    void preservesUnavailableOptionalStateInsteadOfInventingValues() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "pokemon-1", "player-1", "squirtle", 5, Set.of(), 0L
        ));
        parties.createProfileIfAbsent(profile("player-1", List.of("pokemon-1")));

        CanonicalPokemonDetail detail = new CanonicalPokemonDetailService(parties, pokemon)
                .findPokemon("player-1", 1)
                .orElseThrow();

        assertEquals(null, detail.health());
        assertEquals(null, detail.combatStats());
        assertEquals(null, detail.moveLoadout());
        assertEquals(null, detail.baseMovement());
        assertEquals(null, detail.battleTraits());
        assertEquals(null, detail.accuracyEvasion());
        assertEquals(null, detail.injuryState());
        assertFalse(detail.heldItemEquipped());
    }

    @Test
    void returnsEmptyForMissingPartyOrSlotOutsideParty() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        CanonicalPokemonDetailService service = new CanonicalPokemonDetailService(parties, pokemon);

        assertTrue(service.findPokemon("player-1", 1).isEmpty());

        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "pokemon-1", "player-1", "bulbasaur", 5, Set.of(), 0L
        ));
        parties.createProfileIfAbsent(profile("player-1", List.of("pokemon-1")));

        assertTrue(service.findPokemon("player-1", 2).isEmpty());
        assertTrue(service.findPokemon("player-1", 0).isEmpty());
    }

    @Test
    void rejectsPartyReferenceOwnedByAnotherPlayer() {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                "pokemon-1", "other-player", "bulbasaur", 5, Set.of(), 0L
        ));
        parties.createProfileIfAbsent(profile("player-1", List.of("pokemon-1")));

        boolean rejected = false;
        try {
            new CanonicalPokemonDetailService(parties, pokemon).findPokemon("player-1", 1);
        } catch (IllegalStateException expected) {
            rejected = true;
        }
        assertTrue(rejected);
    }

    private static CanonicalPokemonState detailedPokemon(String pokemonId, String ownerId) {
        return new CanonicalPokemonState(
                pokemonId,
                ownerId,
                "bulbasaur",
                8,
                Set.of("overland", "cut"),
                Set.of("burn"),
                CanonicalStatusState.fromNames(Set.of("burn")),
                new CanonicalCombatStats(10, 11, 12, 13, 14),
                new CanonicalHealth(20, 30),
                new CanonicalMoveLoadout(List.of("tackle", "vine-whip")),
                new CanonicalBaseMovement(5, 2, 0, 1, 1),
                new CanonicalBattleTraits(List.of("grass", "poison"), List.of("overgrow")),
                new CanonicalAccuracyEvasion(1, 2, 3, 4),
                new CanonicalInjuryState(1),
                "item-1",
                4L
        );
    }

    private static CanonicalPlayerEncounterProfile profile(String playerId, List<String> pokemonIds) {
        return new CanonicalPlayerEncounterProfile(
                playerId,
                pokemonIds,
                Map.of(),
                new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1),
                0L
        );
    }
}

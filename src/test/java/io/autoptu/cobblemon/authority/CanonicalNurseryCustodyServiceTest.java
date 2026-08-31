package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalNurseryCustodyServiceTest {
    @TempDir Path root;

    @Test
    void enrollmentLeavesPokemonInDurableCustodyAndReleaseReturnsItToBox() {
        Fixture fixture = fixture("player-1", "pokemon-1");

        CanonicalNurseryCustodyService.NurserySummary enrolled = fixture.service.enrollFromBox(
                "player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY, 1);

        assertEquals(List.of("pokemon-1"), enrolled.members().stream().map(CanonicalNurseryCustodyService.Member::pokemonId).toList());
        assertFalse(fixture.storage.findOrCreate("player-1").pokemonIds().contains("pokemon-1"));

        CanonicalNurseryCustodyService reopened = new CanonicalNurseryCustodyService(
                new FileCanonicalNurseryRepository(root),
                new FileCanonicalPokemonStorageRepository(root),
                new FileCanonicalPokemonRepository(root));
        assertEquals(List.of("pokemon-1"), reopened.inspect("player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY)
                .members().stream().map(CanonicalNurseryCustodyService.Member::pokemonId).toList());

        reopened.releaseToBox("player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY, 1);
        assertTrue(new FileCanonicalPokemonStorageRepository(root).findOrCreate("player-1").pokemonIds().contains("pokemon-1"));
        assertTrue(reopened.inspect("player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY).members().isEmpty());
    }

    @Test
    void restartRecoveryRemovesInterruptedReleaseDuplicateFromBox() {
        Fixture fixture = fixture("player-1", "pokemon-1");
        fixture.service.enrollFromBox("player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY, 1);

        CanonicalPokemonStorageState box = fixture.storage.findOrCreate("player-1");
        assertTrue(fixture.storage.replaceIfRevision("player-1", box.revision(),
                new CanonicalPokemonStorageState("player-1", List.of("pokemon-1"), box.revision() + 1)));

        CanonicalNurseryCustodyService reopened = new CanonicalNurseryCustodyService(
                new FileCanonicalNurseryRepository(root),
                new FileCanonicalPokemonStorageRepository(root),
                new FileCanonicalPokemonRepository(root));
        assertEquals(1, reopened.recoverCustody());
        assertFalse(new FileCanonicalPokemonStorageRepository(root).findOrCreate("player-1").pokemonIds().contains("pokemon-1"));
        assertEquals(1, reopened.inspect("player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY).members().size());
    }

    @Test
    void ownershipCapacityUnknownFacilityAndStaleRevisionFailClosed() {
        Fixture fixture = fixture("player-1", "pokemon-1");
        assertThrows(IllegalArgumentException.class, () -> fixture.service.inspect("player-1", "client_fake_nursery"));

        FileCanonicalNurseryRepository repository = fixture.nursery;
        FileCanonicalNurseryRepository.NurseryState baseline = repository.findOrCreate(
                "player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY);
        FileCanonicalNurseryRepository.NurseryState advanced = new FileCanonicalNurseryRepository.NurseryState(
                "player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY, List.of(), baseline.revision() + 1);
        assertTrue(repository.replaceIfRevision(advanced, baseline.revision()));
        assertFalse(repository.replaceIfRevision(
                new FileCanonicalNurseryRepository.NurseryState("player-1", CanonicalNurseryCustodyService.CEDAR_NURSERY,
                        List.of(), advanced.revision() + 1), baseline.revision()));
    }

    private Fixture fixture(String playerId, String pokemonId) {
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(root);
        FileCanonicalPokemonStorageRepository storage = new FileCanonicalPokemonStorageRepository(root);
        FileCanonicalNurseryRepository nursery = new FileCanonicalNurseryRepository(root);
        assertTrue(pokemon.createPokemonIfAbsent(new CanonicalPokemonState(
                pokemonId, playerId, "cobblemon:eevee", 5, Set.of(), 0L)));
        CanonicalPokemonStorageState empty = storage.findOrCreate(playerId);
        assertTrue(storage.replaceIfRevision(playerId, empty.revision(),
                new CanonicalPokemonStorageState(playerId, List.of(pokemonId), empty.revision() + 1)));
        return new Fixture(storage, nursery, new CanonicalNurseryCustodyService(nursery, storage, pokemon));
    }

    private record Fixture(
            FileCanonicalPokemonStorageRepository storage,
            FileCanonicalNurseryRepository nursery,
            CanonicalNurseryCustodyService service
    ) {}
}
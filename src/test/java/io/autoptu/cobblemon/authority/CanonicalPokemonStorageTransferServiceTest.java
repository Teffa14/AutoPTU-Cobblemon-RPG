package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CanonicalPokemonStorageTransferServiceTest {
    private static final String PLAYER = "player-1";
    private static final BattleArenaSnapshot ARENA =
            new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1);

    @TempDir
    Path root;

    @Test
    void depositAndWithdrawMoveOneOwnedPokemonWithoutOverlap() {
        Fixture fixture = fixture(List.of("pokemon-1", "pokemon-2"), List.of());

        CanonicalPokemonStorageTransferService.TransferResult deposited =
                fixture.service.deposit("transfer-deposit", PLAYER, 2);

        assertEquals(FileCanonicalPokemonTransferRepository.Direction.DEPOSIT, deposited.direction());
        assertEquals("pokemon-2", deposited.pokemonId());
        assertEquals(List.of("pokemon-1"), fixture.parties.findProfile(PLAYER).orElseThrow().pokemonIds());
        assertEquals(List.of("pokemon-2"), fixture.storage.findOrCreate(PLAYER).pokemonIds());

        CanonicalPokemonStorageTransferService.TransferResult retried =
                fixture.service.deposit("transfer-deposit", PLAYER, 2);
        assertEquals(deposited, retried);
        assertEquals(List.of("pokemon-1"), fixture.parties.findProfile(PLAYER).orElseThrow().pokemonIds());
        assertEquals(List.of("pokemon-2"), fixture.storage.findOrCreate(PLAYER).pokemonIds());

        CanonicalPokemonStorageTransferService.TransferResult withdrawn =
                fixture.service.withdraw("transfer-withdraw", PLAYER, 1);

        assertEquals(FileCanonicalPokemonTransferRepository.Direction.WITHDRAW, withdrawn.direction());
        assertEquals("pokemon-2", withdrawn.pokemonId());
        assertEquals(List.of("pokemon-1", "pokemon-2"), fixture.parties.findProfile(PLAYER).orElseThrow().pokemonIds());
        assertEquals(List.of(), fixture.storage.findOrCreate(PLAYER).pokemonIds());
    }

    @Test
    void refusesToDepositLastActivePartyMember() {
        Fixture fixture = fixture(List.of("pokemon-1"), List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> fixture.service.deposit("transfer-last", PLAYER, 1)
        );
        assertEquals(List.of("pokemon-1"), fixture.parties.findProfile(PLAYER).orElseThrow().pokemonIds());
        assertEquals(List.of(), fixture.storage.findOrCreate(PLAYER).pokemonIds());
    }

    @Test
    void restartRecoveryFinishesDepositWhenSourceWasAlreadyRemovedBeforeJournalAdvanced() {
        Fixture fixture = fixture(List.of("pokemon-1", "pokemon-2"), List.of());
        fixture.transfers.createIfAbsent(new FileCanonicalPokemonTransferRepository.TransferAttempt(
                "transfer-restart",
                PLAYER,
                FileCanonicalPokemonTransferRepository.Direction.DEPOSIT,
                "pokemon-2",
                FileCanonicalPokemonTransferRepository.Stage.CREATED
        ));

        CanonicalPlayerEncounterProfile party = fixture.parties.findProfile(PLAYER).orElseThrow();
        CanonicalPlayerEncounterProfile afterRemoval = new CanonicalPlayerEncounterProfile(
                PLAYER,
                List.of("pokemon-1"),
                party.consumableQuantities(),
                party.arena(),
                party.revision() + 1
        );
        assertEquals(true, fixture.parties.replaceProfileIfRevision(PLAYER, party.revision(), afterRemoval));

        CanonicalPokemonStorageTransferService restarted = new CanonicalPokemonStorageTransferService(
                new FileCanonicalPlayerEncounterProfileRepository(root),
                new FileCanonicalPokemonStorageRepository(root),
                new FileCanonicalPokemonRepository(root),
                new FileCanonicalPokemonTransferRepository(root)
        );
        restarted.recoverPending();

        FileCanonicalPlayerEncounterProfileRepository partiesAfterRestart =
                new FileCanonicalPlayerEncounterProfileRepository(root);
        FileCanonicalPokemonStorageRepository storageAfterRestart =
                new FileCanonicalPokemonStorageRepository(root);
        FileCanonicalPokemonTransferRepository transfersAfterRestart =
                new FileCanonicalPokemonTransferRepository(root);
        assertEquals(List.of("pokemon-1"), partiesAfterRestart.findProfile(PLAYER).orElseThrow().pokemonIds());
        assertEquals(List.of("pokemon-2"), storageAfterRestart.findOrCreate(PLAYER).pokemonIds());
        assertEquals(
                FileCanonicalPokemonTransferRepository.Stage.COMMITTED,
                transfersAfterRestart.find("transfer-restart").orElseThrow().stage()
        );
    }

    @Test
    void rejectsForeignOwnedPokemonBeforeMutation() {
        Fixture fixture = fixture(List.of("pokemon-1"), List.of("foreign-pokemon"));
        fixture.pokemon.createPokemonIfAbsent(
                new CanonicalPokemonState("foreign-pokemon", "other-player", "cobblemon:eevee", 5, Set.of(), 0L)
        );

        assertThrows(
                IllegalStateException.class,
                () -> fixture.service.withdraw("transfer-foreign", PLAYER, 1)
        );
        assertEquals(List.of("pokemon-1"), fixture.parties.findProfile(PLAYER).orElseThrow().pokemonIds());
        assertEquals(List.of("foreign-pokemon"), fixture.storage.findOrCreate(PLAYER).pokemonIds());
    }

    private Fixture fixture(List<String> partyIds, List<String> boxIds) {
        FileCanonicalPlayerEncounterProfileRepository parties = new FileCanonicalPlayerEncounterProfileRepository(root);
        FileCanonicalPokemonStorageRepository storage = new FileCanonicalPokemonStorageRepository(root);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(root);
        FileCanonicalPokemonTransferRepository transfers = new FileCanonicalPokemonTransferRepository(root);

        parties.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(PLAYER, partyIds, Map.of(), ARENA, 0L));
        CanonicalPokemonStorageState initialStorage = storage.findOrCreate(PLAYER);
        if (!boxIds.isEmpty()) {
            storage.replaceIfRevision(
                    PLAYER,
                    initialStorage.revision(),
                    new CanonicalPokemonStorageState(PLAYER, boxIds, initialStorage.revision() + 1)
            );
        }

        ArrayList<String> owned = new ArrayList<>(partyIds);
        for (String pokemonId : boxIds) {
            if (!pokemonId.equals("foreign-pokemon")) owned.add(pokemonId);
        }
        for (String pokemonId : owned) {
            pokemon.createPokemonIfAbsent(
                    new CanonicalPokemonState(pokemonId, PLAYER, "cobblemon:bulbasaur", 5, Set.of(), 0L)
            );
        }

        return new Fixture(
                parties,
                storage,
                pokemon,
                transfers,
                new CanonicalPokemonStorageTransferService(parties, storage, pokemon, transfers)
        );
    }

    private record Fixture(
            FileCanonicalPlayerEncounterProfileRepository parties,
            FileCanonicalPokemonStorageRepository storage,
            FileCanonicalPokemonRepository pokemon,
            FileCanonicalPokemonTransferRepository transfers,
            CanonicalPokemonStorageTransferService service
    ) {}
}

package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BattleArenaReservationTest {
    @Test
    void battleReservationFreezesServerOwnedArenaPlacement() {
        InMemoryRepository repository = repository();
        BattleAuthorityService service = service(repository);
        BattleArenaSnapshot arena = new BattleArenaSnapshot(
                "minecraft:overworld", 120, 72, -45, 0, 1, -1, 0);

        BattleSnapshotDecision decision = service.reserveBattleInArena(
                "player-1", List.of("pokemon-1"), Map.of(), arena);

        assertTrue(decision.allowed());
        assertEquals(arena, decision.snapshot().arena());
        assertEquals(arena, repository.findSnapshot(decision.snapshot().reservationId()).orElseThrow().arena());
    }

    @Test
    void arenaReservationRejectsMissingPlacementWhileLegacyHeadlessReservationRemainsCompatible() {
        InMemoryRepository repository = repository();
        BattleAuthorityService service = service(repository);

        BattleSnapshotDecision missingArena = service.reserveBattleInArena(
                "player-1", List.of("pokemon-1"), Map.of(), null);
        BattleSnapshotDecision legacy = service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of());

        assertEquals("invalid_battle_arena", missingArena.reason());
        assertTrue(legacy.allowed());
        assertNull(legacy.snapshot().arena());
    }

    @Test
    void arenaSnapshotRejectsNonCardinalOrParallelGridAxes() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 1, 0, 1));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () ->
                new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, -1, 0));
    }

    private static BattleAuthorityService service(InMemoryRepository repository) {
        AtomicInteger ids = new AtomicInteger();
        AtomicLong seeds = new AtomicLong(9000);
        return new BattleAuthorityService(
                repository,
                repository,
                repository,
                () -> "arena-battle-" + ids.incrementAndGet(),
                seeds::incrementAndGet);
    }

    private static InMemoryRepository repository() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.players.put("player-1", new CanonicalPlayerState(
                "player-1", Set.of("Ace Trainer"), Map.of("Command", 4), Set.of("Sky"), 1));
        repository.pokemon.put("pokemon-1", new CanonicalPokemonState(
                "pokemon-1", "player-1", "cobblemon:charizard", 42, Set.of("Sky"), 2));
        return repository;
    }

    private static final class InMemoryRepository
            implements CanonicalStateRepository, CanonicalAssetRepository, BattleSnapshotRepository {
        private final Map<String, CanonicalPlayerState> players = new HashMap<>();
        private final Map<String, CanonicalPokemonState> pokemon = new HashMap<>();
        private final Map<String, BattleAuthoritySnapshot> snapshots = new HashMap<>();

        @Override
        public Optional<CanonicalPlayerState> findPlayer(String playerId) {
            return Optional.ofNullable(players.get(playerId));
        }

        @Override
        public Optional<CanonicalPokemonState> findPokemon(String pokemonId) {
            return Optional.ofNullable(pokemon.get(pokemonId));
        }

        @Override
        public Optional<CanonicalItemInstance> findItem(String itemInstanceId) {
            return Optional.empty();
        }

        @Override
        public Optional<ItemReservation> findReservation(String reservationId) {
            return Optional.empty();
        }

        @Override
        public boolean tryReserveItem(ItemReservation reservation) {
            return false;
        }

        @Override
        public boolean commitItemReservation(String reservationId, String playerId) {
            return false;
        }

        @Override
        public boolean releaseItemReservation(String reservationId, String playerId) {
            return false;
        }

        @Override
        public Optional<BattleAuthoritySnapshot> findSnapshot(String reservationId) {
            return Optional.ofNullable(snapshots.get(reservationId));
        }

        @Override
        public boolean tryReserveSnapshot(BattleAuthoritySnapshot snapshot) {
            if (snapshots.containsKey(snapshot.reservationId())) return false;
            snapshots.put(snapshot.reservationId(), snapshot);
            return true;
        }

        @Override
        public boolean releaseSnapshot(String reservationId, String playerId) {
            BattleAuthoritySnapshot snapshot = snapshots.get(reservationId);
            if (snapshot == null || !snapshot.playerId().equals(playerId)) return false;
            snapshots.remove(reservationId);
            return true;
        }
    }
}

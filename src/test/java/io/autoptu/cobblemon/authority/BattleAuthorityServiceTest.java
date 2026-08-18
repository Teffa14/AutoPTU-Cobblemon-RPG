package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class BattleAuthorityServiceTest {
    @Test
    void snapshotCopiesCanonicalTrainerPokemonHeldItemsAndConsumables() {
        InMemoryRepository repository = standardRepository();
        BattleAuthorityService service = service(repository);

        BattleSnapshotDecision decision = service.reserveBattle(
                "player-1",
                List.of("pokemon-1", "pokemon-2"),
                Map.of("item-potion", 2));

        assertTrue(decision.allowed());
        BattleAuthoritySnapshot snapshot = decision.snapshot();
        assertEquals("battle-1", snapshot.reservationId());
        assertEquals(7001L, snapshot.rngSeed());
        assertEquals(17L, snapshot.trainer().revision());
        assertEquals(Set.of("Ace Trainer"), snapshot.trainer().trainerClasses());
        assertEquals("cobblemon:charizard", snapshot.roster().get(0).speciesId());
        assertEquals(42, snapshot.roster().get(0).level());
        assertEquals("item-charcoal", snapshot.roster().get(0).heldItemInstanceId());

        BattleItemSnapshot held = snapshot.items().stream()
                .filter(BattleItemSnapshot::heldItem)
                .findFirst()
                .orElseThrow();
        assertEquals("autoptu:charcoal", held.templateId());
        assertEquals(1, held.reservedQuantity());
        assertEquals(31L, held.revision());

        BattleItemSnapshot potion = snapshot.items().stream()
                .filter(item -> !item.heldItem())
                .findFirst()
                .orElseThrow();
        assertEquals("autoptu:hyper_potion", potion.templateId());
        assertEquals(2, potion.reservedQuantity());
        assertEquals(44L, potion.revision());
    }

    @Test
    void rejectsOwnershipAndQuantityClaimsBeforeCreatingSnapshot() {
        InMemoryRepository repository = standardRepository();
        repository.putPokemon(new CanonicalPokemonState(
                "pokemon-other", "player-2", "cobblemon:gengar", 40, Set.of("Phasing"), 3));
        BattleAuthorityService service = service(repository);

        BattleSnapshotDecision foreignPokemon = service.reserveBattle(
                "player-1", List.of("pokemon-other"), Map.of());
        BattleSnapshotDecision forgedQuantity = service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 999));

        assertFalse(foreignPokemon.allowed());
        assertEquals("pokemon_not_owned:pokemon-other", foreignPokemon.reason());
        assertFalse(forgedQuantity.allowed());
        assertEquals("insufficient_quantity:item-potion", forgedQuantity.reason());
        assertTrue(repository.snapshots.isEmpty());
    }

    @Test
    void atomicReservationRejectsPokemonChangedAfterServiceRead() {
        InMemoryRepository repository = standardRepository();
        repository.bumpPokemonBeforeNextBattleReserve = "pokemon-1";
        BattleAuthorityService service = service(repository);

        BattleSnapshotDecision decision = service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of());

        assertFalse(decision.allowed());
        assertEquals("state_changed_or_assets_reserved", decision.reason());
        assertEquals(9L, repository.findPokemon("pokemon-1").orElseThrow().revision());
        assertTrue(repository.snapshots.isEmpty());
    }

    @Test
    void atomicReservationRejectsHeldItemChangedAfterServiceRead() {
        InMemoryRepository repository = standardRepository();
        repository.bumpItemBeforeNextBattleReserve = "item-charcoal";
        BattleAuthorityService service = service(repository);

        BattleSnapshotDecision decision = service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of());

        assertFalse(decision.allowed());
        assertEquals("state_changed_or_assets_reserved", decision.reason());
        assertEquals(32L, repository.findItem("item-charcoal").orElseThrow().revision());
    }

    @Test
    void acceptedSnapshotDoesNotChangeWhenLiveCanonicalStateLaterChanges() {
        InMemoryRepository repository = standardRepository();
        BattleAuthorityService service = service(repository);
        BattleAuthoritySnapshot snapshot = service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 1)).snapshot();

        CanonicalPokemonState live = repository.findPokemon("pokemon-1").orElseThrow();
        repository.putPokemon(new CanonicalPokemonState(
                live.pokemonId(), live.ownerPlayerId(), "cobblemon:magikarp", 99,
                Set.of("Swim"), null, live.revision() + 1));
        CanonicalItemInstance potion = repository.findItem("item-potion").orElseThrow();
        repository.putItem(new CanonicalItemInstance(
                potion.itemInstanceId(), potion.ownerPlayerId(), "client:forged_item", 999, potion.revision() + 1));

        assertEquals("cobblemon:charizard", snapshot.roster().get(0).speciesId());
        assertEquals(42, snapshot.roster().get(0).level());
        assertEquals("item-charcoal", snapshot.roster().get(0).heldItemInstanceId());
        assertEquals("autoptu:hyper_potion", snapshot.items().stream()
                .filter(item -> item.itemInstanceId().equals("item-potion"))
                .findFirst().orElseThrow().templateId());
        assertEquals(1, snapshot.items().stream()
                .filter(item -> item.itemInstanceId().equals("item-potion"))
                .findFirst().orElseThrow().reservedQuantity());
    }

    @Test
    void lockedBattleAssetsCannotEnterSecondBattleUntilReleased() {
        InMemoryRepository repository = standardRepository();
        BattleAuthorityService service = service(repository);

        BattleSnapshotDecision first = service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-potion", 1));
        BattleSnapshotDecision second = service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of());

        assertTrue(first.allowed());
        assertFalse(second.allowed());
        assertEquals("state_changed_or_assets_reserved", second.reason());

        assertTrue(service.releaseBattle("player-1", first.snapshot().reservationId()).allowed());
        assertTrue(service.reserveBattle("player-1", List.of("pokemon-1"), Map.of()).allowed());
    }

    @Test
    void anotherPlayerCannotReleaseBattleReservation() {
        InMemoryRepository repository = standardRepository();
        BattleAuthorityService service = service(repository);
        BattleAuthoritySnapshot snapshot = service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of()).snapshot();

        BattleSnapshotDecision forgedRelease = service.releaseBattle("player-2", snapshot.reservationId());

        assertFalse(forgedRelease.allowed());
        assertEquals("battle_reservation_not_owned", forgedRelease.reason());
        assertTrue(repository.findSnapshot(snapshot.reservationId()).isPresent());
    }

    @Test
    void duplicateRosterAndHeldItemConsumableOverlapFailClosed() {
        InMemoryRepository repository = standardRepository();
        BattleAuthorityService service = service(repository);

        assertEquals("invalid_roster", service.reserveBattle(
                "player-1", List.of("pokemon-1", "pokemon-1"), Map.of()).reason());
        assertEquals("item_role_conflict:item-charcoal", service.reserveBattle(
                "player-1", List.of("pokemon-1"), Map.of("item-charcoal", 1)).reason());
    }

    private static BattleAuthorityService service(InMemoryRepository repository) {
        AtomicInteger ids = new AtomicInteger();
        AtomicLong seeds = new AtomicLong(7000);
        return new BattleAuthorityService(
                repository,
                repository,
                repository,
                () -> "battle-" + ids.incrementAndGet(),
                seeds::incrementAndGet);
    }

    private static InMemoryRepository standardRepository() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.putPlayer(new CanonicalPlayerState(
                "player-1", Set.of("Ace Trainer"), Map.of("Command", 4), Set.of("Sky"), 17));
        repository.putPokemon(new CanonicalPokemonState(
                "pokemon-1", "player-1", "cobblemon:charizard", 42,
                Set.of("Sky", "Power 5"), "item-charcoal", 8));
        repository.putPokemon(new CanonicalPokemonState(
                "pokemon-2", "player-1", "cobblemon:blastoise", 41,
                Set.of("Swim", "Power 5"), null, 12));
        repository.putItem(new CanonicalItemInstance(
                "item-charcoal", "player-1", "autoptu:charcoal", 1, 31));
        repository.putItem(new CanonicalItemInstance(
                "item-potion", "player-1", "autoptu:hyper_potion", 4, 44));
        return repository;
    }

    private static final class InMemoryRepository
            implements CanonicalStateRepository, CanonicalAssetRepository, BattleSnapshotRepository {
        private final Map<String, CanonicalPlayerState> players = new HashMap<>();
        private final Map<String, CanonicalPokemonState> pokemon = new HashMap<>();
        private final Map<String, CanonicalItemInstance> items = new HashMap<>();
        private final Map<String, BattleAuthoritySnapshot> snapshots = new HashMap<>();
        private final Set<String> lockedPokemon = new HashSet<>();
        private final Set<String> lockedItems = new HashSet<>();
        private String bumpPokemonBeforeNextBattleReserve;
        private String bumpItemBeforeNextBattleReserve;

        void putPlayer(CanonicalPlayerState state) {
            players.put(state.playerId(), state);
        }

        void putPokemon(CanonicalPokemonState state) {
            pokemon.put(state.pokemonId(), state);
        }

        void putItem(CanonicalItemInstance state) {
            items.put(state.itemInstanceId(), state);
        }

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
            return Optional.ofNullable(items.get(itemInstanceId));
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
        public synchronized boolean tryReserveSnapshot(BattleAuthoritySnapshot snapshot) {
            if (bumpPokemonBeforeNextBattleReserve != null) {
                CanonicalPokemonState state = pokemon.get(bumpPokemonBeforeNextBattleReserve);
                pokemon.put(state.pokemonId(), new CanonicalPokemonState(
                        state.pokemonId(), state.ownerPlayerId(), state.speciesId(), state.level(),
                        state.capabilities(), state.heldItemInstanceId(), state.revision() + 1));
                bumpPokemonBeforeNextBattleReserve = null;
            }
            if (bumpItemBeforeNextBattleReserve != null) {
                CanonicalItemInstance state = items.get(bumpItemBeforeNextBattleReserve);
                items.put(state.itemInstanceId(), new CanonicalItemInstance(
                        state.itemInstanceId(), state.ownerPlayerId(), state.templateId(),
                        state.quantity(), state.revision() + 1));
                bumpItemBeforeNextBattleReserve = null;
            }

            CanonicalPlayerState player = players.get(snapshot.playerId());
            if (player == null || player.revision() != snapshot.trainer().revision()) {
                return false;
            }
            if (snapshots.containsKey(snapshot.reservationId())) {
                return false;
            }

            for (BattlePokemonSnapshot requested : snapshot.roster()) {
                CanonicalPokemonState live = pokemon.get(requested.pokemonId());
                if (live == null
                        || !live.ownerPlayerId().equals(snapshot.playerId())
                        || live.revision() != requested.revision()
                        || !live.speciesId().equals(requested.speciesId())
                        || live.level() != requested.level()
                        || !Objects.equals(live.heldItemInstanceId(), requested.heldItemInstanceId())
                        || lockedPokemon.contains(requested.pokemonId())) {
                    return false;
                }
            }

            for (BattleItemSnapshot requested : snapshot.items()) {
                CanonicalItemInstance live = items.get(requested.itemInstanceId());
                if (live == null
                        || !live.ownerPlayerId().equals(snapshot.playerId())
                        || live.revision() != requested.revision()
                        || !live.templateId().equals(requested.templateId())
                        || live.quantity() < requested.reservedQuantity()
                        || lockedItems.contains(requested.itemInstanceId())) {
                    return false;
                }
                if (requested.heldItem()
                        && snapshot.roster().stream().noneMatch(p -> requested.itemInstanceId().equals(p.heldItemInstanceId()))) {
                    return false;
                }
            }

            snapshot.roster().forEach(p -> lockedPokemon.add(p.pokemonId()));
            snapshot.items().forEach(i -> lockedItems.add(i.itemInstanceId()));
            snapshots.put(snapshot.reservationId(), snapshot);
            return true;
        }

        @Override
        public synchronized boolean releaseSnapshot(String reservationId, String playerId) {
            BattleAuthoritySnapshot snapshot = snapshots.get(reservationId);
            if (snapshot == null || !snapshot.playerId().equals(playerId)) {
                return false;
            }
            snapshot.roster().forEach(p -> lockedPokemon.remove(p.pokemonId()));
            snapshot.items().forEach(i -> lockedItems.remove(i.itemInstanceId()));
            snapshots.remove(reservationId);
            return true;
        }
    }
}

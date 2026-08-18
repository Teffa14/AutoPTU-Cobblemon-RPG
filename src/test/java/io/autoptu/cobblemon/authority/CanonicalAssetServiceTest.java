package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class CanonicalAssetServiceTest {
    @Test
    void exposesOnlyPokemonOwnedByRequestingPlayer() {
        InMemoryRepository repository = new InMemoryRepository();
        repository.putPokemon(new CanonicalPokemonState(
                "pokemon-1", "player-1", "cobblemon:charizard", 42, Set.of("Sky", "Power 5"), 7));
        CanonicalAssetService service = service(repository);

        assertTrue(service.findOwnedPokemon("player-1", "pokemon-1").isPresent());
        assertTrue(service.findOwnedPokemon("player-2", "pokemon-1").isEmpty());
    }

    @Test
    void reservationCopiesServerCanonicalTemplateAndRevision() {
        InMemoryRepository repository = repositoryWithItem(3, 11);
        CanonicalAssetService service = service(repository);

        ReservationDecision decision = service.reserveItem("player-1", "item-1", 2);

        assertTrue(decision.allowed());
        assertEquals("autoptu:hyper_potion", decision.reservation().itemTemplateId());
        assertEquals(11, decision.reservation().itemRevision());
        assertEquals(2, decision.reservation().quantity());
    }

    @Test
    void reservationRejectsOwnershipAndQuantityForgery() {
        InMemoryRepository repository = repositoryWithItem(3, 4);
        CanonicalAssetService service = service(repository);

        assertEquals("item_not_owned", service.reserveItem("player-2", "item-1", 1).reason());
        assertEquals("insufficient_quantity", service.reserveItem("player-1", "item-1", 99).reason());
    }

    @Test
    void oneReservationLocksTheCanonicalItemInstanceUntilReleased() {
        InMemoryRepository repository = repositoryWithItem(3, 4);
        CanonicalAssetService service = service(repository);

        ReservationDecision first = service.reserveItem("player-1", "item-1", 1);
        ReservationDecision second = service.reserveItem("player-1", "item-1", 1);

        assertTrue(first.allowed());
        assertFalse(second.allowed());
        assertEquals("state_changed_or_already_reserved", second.reason());

        assertTrue(service.releaseReservation("player-1", first.reservation().reservationId()).allowed());
        assertTrue(service.reserveItem("player-1", "item-1", 1).allowed());
    }

    @Test
    void commitConsumesReservedQuantityAndAdvancesRevisionExactlyOnce() {
        InMemoryRepository repository = repositoryWithItem(3, 11);
        CanonicalAssetService service = service(repository);
        ReservationDecision reserved = service.reserveItem("player-1", "item-1", 2);

        ReservationDecision committed = service.commitReservation(
                "player-1", reserved.reservation().reservationId());

        assertTrue(committed.allowed());
        CanonicalItemInstance item = repository.findItem("item-1").orElseThrow();
        assertEquals(1, item.quantity());
        assertEquals(12, item.revision());
        assertEquals("unknown_reservation", service.commitReservation(
                "player-1", reserved.reservation().reservationId()).reason());
    }

    @Test
    void staleReadCannotCreateReservationAfterCanonicalStateChanges() {
        InMemoryRepository repository = repositoryWithItem(3, 11);
        repository.bumpRevisionBeforeNextReserve = true;
        CanonicalAssetService service = service(repository);

        ReservationDecision decision = service.reserveItem("player-1", "item-1", 1);

        assertFalse(decision.allowed());
        assertEquals("state_changed_or_already_reserved", decision.reason());
        assertEquals(12, repository.findItem("item-1").orElseThrow().revision());
    }

    @Test
    void anotherPlayerCannotCommitSomeoneElsesReservation() {
        InMemoryRepository repository = repositoryWithItem(2, 5);
        CanonicalAssetService service = service(repository);
        ReservationDecision reserved = service.reserveItem("player-1", "item-1", 1);

        ReservationDecision forgedCommit = service.commitReservation(
                "player-2", reserved.reservation().reservationId());

        assertFalse(forgedCommit.allowed());
        assertEquals("reservation_not_owned", forgedCommit.reason());
        assertEquals(2, repository.findItem("item-1").orElseThrow().quantity());
    }

    private static CanonicalAssetService service(InMemoryRepository repository) {
        AtomicInteger sequence = new AtomicInteger();
        return new CanonicalAssetService(repository, () -> "reservation-" + sequence.incrementAndGet());
    }

    private static InMemoryRepository repositoryWithItem(int quantity, long revision) {
        InMemoryRepository repository = new InMemoryRepository();
        repository.putItem(new CanonicalItemInstance(
                "item-1", "player-1", "autoptu:hyper_potion", quantity, revision));
        return repository;
    }

    private static final class InMemoryRepository implements CanonicalAssetRepository {
        private final Map<String, CanonicalPokemonState> pokemon = new HashMap<>();
        private final Map<String, CanonicalItemInstance> items = new HashMap<>();
        private final Map<String, ItemReservation> reservations = new HashMap<>();
        private boolean bumpRevisionBeforeNextReserve;

        void putPokemon(CanonicalPokemonState state) {
            pokemon.put(state.pokemonId(), state);
        }

        void putItem(CanonicalItemInstance state) {
            items.put(state.itemInstanceId(), state);
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
            return Optional.ofNullable(reservations.get(reservationId));
        }

        @Override
        public synchronized boolean tryReserveItem(ItemReservation reservation) {
            CanonicalItemInstance item = items.get(reservation.itemInstanceId());
            if (item == null) {
                return false;
            }
            if (bumpRevisionBeforeNextReserve) {
                bumpRevisionBeforeNextReserve = false;
                item = new CanonicalItemInstance(
                        item.itemInstanceId(), item.ownerPlayerId(), item.templateId(), item.quantity(), item.revision() + 1);
                items.put(item.itemInstanceId(), item);
            }
            boolean itemAlreadyReserved = reservations.values().stream()
                    .anyMatch(existing -> existing.itemInstanceId().equals(reservation.itemInstanceId()));
            if (itemAlreadyReserved
                    || item.revision() != reservation.itemRevision()
                    || !item.ownerPlayerId().equals(reservation.playerId())
                    || !item.templateId().equals(reservation.itemTemplateId())
                    || item.quantity() < reservation.quantity()
                    || reservations.containsKey(reservation.reservationId())) {
                return false;
            }
            reservations.put(reservation.reservationId(), reservation);
            return true;
        }

        @Override
        public synchronized boolean commitItemReservation(String reservationId, String playerId) {
            ItemReservation reservation = reservations.get(reservationId);
            if (reservation == null || !reservation.playerId().equals(playerId)) {
                return false;
            }
            CanonicalItemInstance item = items.get(reservation.itemInstanceId());
            if (item == null
                    || item.revision() != reservation.itemRevision()
                    || item.quantity() < reservation.quantity()) {
                return false;
            }
            items.put(item.itemInstanceId(), new CanonicalItemInstance(
                    item.itemInstanceId(),
                    item.ownerPlayerId(),
                    item.templateId(),
                    item.quantity() - reservation.quantity(),
                    item.revision() + 1));
            reservations.remove(reservationId);
            return true;
        }

        @Override
        public synchronized boolean releaseItemReservation(String reservationId, String playerId) {
            ItemReservation reservation = reservations.get(reservationId);
            if (reservation == null || !reservation.playerId().equals(playerId)) {
                return false;
            }
            reservations.remove(reservationId);
            return true;
        }
    }
}

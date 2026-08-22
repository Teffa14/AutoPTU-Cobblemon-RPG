package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class FileCanonicalItemReservationRepositoryTest {
    @TempDir
    Path tempDirectory;

    @Test
    void itemAndReservationSurviveRepositoryRestartAndCommitAtomically() {
        FileCanonicalItemReservationRepository first = new FileCanonicalItemReservationRepository(tempDirectory);
        CanonicalItemInstance initial = new CanonicalItemInstance("item-1", "player-1", "potion", 4, 7);
        assertTrue(first.createItemIfAbsent(initial));

        ItemReservation reservation = new ItemReservation("reservation-1", "player-1", "item-1", "potion", 2, 7);
        assertTrue(first.tryReserveItem(reservation));

        FileCanonicalItemReservationRepository restarted = new FileCanonicalItemReservationRepository(tempDirectory);
        assertEquals(initial, restarted.findItem("item-1").orElseThrow());
        assertEquals(reservation, restarted.findReservation("reservation-1").orElseThrow());

        assertTrue(restarted.commitItemReservation("reservation-1", "player-1"));
        assertTrue(restarted.findReservation("reservation-1").isEmpty());
        assertEquals(new CanonicalItemInstance("item-1", "player-1", "potion", 2, 8),
                restarted.findItem("item-1").orElseThrow());
    }

    @Test
    void releaseAfterRestartPreservesQuantityAndRevision() {
        FileCanonicalItemReservationRepository first = new FileCanonicalItemReservationRepository(tempDirectory);
        CanonicalItemInstance initial = new CanonicalItemInstance("item-2", "player-2", "revive", 3, 11);
        assertTrue(first.createItemIfAbsent(initial));
        assertTrue(first.tryReserveItem(new ItemReservation(
                "reservation-2", "player-2", "item-2", "revive", 1, 11)));

        FileCanonicalItemReservationRepository restarted = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(restarted.releaseItemReservation("reservation-2", "player-2"));
        assertEquals(initial, restarted.findItem("item-2").orElseThrow());
        assertTrue(restarted.findReservation("reservation-2").isEmpty());
    }

    @Test
    void reservedItemRejectsIndependentRevisionMutationUntilReservationFinishes() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("item-3", "player-3", "ball", 5, 2)));
        assertTrue(repository.tryReserveItem(new ItemReservation(
                "reservation-3", "player-3", "item-3", "ball", 1, 2)));

        assertFalse(repository.replaceItemIfRevision(
                "item-3", 2, new CanonicalItemInstance("item-3", "player-3", "ball", 6, 3)));
        assertTrue(repository.releaseItemReservation("reservation-3", "player-3"));
        assertTrue(repository.replaceItemIfRevision(
                "item-3", 2, new CanonicalItemInstance("item-3", "player-3", "ball", 6, 3)));
    }

    @Test
    void staleReservationCannotReserveNewerItemRevision() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("item-4", "player-4", "berry", 2, 4)));
        assertTrue(repository.replaceItemIfRevision(
                "item-4", 4, new CanonicalItemInstance("item-4", "player-4", "berry", 2, 5)));

        assertFalse(repository.tryReserveItem(new ItemReservation(
                "reservation-4", "player-4", "item-4", "berry", 1, 4)));
        assertTrue(repository.findReservation("reservation-4").isEmpty());
    }

    @Test
    void independentRepositoryInstancesAllowOnlyOneReservationForAnItemRevision() throws Exception {
        FileCanonicalItemReservationRepository first = new FileCanonicalItemReservationRepository(tempDirectory);
        FileCanonicalItemReservationRepository second = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(first.createItemIfAbsent(new CanonicalItemInstance("item-5", "player-5", "potion", 8, 20)));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        Runnable firstAttempt = () -> reserve(first, "reservation-a", ready, start, winners);
        Runnable secondAttempt = () -> reserve(second, "reservation-b", ready, start, winners);
        Thread a = new Thread(firstAttempt);
        Thread b = new Thread(secondAttempt);
        a.start();
        b.start();
        ready.await();
        start.countDown();
        a.join();
        b.join();

        assertEquals(1, winners.get());
        int persisted = (first.findReservation("reservation-a").isPresent() ? 1 : 0)
                + (first.findReservation("reservation-b").isPresent() ? 1 : 0);
        assertEquals(1, persisted);
    }

    @Test
    void pokemonLookupRemainsExplicitReadOnlyCompositionBoundary() {
        CanonicalPokemonState pokemon = new CanonicalPokemonState(
                "pokemon-1", "player-1", "pikachu", 10, java.util.Set.of("battle"), 3);
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(
                tempDirectory,
                id -> id.equals("pokemon-1") ? Optional.of(pokemon) : Optional.empty());

        assertEquals(pokemon, repository.findPokemon("pokemon-1").orElseThrow());
        assertTrue(repository.findPokemon("unknown").isEmpty());
    }

    private static void reserve(
            FileCanonicalItemReservationRepository repository,
            String reservationId,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger winners
    ) {
        try {
            ready.countDown();
            start.await();
            if (repository.tryReserveItem(new ItemReservation(
                    reservationId, "player-5", "item-5", "potion", 1, 20))) {
                winners.incrementAndGet();
            }
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(error);
        }
    }
}

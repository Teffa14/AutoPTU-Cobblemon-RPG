package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CanonicalBagQueryServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void reportsOnlyOwnedStacksAndSubtractsActiveReservationsFromAvailability() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("stack-a", "player-1", "field_ration", 5, 0)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("stack-b", "player-1", "repair_kit", 2, 0)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("other", "player-2", "field_ration", 99, 0)));
        assertTrue(repository.tryReserveItem(new ItemReservation(
                "reservation-a", "player-1", "stack-a", "field_ration", 2, 0)));

        CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(repository).inspect("player-1");

        assertEquals(2, bag.entries().size());
        assertEquals(7, bag.totalQuantity());
        assertEquals(5, bag.totalAvailable());
        assertEquals(2, bag.totalReserved());
        assertEquals(1, bag.transactionLocks());
        CanonicalBagQueryService.BagEntry ration = bag.entries().stream()
                .filter(entry -> entry.itemInstanceId().equals("stack-a"))
                .findFirst().orElseThrow();
        assertEquals(3, ration.availableQuantity());
        assertEquals(2, ration.reservedQuantity());
    }

    @Test
    void consumedReservationLockDoesNotSubtractAlreadyConsumedQuantityTwice() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("stack-a", "player-1", "field_ration", 2, 7)));
        assertTrue(repository.tryReserveItem(new ItemReservation(
                "reservation-a", "player-1", "stack-a", "field_ration", 2, 7)));
        assertTrue(repository.consumeReservationRetainingLock("reservation-a", "player-1"));

        CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(repository).inspect("player-1");

        assertEquals(1, bag.entries().size());
        CanonicalBagQueryService.BagEntry entry = bag.entries().get(0);
        assertEquals(0, entry.quantity());
        assertEquals(0, entry.availableQuantity());
        assertEquals(0, entry.reservedQuantity());
        assertTrue(entry.reservationConsumed());
        assertTrue(entry.transactionLocked());
        assertEquals(1, bag.transactionLocks());
    }
}

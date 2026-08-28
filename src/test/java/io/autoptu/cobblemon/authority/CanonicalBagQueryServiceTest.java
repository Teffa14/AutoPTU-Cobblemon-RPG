package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
                "reservation-a", "player-1", "stack-a", "field_ration", 1, 7)));
        assertTrue(repository.consumeReservationRetainingLock("reservation-a", "player-1"));

        CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService(repository).inspect("player-1");

        assertEquals(1, bag.entries().size());
        CanonicalBagQueryService.BagEntry entry = bag.entries().get(0);
        assertEquals(1, entry.quantity());
        assertEquals(1, entry.availableQuantity());
        assertEquals(0, entry.reservedQuantity());
        assertTrue(entry.reservationConsumed());
        assertTrue(entry.transactionLocked());
        assertEquals(1, bag.transactionLocks());
    }

    @Test
    void inspectItemAggregatesOwnedTemplateStacksAndPreservesReservationDetail() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("ration-a", "player-1", "field_ration", 5, 2)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("ration-b", "player-1", "field_ration", 3, 4)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("foreign", "player-2", "field_ration", 50, 0)));
        assertTrue(repository.tryReserveItem(new ItemReservation(
                "reservation-a", "player-1", "ration-a", "field_ration", 2, 2)));

        CanonicalBagQueryService.ItemInspection inspection =
                new CanonicalBagQueryService(repository).inspectItem("player-1", "field_ration");

        assertTrue(inspection.found());
        assertFalse(inspection.exactInstanceMatch());
        assertEquals(2, inspection.entries().size());
        assertEquals(8, inspection.totalQuantity());
        assertEquals(6, inspection.totalAvailable());
        assertEquals(2, inspection.totalReserved());
        assertEquals("reservation-a", inspection.entries().stream()
                .filter(entry -> entry.itemInstanceId().equals("ration-a"))
                .findFirst().orElseThrow().reservationId());
    }

    @Test
    void inspectItemPrefersExactOwnedInstanceAndNeverLeaksForeignStacks() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("shared-key", "player-1", "repair_kit", 2, 3)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("other-owned", "player-1", "shared-key", 7, 0)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance("foreign-secret", "player-2", "field_ration", 99, 0)));

        CanonicalBagQueryService service = new CanonicalBagQueryService(repository);
        CanonicalBagQueryService.ItemInspection exact = service.inspectItem("player-1", "shared-key");
        CanonicalBagQueryService.ItemInspection foreign = service.inspectItem("player-1", "foreign-secret");

        assertTrue(exact.exactInstanceMatch());
        assertEquals(1, exact.entries().size());
        assertEquals("repair_kit", exact.entries().get(0).templateId());
        assertFalse(foreign.found());
        assertEquals(0, foreign.entries().size());
    }
}

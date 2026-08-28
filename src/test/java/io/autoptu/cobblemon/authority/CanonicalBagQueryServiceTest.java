package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalBagQueryServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void readsOnlyOwnedAvailableAuthoredCanonicalStacksInStableOrder() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDirectory);
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance(
                "wheat-a", "player-1", "minecraft:wheat", 2, 0)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance(
                "wheat-b", "player-1", "minecraft:wheat", 3, 0)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance(
                "ration-a", "player-1", "ouros:field_ration", 1, 0)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance(
                "other-owner", "player-2", "minecraft:wheat", 99, 0)));
        assertTrue(repository.createItemIfAbsent(new CanonicalItemInstance(
                "unknown-template", "player-1", "ouros:not_authored_here", 7, 0)));

        CanonicalBagQueryService service = new CanonicalBagQueryService(repository, new WorldTaskCatalogue());
        CanonicalBagQueryService.BagSnapshot bag = service.readAvailable("player-1");

        assertEquals("player-1", bag.playerId());
        assertEquals(2, bag.entries().size());
        assertEquals(new CanonicalBagQueryService.Entry("minecraft:wheat", 5, 2), bag.entries().get(0));
        assertEquals(new CanonicalBagQueryService.Entry("ouros:field_ration", 1, 1), bag.entries().get(1));
    }

    @Test
    void reservedStacksAreNotPresentedAsAvailable() {
        FileCanonicalItemReservationRepository repository = new FileCanonicalItemReservationRepository(tempDirectory);
        CanonicalItemInstance wheat = new CanonicalItemInstance(
                "wheat-reserved", "player-1", "minecraft:wheat", 4, 3);
        assertTrue(repository.createItemIfAbsent(wheat));
        assertTrue(repository.tryReserveItem(new ItemReservation(
                "craft-reservation", "player-1", wheat.itemInstanceId(), wheat.templateId(), 2, wheat.revision())));

        CanonicalBagQueryService service = new CanonicalBagQueryService(repository, new WorldTaskCatalogue());
        CanonicalBagQueryService.BagSnapshot bag = service.readAvailable("player-1");

        assertTrue(bag.empty());
    }
}

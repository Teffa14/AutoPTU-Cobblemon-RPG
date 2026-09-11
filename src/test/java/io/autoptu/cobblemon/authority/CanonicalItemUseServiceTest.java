package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalItemUseServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void allowsAvailableOwnedStackForServerObservedTargetAndContext() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDir);
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "item:field-kit:1", "player:test", "field-kit", 3, 0)));
        CanonicalItemUseService service = serviceFor("player:test", items);

        CanonicalItemUseService.Decision decision = service.canUse(new CanonicalItemUseService.Request(
                "player:test",
                "item:field-kit:1",
                "self",
                "bag_inspection",
                true,
                true
        ));

        assertTrue(decision.allowed());
        assertEquals("item:field-kit:1", decision.itemInstanceId());
        assertEquals("field-kit", decision.itemTemplateId());
        assertEquals("self", decision.targetId());
        assertEquals("bag_inspection", decision.contextId());
        assertEquals(3, decision.availableQuantity());
    }

    @Test
    void rejectsMissingTrainerAndForeignStack() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDir);
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "item:foreign:1", "player:other", "field-kit", 1, 0)));

        CanonicalItemUseService missingTrainer = new CanonicalItemUseService(
                ignored -> Optional.empty(), items);
        assertEquals(
                "canonical Trainer is not provisioned",
                missingTrainer.canUse(request("player:test", "item:foreign:1")).reason()
        );

        CanonicalItemUseService foreignItem = serviceFor("player:test", items);
        assertEquals(
                "canonical item is not owned by this Trainer",
                foreignItem.canUse(request("player:test", "item:foreign:1")).reason()
        );
    }

    @Test
    void rejectsAuthoritativeTransactionLockEvenWhenQuantityRemains() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDir);
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "item:locked:1", "player:test", "field-kit", 3, 0)));
        assertTrue(items.tryReserveItem(new ItemReservation(
                "reservation:test",
                "player:test",
                "item:locked:1",
                "field-kit",
                1,
                0
        )));

        CanonicalItemUseService.Decision decision = serviceFor("player:test", items)
                .canUse(request("player:test", "item:locked:1"));

        assertFalse(decision.allowed());
        assertEquals("canonical item is locked by an authoritative transaction", decision.reason());
    }

    @Test
    void rejectsEmptyStackAndUnobservedTargetOrContext() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDir);
        assertTrue(items.createItemIfAbsent(new CanonicalItemInstance(
                "item:empty:1", "player:test", "field-kit", 0, 0)));
        CanonicalItemUseService service = serviceFor("player:test", items);

        assertEquals(
                "canonical item stack is empty",
                service.canUse(request("player:test", "item:empty:1")).reason()
        );
        assertEquals(
                "item-use target was not observed by the server",
                service.canUse(new CanonicalItemUseService.Request(
                        "player:test", "item:empty:1", "self", "bag_inspection", false, true)).reason()
        );
        assertEquals(
                "item-use context was not observed by the server",
                service.canUse(new CanonicalItemUseService.Request(
                        "player:test", "item:empty:1", "self", "bag_inspection", true, false)).reason()
        );
    }

    @Test
    void rejectsMissingItemTargetAndContextIdentities() {
        FileCanonicalItemReservationRepository items = new FileCanonicalItemReservationRepository(tempDir);
        CanonicalItemUseService service = serviceFor("player:test", items);

        assertFalse(service.canUse(new CanonicalItemUseService.Request(
                "player:test", "", "self", "bag_inspection", true, true)).allowed());
        assertFalse(service.canUse(new CanonicalItemUseService.Request(
                "player:test", "item:missing", "", "bag_inspection", true, true)).allowed());
        assertFalse(service.canUse(new CanonicalItemUseService.Request(
                "player:test", "item:missing", "self", "", true, true)).allowed());
    }

    private static CanonicalItemUseService serviceFor(
            String provisionedPlayerId,
            FileCanonicalItemReservationRepository items
    ) {
        CanonicalPlayerState player = new CanonicalPlayerState(
                provisionedPlayerId,
                Set.of(),
                Map.of(),
                Set.of(),
                0
        );
        CanonicalStateRepository players = playerId -> playerId.equals(provisionedPlayerId)
                ? Optional.of(player)
                : Optional.empty();
        return new CanonicalItemUseService(players, items);
    }

    private static CanonicalItemUseService.Request request(String playerId, String itemInstanceId) {
        return new CanonicalItemUseService.Request(
                playerId,
                itemInstanceId,
                "self",
                "bag_inspection",
                true,
                true
        );
    }
}

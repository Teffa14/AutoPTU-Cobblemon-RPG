package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

final class CanonicalStorageManagementServiceTest {
    private final CanonicalStorageManagementService service = new CanonicalStorageManagementService();

    @Test
    void allowsExactServerOwnedDepositSelection() {
        CanonicalBagQueryService.BagEntry entry = new CanonicalBagQueryService.BagEntry(
                "stack-1", "potion", 3, 3, 0, null, false, 7L);
        CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService.BagSnapshot(
                "player-1", List.of(entry), 3, 3, 0, 0);
        var decision = service.canManage(new CanonicalStorageManagementService.Request(
                "player-1", true, CanonicalStorageManagementService.Mutation.DEPOSIT,
                "stack-1", "potion", 1, 7L, 3, -1L, -1, bag, null));
        assertTrue(decision.allowed());
        assertEquals("stack-1", decision.itemInstanceId());
        assertEquals("potion", decision.templateId());
    }

    @Test
    void rejectsStaleOrForgedDepositSelection() {
        CanonicalBagQueryService.BagEntry entry = new CanonicalBagQueryService.BagEntry(
                "stack-1", "potion", 3, 2, 1, "reservation-1", false, 8L);
        CanonicalBagQueryService.BagSnapshot bag = new CanonicalBagQueryService.BagSnapshot(
                "player-1", List.of(entry), 3, 2, 1, 1);
        var stale = service.canManage(new CanonicalStorageManagementService.Request(
                "player-1", true, CanonicalStorageManagementService.Mutation.DEPOSIT,
                "stack-1", "potion", 1, 7L, 3, -1L, -1, bag, null));
        assertFalse(stale.allowed());
        var forged = service.canManage(new CanonicalStorageManagementService.Request(
                "player-1", true, CanonicalStorageManagementService.Mutation.DEPOSIT,
                "foreign-stack", "potion", 1, 8L, 2, -1L, -1, bag, null));
        assertFalse(forged.allowed());
    }

    @Test
    void allowsExactWithdrawAndRejectsStaleStorageRevision() {
        CanonicalItemStorageState storage = new CanonicalItemStorageState(
                "player-1", Map.of("potion", 4), Set.of(), 5L);
        var allowed = service.canManage(new CanonicalStorageManagementService.Request(
                "player-1", true, CanonicalStorageManagementService.Mutation.WITHDRAW,
                null, "potion", 1, -1L, -1, 5L, 4, null, storage));
        assertTrue(allowed.allowed());
        var stale = service.canManage(new CanonicalStorageManagementService.Request(
                "player-1", true, CanonicalStorageManagementService.Mutation.WITHDRAW,
                null, "potion", 1, -1L, -1, 4L, 4, null, storage));
        assertFalse(stale.allowed());
    }

    @Test
    void rejectsMissingTrainerAndForeignStorageOwner() {
        CanonicalItemStorageState storage = new CanonicalItemStorageState(
                "player-2", Map.of("potion", 2), Set.of(), 1L);
        assertFalse(service.canManage(new CanonicalStorageManagementService.Request(
                "player-1", false, CanonicalStorageManagementService.Mutation.WITHDRAW,
                null, "potion", 1, -1L, -1, 1L, 2, null, storage)).allowed());
        assertFalse(service.canManage(new CanonicalStorageManagementService.Request(
                "player-1", true, CanonicalStorageManagementService.Mutation.WITHDRAW,
                null, "potion", 1, -1L, -1, 1L, 2, null, storage)).allowed());
    }
}

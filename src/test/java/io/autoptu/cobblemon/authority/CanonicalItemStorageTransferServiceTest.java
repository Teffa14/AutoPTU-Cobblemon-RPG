package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalItemStorageTransferServiceTest {
    private static final String PLAYER = "player-1";

    @TempDir
    Path root;

    @Test
    void depositAndWithdrawKeepStoredItemsOutOfActiveBagAndRetryExactlyOnce() {
        Fixture fixture = fixture();
        fixture.bag.createItemIfAbsent(new CanonicalItemInstance("bag-ration", PLAYER, "field_ration", 5, 0L));

        CanonicalItemStorageTransferService.TransferResult deposited =
                fixture.service.deposit("deposit-1", PLAYER, "field_ration", 3);
        assertEquals(3, deposited.quantity());
        assertEquals(3, fixture.storage.findOrCreate(PLAYER).quantity("field_ration"));
        assertEquals(2, fixture.bag.findItem("bag-ration").orElseThrow().quantity());
        assertTrue(fixture.bag.findReservation("item-storage:deposit-1").isEmpty());

        assertEquals(deposited, fixture.service.deposit("deposit-1", PLAYER, "ignored-retry-key", 3));
        assertEquals(3, fixture.storage.findOrCreate(PLAYER).quantity("field_ration"));
        assertEquals(2, fixture.bag.findItem("bag-ration").orElseThrow().quantity());

        CanonicalItemStorageTransferService.TransferResult withdrawn =
                fixture.service.withdraw("withdraw-1", PLAYER, "field_ration", 2);
        assertEquals(1, fixture.storage.findOrCreate(PLAYER).quantity("field_ration"));
        CanonicalItemInstance restored = fixture.bag.findItem(withdrawn.bagItemInstanceId()).orElseThrow();
        assertEquals("field_ration", restored.templateId());
        assertEquals(2, restored.quantity());
        assertEquals(withdrawn, fixture.service.withdraw("withdraw-1", PLAYER, "field_ration", 2));
        assertEquals(1, fixture.storage.findOrCreate(PLAYER).quantity("field_ration"));
    }

    @Test
    void depositRejectsReservedOrInsufficientBagStateWithoutCreatingStorage() {
        Fixture fixture = fixture();
        fixture.bag.createItemIfAbsent(new CanonicalItemInstance("bag-locked", PLAYER, "revive_kit", 2, 0L));
        assertTrue(fixture.bag.tryReserveItem(new ItemReservation(
                "other-transaction", PLAYER, "bag-locked", "revive_kit", 1, 0L)));

        assertThrows(IllegalStateException.class,
                () -> fixture.service.deposit("deposit-locked", PLAYER, "revive_kit", 1));
        assertEquals(0, fixture.storage.findOrCreate(PLAYER).quantity("revive_kit"));
        assertEquals(2, fixture.bag.findItem("bag-locked").orElseThrow().quantity());

        fixture.bag.releaseItemReservation("other-transaction", PLAYER);
        assertThrows(IllegalStateException.class,
                () -> fixture.service.deposit("deposit-too-many", PLAYER, "revive_kit", 3));
        assertEquals(0, fixture.storage.findOrCreate(PLAYER).quantity("revive_kit"));
    }

    @Test
    void withdrawalRejectsInsufficientStorageAndCannotReadAnotherPlayersStorage() {
        Fixture fixture = fixture();
        fixture.storage.applyDeltaOnce(PLAYER, "seed-own", "basic_bandage", 2);
        fixture.storage.applyDeltaOnce("other-player", "seed-other", "basic_bandage", 9);

        assertThrows(IllegalStateException.class,
                () -> fixture.service.withdraw("withdraw-too-many", PLAYER, "basic_bandage", 3));
        assertEquals(2, fixture.storage.findOrCreate(PLAYER).quantity("basic_bandage"));
        assertEquals(9, fixture.service.inspect("other-player").quantity("basic_bandage"));
        assertEquals(2, fixture.service.inspect(PLAYER).quantity("basic_bandage"));
    }

    @Test
    void restartRecoveryFinishesDepositAfterBagWasAlreadyConsumed() {
        Fixture fixture = fixture();
        fixture.bag.createItemIfAbsent(new CanonicalItemInstance("bag-restart", PLAYER, "field_ration", 4, 0L));
        FileCanonicalItemStorageTransferRepository.TransferAttempt attempt = new FileCanonicalItemStorageTransferRepository.TransferAttempt(
                "deposit-restart", PLAYER, FileCanonicalItemStorageTransferRepository.Direction.DEPOSIT,
                "bag-restart", "field_ration", 2, FileCanonicalItemStorageTransferRepository.Stage.CREATED);
        fixture.transfers.createIfAbsent(attempt);
        assertTrue(fixture.bag.tryReserveItem(new ItemReservation(
                "item-storage:deposit-restart", PLAYER, "bag-restart", "field_ration", 2, 0L)));
        assertTrue(fixture.bag.consumeReservationRetainingLock("item-storage:deposit-restart", PLAYER));
        assertEquals(2, fixture.bag.findItem("bag-restart").orElseThrow().quantity());

        CanonicalItemStorageTransferService restarted = new CanonicalItemStorageTransferService(
                new FileCanonicalItemReservationRepository(root),
                new FileCanonicalItemStorageRepository(root),
                new FileCanonicalItemStorageTransferRepository(root));
        restarted.recoverPending();

        FileCanonicalItemReservationRepository bagAfter = new FileCanonicalItemReservationRepository(root);
        FileCanonicalItemStorageRepository storageAfter = new FileCanonicalItemStorageRepository(root);
        FileCanonicalItemStorageTransferRepository transfersAfter = new FileCanonicalItemStorageTransferRepository(root);
        assertEquals(2, bagAfter.findItem("bag-restart").orElseThrow().quantity());
        assertEquals(2, storageAfter.findOrCreate(PLAYER).quantity("field_ration"));
        assertTrue(bagAfter.findReservation("item-storage:deposit-restart").isEmpty());
        assertEquals(FileCanonicalItemStorageTransferRepository.Stage.COMMITTED,
                transfersAfter.find("deposit-restart").orElseThrow().stage());
    }

    @Test
    void restartRecoveryFinishesWithdrawalAfterStorageWasAlreadyDebited() {
        Fixture fixture = fixture();
        fixture.storage.applyDeltaOnce(PLAYER, "seed", "basic_bandage", 5);
        String transferId = "withdraw-restart";
        String bagItemId = "storage-withdraw-" + UUID.nameUUIDFromBytes(transferId.getBytes(StandardCharsets.UTF_8));
        fixture.transfers.createIfAbsent(new FileCanonicalItemStorageTransferRepository.TransferAttempt(
                transferId, PLAYER, FileCanonicalItemStorageTransferRepository.Direction.WITHDRAW,
                bagItemId, "basic_bandage", 3, FileCanonicalItemStorageTransferRepository.Stage.CREATED));
        fixture.storage.applyDeltaOnce(PLAYER, transferId, "basic_bandage", -3);

        CanonicalItemStorageTransferService restarted = new CanonicalItemStorageTransferService(
                new FileCanonicalItemReservationRepository(root),
                new FileCanonicalItemStorageRepository(root),
                new FileCanonicalItemStorageTransferRepository(root));
        restarted.recoverPending();

        FileCanonicalItemReservationRepository bagAfter = new FileCanonicalItemReservationRepository(root);
        FileCanonicalItemStorageRepository storageAfter = new FileCanonicalItemStorageRepository(root);
        assertEquals(2, storageAfter.findOrCreate(PLAYER).quantity("basic_bandage"));
        assertEquals(3, bagAfter.findItem(bagItemId).orElseThrow().quantity());
        assertFalse(new CanonicalBagQueryService(bagAfter).inspect(PLAYER).entries().isEmpty());
    }

    private Fixture fixture() {
        FileCanonicalItemReservationRepository bag = new FileCanonicalItemReservationRepository(root);
        FileCanonicalItemStorageRepository storage = new FileCanonicalItemStorageRepository(root);
        FileCanonicalItemStorageTransferRepository transfers = new FileCanonicalItemStorageTransferRepository(root);
        return new Fixture(bag, storage, transfers, new CanonicalItemStorageTransferService(bag, storage, transfers));
    }

    private record Fixture(
            FileCanonicalItemReservationRepository bag,
            FileCanonicalItemStorageRepository storage,
            FileCanonicalItemStorageTransferRepository transfers,
            CanonicalItemStorageTransferService service
    ) {}
}

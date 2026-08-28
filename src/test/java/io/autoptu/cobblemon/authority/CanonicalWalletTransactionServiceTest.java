package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CanonicalWalletTransactionServiceTest {
    private static final int WALLET_MAGIC = 0x4150574C;

    @TempDir
    Path tempDirectory;

    @Test
    void retryAndRestartApplyCreditExactlyOnce() {
        FileCanonicalWalletRepository firstRepository = new FileCanonicalWalletRepository(tempDirectory);
        CanonicalWalletTransactionService first = new CanonicalWalletTransactionService(firstRepository);

        CanonicalWalletTransactionService.TransactionResult applied =
                first.credit("quest:oak:reward:player-1", "player-1", 150L, "quest:oak:reward");
        CanonicalWalletTransactionService.TransactionResult retry =
                first.credit("quest:oak:reward:player-1", "player-1", 150L, "quest:oak:reward");

        assertEquals(CanonicalWalletTransactionService.Status.APPLIED, applied.status());
        assertEquals(150L, applied.balance());
        assertEquals(1L, applied.revision());
        assertEquals(CanonicalWalletTransactionService.Status.ALREADY_APPLIED, retry.status());
        assertEquals(150L, retry.balance());
        assertEquals(1L, retry.revision());

        FileCanonicalWalletRepository restartedRepository = new FileCanonicalWalletRepository(tempDirectory);
        CanonicalWalletTransactionService restarted = new CanonicalWalletTransactionService(restartedRepository);
        CanonicalWalletTransactionService.TransactionResult restartRetry =
                restarted.credit("quest:oak:reward:player-1", "player-1", 150L, "quest:oak:reward");

        assertEquals(CanonicalWalletTransactionService.Status.ALREADY_APPLIED, restartRetry.status());
        assertEquals(150L, restartRetry.balance());
        assertEquals(1L, restartRetry.revision());
        assertTrue(restartedRepository.findAppliedTransaction("player-1", "quest:oak:reward:player-1").isPresent());
    }

    @Test
    void debitRejectsInsufficientFundsWithoutCreatingReceipt() {
        FileCanonicalWalletRepository repository = new FileCanonicalWalletRepository(tempDirectory);
        CanonicalWalletTransactionService service = new CanonicalWalletTransactionService(repository);
        service.credit("bootstrap:fund", "player-1", 80L, "test:server-funding");

        CanonicalWalletTransactionService.TransactionResult rejected =
                service.debit("shop:purchase:1", "player-1", 100L, "shop:cedar-mart");

        assertEquals(CanonicalWalletTransactionService.Status.INSUFFICIENT_FUNDS, rejected.status());
        assertEquals(80L, rejected.balance());
        assertEquals(1L, rejected.revision());
        assertFalse(repository.findAppliedTransaction("player-1", "shop:purchase:1").isPresent());
    }

    @Test
    void successfulDebitPersistsOneRevisionAndOneReceipt() {
        FileCanonicalWalletRepository repository = new FileCanonicalWalletRepository(tempDirectory);
        CanonicalWalletTransactionService service = new CanonicalWalletTransactionService(repository);
        service.credit("bootstrap:fund", "player-1", 200L, "test:server-funding");

        CanonicalWalletTransactionService.TransactionResult debit =
                service.debit("service:healing:1", "player-1", 75L, "facility:healing-service");

        assertEquals(CanonicalWalletTransactionService.Status.APPLIED, debit.status());
        assertEquals(125L, debit.balance());
        assertEquals(2L, debit.revision());
        FileCanonicalWalletRepository.AppliedTransaction receipt =
                repository.findAppliedTransaction("player-1", "service:healing:1").orElseThrow();
        assertEquals(FileCanonicalWalletRepository.TransactionDirection.DEBIT, receipt.direction());
        assertEquals(200L, receipt.balanceBefore());
        assertEquals(125L, receipt.balanceAfter());
        assertEquals(2L, receipt.resultingRevision());
    }

    @Test
    void transactionIdentityCannotBeReusedForDifferentMutation() {
        FileCanonicalWalletRepository repository = new FileCanonicalWalletRepository(tempDirectory);
        CanonicalWalletTransactionService service = new CanonicalWalletTransactionService(repository);
        service.credit("economy:tx:1", "player-1", 20L, "source:a");

        CanonicalWalletTransactionService.TransactionResult changedAmount =
                service.credit("economy:tx:1", "player-1", 21L, "source:a");
        CanonicalWalletTransactionService.TransactionResult changedSource =
                service.credit("economy:tx:1", "player-1", 20L, "source:b");
        CanonicalWalletTransactionService.TransactionResult changedDirection =
                service.debit("economy:tx:1", "player-1", 20L, "source:a");

        assertEquals(CanonicalWalletTransactionService.Status.TRANSACTION_CONFLICT, changedAmount.status());
        assertEquals(CanonicalWalletTransactionService.Status.TRANSACTION_CONFLICT, changedSource.status());
        assertEquals(CanonicalWalletTransactionService.Status.TRANSACTION_CONFLICT, changedDirection.status());
        assertEquals(20L, repository.findOrCreate("player-1").balance());
        assertEquals(1L, repository.findOrCreate("player-1").revision());
    }

    @Test
    void schemaOneWalletReadsAndUpgradesOnFirstTransaction() throws Exception {
        String playerId = "legacy-player";
        Path walletDirectory = tempDirectory.resolve("wallets");
        Files.createDirectories(walletDirectory);
        String key = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(playerId.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        Path walletFile = walletDirectory.resolve(key + ".bin");
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(walletFile))) {
            output.writeInt(WALLET_MAGIC);
            output.writeInt(1);
            output.writeUTF(playerId);
            output.writeUTF("ouros_credit");
            output.writeLong(40L);
            output.writeLong(3L);
        }

        FileCanonicalWalletRepository repository = new FileCanonicalWalletRepository(tempDirectory);
        assertEquals(40L, repository.findOrCreate(playerId).balance());
        assertEquals(3L, repository.findOrCreate(playerId).revision());

        CanonicalWalletTransactionService.TransactionResult credited =
                new CanonicalWalletTransactionService(repository).credit("migration:credit", playerId, 10L, "migration:test");

        assertEquals(CanonicalWalletTransactionService.Status.APPLIED, credited.status());
        assertEquals(50L, credited.balance());
        assertEquals(4L, credited.revision());

        FileCanonicalWalletRepository restarted = new FileCanonicalWalletRepository(tempDirectory);
        assertEquals(50L, restarted.findOrCreate(playerId).balance());
        assertTrue(restarted.findAppliedTransaction(playerId, "migration:credit").isPresent());
    }
}

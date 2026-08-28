package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

final class CanonicalWalletQueryServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void firstReadCreatesZeroBalanceCanonicalWallet() {
        FileCanonicalWalletRepository repository = new FileCanonicalWalletRepository(tempDirectory);

        CanonicalWalletQueryService.WalletSnapshot wallet =
                new CanonicalWalletQueryService(repository).inspect("player-1");

        assertEquals("player-1", wallet.playerId());
        assertEquals("ouros_credit", wallet.currencyId());
        assertEquals(0L, wallet.balance());
        assertEquals(0L, wallet.revision());
        assertTrue(repository.find("player-1").isPresent());
        assertFalse(repository.find("player-2").isPresent());
    }

    @Test
    void walletSurvivesRepositoryRestartAndRejectsStaleRevision() {
        FileCanonicalWalletRepository first = new FileCanonicalWalletRepository(tempDirectory);
        FileCanonicalWalletRepository.WalletState initial = first.findOrCreate("player-1");
        assertTrue(first.replaceIfRevision(
                new FileCanonicalWalletRepository.WalletState("player-1", "ouros_credit", 125L, 1L),
                initial.revision()));

        FileCanonicalWalletRepository restarted = new FileCanonicalWalletRepository(tempDirectory);
        FileCanonicalWalletRepository.WalletState recovered = restarted.findOrCreate("player-1");

        assertEquals(125L, recovered.balance());
        assertEquals(1L, recovered.revision());
        assertFalse(restarted.replaceIfRevision(
                new FileCanonicalWalletRepository.WalletState("player-1", "ouros_credit", 999L, 1L),
                0L));
        assertEquals(125L, restarted.findOrCreate("player-1").balance());
    }
}

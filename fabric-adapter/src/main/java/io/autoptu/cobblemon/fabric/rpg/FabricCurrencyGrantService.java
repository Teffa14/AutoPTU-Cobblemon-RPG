package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalWalletTransactionService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Objects;
import java.util.UUID;

/**
 * Server-authoritative currency grant boundary shared by operator commands and future world/UI surfaces.
 *
 * <p>The caller may request a positive delta only. Canonical identity, wallet state, revision handling,
 * transaction persistence, and resulting balance remain server-owned.</p>
 */
public final class FabricCurrencyGrantService {
    public static final long MAX_GRANT = 1_000_000_000L;
    private static final String SOURCE_ID = "admin-currency-grant";

    private FabricCurrencyGrantService() {
    }

    public static GrantResult grant(MinecraftServer server, ServerPlayerEntity target, long amount) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(target, "target");
        if (amount < 1L || amount > MAX_GRANT) {
            throw new IllegalArgumentException("amount must be between 1 and " + MAX_GRANT);
        }

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(target.getUuid());
        if (FabricCanonicalPlayerStoreRuntime.requireRepository(server).findPlayer(playerId).isEmpty()) {
            return GrantResult.noTrainer(playerId);
        }

        String transactionId = "admin-grant:" + UUID.randomUUID();
        CanonicalWalletTransactionService.TransactionResult transaction = new CanonicalWalletTransactionService(
                FabricCanonicalPlayerStoreRuntime.requireWalletRepository(server))
                .credit(transactionId, playerId, amount, SOURCE_ID);

        if (!transaction.committed()) {
            return GrantResult.rejected(playerId, transactionId, transaction);
        }
        return GrantResult.committed(playerId, transactionId, transaction);
    }

    public record GrantResult(
            Status status,
            String playerId,
            String transactionId,
            String currencyId,
            long balance,
            long revision,
            CanonicalWalletTransactionService.Status transactionStatus
    ) {
        public enum Status { COMMITTED, NO_CANONICAL_TRAINER, TRANSACTION_REJECTED }

        private static GrantResult noTrainer(String playerId) {
            return new GrantResult(Status.NO_CANONICAL_TRAINER, playerId, null, null, 0L, 0L, null);
        }

        private static GrantResult committed(
                String playerId,
                String transactionId,
                CanonicalWalletTransactionService.TransactionResult transaction
        ) {
            return new GrantResult(
                    Status.COMMITTED,
                    playerId,
                    transactionId,
                    transaction.currencyId(),
                    transaction.balance(),
                    transaction.revision(),
                    transaction.status());
        }

        private static GrantResult rejected(
                String playerId,
                String transactionId,
                CanonicalWalletTransactionService.TransactionResult transaction
        ) {
            return new GrantResult(
                    Status.TRANSACTION_REJECTED,
                    playerId,
                    transactionId,
                    transaction.currencyId(),
                    transaction.balance(),
                    transaction.revision(),
                    transaction.status());
        }
    }
}

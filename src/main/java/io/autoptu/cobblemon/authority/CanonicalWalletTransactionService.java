package io.autoptu.cobblemon.authority;

import java.util.Objects;

/**
 * Server-owned currency mutation boundary.
 *
 * <p>Callers must derive transaction identity, source and amount from authenticated authored RPG
 * services. Client payloads are requests only and must never be treated as trusted balance truth.
 * This service defines no shop prices, quest rewards, PTU item effects or battle outcomes.</p>
 */
public final class CanonicalWalletTransactionService {
    private static final int MAX_STALE_RETRIES = 16;
    private final FileCanonicalWalletRepository repository;

    public CanonicalWalletTransactionService(FileCanonicalWalletRepository repository) {
        this.repository = Objects.requireNonNull(repository, "repository");
    }

    public TransactionResult credit(String transactionId, String playerId, long amount, String sourceId) {
        return commit(transactionId, playerId, FileCanonicalWalletRepository.TransactionDirection.CREDIT, amount, sourceId);
    }

    public TransactionResult debit(String transactionId, String playerId, long amount, String sourceId) {
        return commit(transactionId, playerId, FileCanonicalWalletRepository.TransactionDirection.DEBIT, amount, sourceId);
    }

    private TransactionResult commit(
            String transactionId,
            String playerId,
            FileCanonicalWalletRepository.TransactionDirection direction,
            long amount,
            String sourceId
    ) {
        for (int attempt = 0; attempt < MAX_STALE_RETRIES; attempt++) {
            FileCanonicalWalletRepository.WalletState current = repository.findOrCreate(playerId);
            FileCanonicalWalletRepository.TransactionCommitResult committed = repository.commitTransaction(
                    transactionId,
                    playerId,
                    direction,
                    amount,
                    sourceId,
                    current.revision()
            );
            if (committed.status() == FileCanonicalWalletRepository.TransactionCommitStatus.STALE_REVISION) continue;
            return project(committed);
        }
        FileCanonicalWalletRepository.WalletState current = repository.findOrCreate(playerId);
        return new TransactionResult(
                Status.RETRY_EXHAUSTED,
                current.playerId(),
                current.currencyId(),
                current.balance(),
                current.revision(),
                null
        );
    }

    private static TransactionResult project(FileCanonicalWalletRepository.TransactionCommitResult committed) {
        Status status = switch (committed.status()) {
            case APPLIED -> Status.APPLIED;
            case ALREADY_APPLIED -> Status.ALREADY_APPLIED;
            case INSUFFICIENT_FUNDS -> Status.INSUFFICIENT_FUNDS;
            case TRANSACTION_CONFLICT -> Status.TRANSACTION_CONFLICT;
            case STALE_REVISION -> throw new IllegalStateException("stale revision must be retried before projection");
        };
        FileCanonicalWalletRepository.WalletState wallet = committed.wallet();
        return new TransactionResult(
                status,
                wallet.playerId(),
                wallet.currencyId(),
                wallet.balance(),
                wallet.revision(),
                committed.transaction()
        );
    }

    public enum Status {
        APPLIED,
        ALREADY_APPLIED,
        INSUFFICIENT_FUNDS,
        TRANSACTION_CONFLICT,
        RETRY_EXHAUSTED
    }

    public record TransactionResult(
            Status status,
            String playerId,
            String currencyId,
            long balance,
            long revision,
            FileCanonicalWalletRepository.AppliedTransaction transaction
    ) {
        public TransactionResult {
            status = Objects.requireNonNull(status, "status");
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
            if (currencyId == null || currencyId.isBlank()) throw new IllegalArgumentException("currencyId must not be blank");
            if (balance < 0 || revision < 0) throw new IllegalArgumentException("wallet values must not be negative");
        }

        public boolean committed() {
            return status == Status.APPLIED || status == Status.ALREADY_APPLIED;
        }
    }
}

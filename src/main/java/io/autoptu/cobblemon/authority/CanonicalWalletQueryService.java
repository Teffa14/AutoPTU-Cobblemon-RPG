package io.autoptu.cobblemon.authority;

/** Read-only wallet projection for authenticated RPG surfaces. */
public final class CanonicalWalletQueryService {
    private final FileCanonicalWalletRepository repository;

    public CanonicalWalletQueryService(FileCanonicalWalletRepository repository) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        this.repository = repository;
    }

    public WalletSnapshot inspect(String playerId) {
        FileCanonicalWalletRepository.WalletState state = repository.findOrCreate(playerId);
        return new WalletSnapshot(state.playerId(), state.currencyId(), state.balance(), state.revision());
    }

    public record WalletSnapshot(String playerId, String currencyId, long balance, long revision) {
        public WalletSnapshot {
            if (playerId == null || playerId.isBlank()) throw new IllegalArgumentException("playerId must not be blank");
            if (currencyId == null || currencyId.isBlank()) throw new IllegalArgumentException("currencyId must not be blank");
            if (balance < 0 || revision < 0) throw new IllegalArgumentException("wallet values must not be negative");
        }
    }
}

package io.autoptu.cobblemon.authority;

/** Durable server-owned mutable stock boundary for authored shop offers. */
public interface CanonicalShopStockRepository {
    StockState getOrCreate(String shopId, String offerId, int authoredStockLimit);

    boolean replaceIfRevision(String shopId, String offerId, long expectedRevision, int remainingStock);

    record StockState(String shopId, String offerId, int remainingStock, long revision) {
        public StockState {
            if (shopId == null || shopId.isBlank()) throw new IllegalArgumentException("shopId is required");
            if (offerId == null || offerId.isBlank()) throw new IllegalArgumentException("offerId is required");
            if (remainingStock < 0) throw new IllegalArgumentException("remainingStock cannot be negative");
            if (revision < 0) throw new IllegalArgumentException("revision cannot be negative");
        }
    }
}

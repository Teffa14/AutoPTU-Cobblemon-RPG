package io.autoptu.cobblemon.authority;

/** Durable server-owned mutable stock boundary for authored shop offers. */
public interface CanonicalShopStockRepository {
    StockState getOrCreate(String shopId, String offerId, int authoredStockLimit);

    boolean replaceIfRevision(String shopId, String offerId, long expectedRevision, int remainingStock);

    DepletionResult deplete(
            String transactionId,
            String shopId,
            String offerId,
            int quantity,
            int authoredStockLimit,
            long expectedRevision
    );

    record StockState(String shopId, String offerId, int remainingStock, long revision) {
        public StockState {
            if (shopId == null || shopId.isBlank()) throw new IllegalArgumentException("shopId is required");
            if (offerId == null || offerId.isBlank()) throw new IllegalArgumentException("offerId is required");
            if (remainingStock < 0) throw new IllegalArgumentException("remainingStock cannot be negative");
            if (revision < 0) throw new IllegalArgumentException("revision cannot be negative");
        }
    }

    enum DepletionStatus {
        APPLIED,
        ALREADY_APPLIED,
        OUT_OF_STOCK,
        STALE_REVISION,
        TRANSACTION_CONFLICT
    }

    record AppliedDepletion(
            String transactionId,
            int quantity,
            int stockBefore,
            int stockAfter,
            long resultingRevision
    ) {
        public AppliedDepletion {
            if (transactionId == null || transactionId.isBlank()) throw new IllegalArgumentException("transactionId is required");
            if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
            if (stockBefore < 0 || stockAfter < 0 || stockAfter > stockBefore) {
                throw new IllegalArgumentException("invalid stock transition");
            }
            if (stockBefore - stockAfter != quantity) throw new IllegalArgumentException("quantity must match stock transition");
            if (resultingRevision <= 0) throw new IllegalArgumentException("resultingRevision must be positive");
            transactionId = transactionId.trim();
        }
    }

    record DepletionResult(DepletionStatus status, StockState stock, AppliedDepletion depletion) {
        public DepletionResult {
            if (status == null) throw new IllegalArgumentException("status is required");
            if (stock == null) throw new IllegalArgumentException("stock is required");
        }

        public boolean committed() {
            return status == DepletionStatus.APPLIED || status == DepletionStatus.ALREADY_APPLIED;
        }
    }
}

package io.autoptu.cobblemon.authority;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Applies durable server-authored shop restocks against the monotonic RPG day. */
public final class CanonicalShopRestockService {
    private static final int MAX_STALE_RETRIES = 16;

    private final CanonicalShopCatalogue catalogue;
    private final CanonicalShopStockRepository stock;

    public CanonicalShopRestockService(CanonicalShopCatalogue catalogue, CanonicalShopStockRepository stock) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.stock = Objects.requireNonNull(stock, "stock");
    }

    public List<CanonicalShopStockRepository.RestockResult> reconcileShop(String shopId, long rpgDayId) {
        if (rpgDayId < 0) throw new IllegalArgumentException("rpgDayId cannot be negative");
        List<CanonicalShopOffer> offers = catalogue.offers(shopId);
        List<CanonicalShopStockRepository.RestockResult> results = new ArrayList<>(offers.size());
        for (CanonicalShopOffer offer : offers) {
            results.add(reconcileOffer(shopId, offer, rpgDayId));
        }
        return List.copyOf(results);
    }

    private CanonicalShopStockRepository.RestockResult reconcileOffer(
            String shopId,
            CanonicalShopOffer offer,
            long rpgDayId
    ) {
        String transactionId = "rpg-day:" + rpgDayId;
        for (int retry = 0; retry < MAX_STALE_RETRIES; retry++) {
            CanonicalShopStockRepository.StockState current = stock.getOrCreate(
                    shopId, offer.offerId(), offer.stockLimit());
            CanonicalShopStockRepository.RestockResult result = stock.restockToAuthoredLimit(
                    transactionId,
                    shopId,
                    offer.offerId(),
                    offer.stockLimit(),
                    current.revision());
            if (result.status() == CanonicalShopStockRepository.RestockStatus.STALE_REVISION) continue;
            if (result.status() == CanonicalShopStockRepository.RestockStatus.TRANSACTION_CONFLICT) {
                throw new IllegalStateException("shop restock transaction identity conflict");
            }
            return result;
        }
        throw new IllegalStateException("shop restock retry exhausted");
    }
}

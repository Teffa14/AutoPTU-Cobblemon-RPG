package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;

/** Reusable read authority shared by command/UI/world shop surfaces. */
public final class CanonicalShopQueryService {
    private final CanonicalShopCatalogue catalogue;
    private final CanonicalShopStockRepository stockRepository;

    public CanonicalShopQueryService(
            CanonicalShopCatalogue catalogue,
            CanonicalShopStockRepository stockRepository
    ) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
        this.stockRepository = Objects.requireNonNull(stockRepository, "stockRepository");
    }

    public ShopSnapshot inspectShop(String playerId, String shopId) {
        String owner = requireText(playerId, "playerId");
        String canonicalShopId = requireText(shopId, "shopId");
        List<OfferSnapshot> offers = catalogue.offers(canonicalShopId).stream()
                .map(offer -> {
                    CanonicalShopStockRepository.StockState stock = stockRepository.getOrCreate(
                            canonicalShopId, offer.offerId(), offer.stockLimit());
                    return new OfferSnapshot(offer, stock.remainingStock(), stock.revision());
                })
                .toList();
        return new ShopSnapshot(owner, canonicalShopId, offers);
    }

    public record OfferSnapshot(CanonicalShopOffer offer, int remainingStock, long stockRevision) {
        public OfferSnapshot {
            offer = Objects.requireNonNull(offer, "offer");
            if (remainingStock < 0 || remainingStock > offer.stockLimit()) {
                throw new IllegalArgumentException("remainingStock must stay inside authored stock limit");
            }
            if (stockRevision < 0) throw new IllegalArgumentException("stockRevision cannot be negative");
        }
    }

    public record ShopSnapshot(String playerId, String shopId, List<OfferSnapshot> offers) {
        public ShopSnapshot {
            playerId = requireText(playerId, "playerId");
            shopId = requireText(shopId, "shopId");
            offers = List.copyOf(Objects.requireNonNull(offers, "offers"));
        }
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}

package io.autoptu.cobblemon.authority;

import java.util.List;
import java.util.Objects;

/** Reusable read authority shared by command/UI/world shop surfaces. */
public final class CanonicalShopQueryService {
    private final CanonicalShopCatalogue catalogue;

    public CanonicalShopQueryService(CanonicalShopCatalogue catalogue) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue");
    }

    public ShopSnapshot inspectShop(String playerId, String shopId) {
        String owner = requireText(playerId, "playerId");
        String canonicalShopId = requireText(shopId, "shopId");
        return new ShopSnapshot(owner, canonicalShopId, catalogue.offers(canonicalShopId));
    }

    public record ShopSnapshot(String playerId, String shopId, List<CanonicalShopOffer> offers) {
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

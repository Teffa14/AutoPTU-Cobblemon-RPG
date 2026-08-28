package io.autoptu.cobblemon.authority;

import java.util.Objects;

/** Immutable server-authored shop offer metadata. Purchase legality is handled elsewhere. */
public record CanonicalShopOffer(
        String offerId,
        String itemTemplateId,
        String currencyId,
        long unitPrice,
        int stockLimit
) {
    public CanonicalShopOffer {
        offerId = requireText(offerId, "offerId");
        itemTemplateId = requireText(itemTemplateId, "itemTemplateId");
        currencyId = requireText(currencyId, "currencyId");
        if (unitPrice <= 0) throw new IllegalArgumentException("unitPrice must be positive");
        if (stockLimit <= 0) throw new IllegalArgumentException("stockLimit must be positive");
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}

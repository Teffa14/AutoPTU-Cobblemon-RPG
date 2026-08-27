package io.autoptu.cobblemon.authority;

/** Server-authored shop catalogue entry. Purchase authority is intentionally handled elsewhere. */
public record CanonicalShopOffer(
        String offerId,
        String displayName,
        String itemTemplateId,
        String currencyId,
        int unitPrice,
        int availableStock
) {
    public CanonicalShopOffer {
        if (offerId == null || offerId.isBlank()) throw new IllegalArgumentException("offerId must not be blank");
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName must not be blank");
        if (itemTemplateId == null || itemTemplateId.isBlank()) throw new IllegalArgumentException("itemTemplateId must not be blank");
        if (currencyId == null || currencyId.isBlank()) throw new IllegalArgumentException("currencyId must not be blank");
        if (unitPrice < 0) throw new IllegalArgumentException("unitPrice must be >= 0");
        if (availableStock < 0) throw new IllegalArgumentException("availableStock must be >= 0");
    }
}

package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Read-only authored catalogue. Clients may select IDs but never provide price, stock, template, or currency truth. */
public final class CanonicalShopCatalogue {
    public static final CanonicalShopCatalogue DEFAULT = new CanonicalShopCatalogue(Map.of(
            "cedar-mart", List.of(
                    new CanonicalShopOffer("field-ration", "field_ration", "ouros_credit", 80, 12),
                    new CanonicalShopOffer("basic-bandage", "basic_bandage", "ouros_credit", 120, 8),
                    new CanonicalShopOffer("revive-kit", "revive_kit", "ouros_credit", 260, 4)
            )
    ));

    private final Map<String, List<CanonicalShopOffer>> offersByShopId;

    public CanonicalShopCatalogue(Map<String, List<CanonicalShopOffer>> authored) {
        Objects.requireNonNull(authored, "authored");
        Map<String, List<CanonicalShopOffer>> normalized = new LinkedHashMap<>();
        authored.forEach((shopId, offers) -> {
            String key = requireText(shopId, "shopId");
            List<CanonicalShopOffer> copy = List.copyOf(Objects.requireNonNull(offers, "offers"));
            long distinctIds = copy.stream().map(CanonicalShopOffer::offerId).distinct().count();
            if (distinctIds != copy.size()) throw new IllegalArgumentException("duplicate offerId in " + key);
            if (normalized.put(key, copy) != null) throw new IllegalArgumentException("duplicate shopId " + key);
        });
        offersByShopId = Map.copyOf(normalized);
    }

    public List<String> shopIds() {
        return offersByShopId.keySet().stream().sorted().toList();
    }

    public List<CanonicalShopOffer> offers(String shopId) {
        return offersByShopId.getOrDefault(requireText(shopId, "shopId"), List.of());
    }

    public Optional<CanonicalShopOffer> offer(String shopId, String offerId) {
        String normalizedOfferId = requireText(offerId, "offerId");
        return offers(shopId).stream().filter(offer -> offer.offerId().equals(normalizedOfferId)).findFirst();
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }
}

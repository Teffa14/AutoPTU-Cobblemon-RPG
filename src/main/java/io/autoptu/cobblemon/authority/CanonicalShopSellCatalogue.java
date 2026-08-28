package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Server-authored shop sell policy. No client-provided or derived buyback price is trusted. */
public final class CanonicalShopSellCatalogue {
    public static final CanonicalShopSellCatalogue DEFAULT = new CanonicalShopSellCatalogue(Map.of(
            "cedar-mart", List.of(
                    new SellOffer("field_ration", "ouros_credit", 40),
                    new SellOffer("basic_bandage", "ouros_credit", 60),
                    new SellOffer("revive_kit", "ouros_credit", 130)
            )
    ));

    private final Map<String, List<SellOffer>> offersByShopId;

    public CanonicalShopSellCatalogue(Map<String, List<SellOffer>> authored) {
        Objects.requireNonNull(authored, "authored");
        Map<String, List<SellOffer>> normalized = new LinkedHashMap<>();
        authored.forEach((shopId, offers) -> {
            String key = requireText(shopId, "shopId");
            List<SellOffer> copy = List.copyOf(Objects.requireNonNull(offers, "offers"));
            long distinctTemplates = copy.stream().map(SellOffer::itemTemplateId).distinct().count();
            if (distinctTemplates != copy.size()) throw new IllegalArgumentException("duplicate sell template in " + key);
            if (normalized.put(key, copy) != null) throw new IllegalArgumentException("duplicate shopId " + key);
        });
        offersByShopId = Map.copyOf(normalized);
    }

    public Optional<SellOffer> offer(String shopId, String itemTemplateId) {
        String template = requireText(itemTemplateId, "itemTemplateId");
        return offersByShopId.getOrDefault(requireText(shopId, "shopId"), List.of()).stream()
                .filter(offer -> offer.itemTemplateId().equals(template))
                .findFirst();
    }

    private static String requireText(String value, String field) {
        String normalized = Objects.requireNonNull(value, field).trim();
        if (normalized.isEmpty()) throw new IllegalArgumentException(field + " must not be blank");
        return normalized;
    }

    public record SellOffer(String itemTemplateId, String currencyId, long unitPrice) {
        public SellOffer {
            itemTemplateId = requireText(itemTemplateId, "itemTemplateId");
            currencyId = requireText(currencyId, "currencyId");
            if (unitPrice <= 0) throw new IllegalArgumentException("unitPrice must be positive");
        }
    }
}

package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Server-owned RPG shop catalogue. Clients may display these offers but cannot provide trusted
 * stock, prices, item templates, or purchase eligibility.
 */
public final class CanonicalShopCatalogue {
    private final List<CanonicalShopOffer> offers;
    private final Map<String, CanonicalShopOffer> byId;

    public CanonicalShopCatalogue() {
        this(List.of(
                new CanonicalShopOffer("basic-potion", "Potion", "autoptu:potion", "autoptu:credits", 200, 20),
                new CanonicalShopOffer("antidote", "Antidote", "autoptu:antidote", "autoptu:credits", 100, 12),
                new CanonicalShopOffer("pokeball", "Poke Ball", "autoptu:poke_ball", "autoptu:credits", 250, 15)
        ));
    }

    public CanonicalShopCatalogue(List<CanonicalShopOffer> offers) {
        if (offers == null) throw new IllegalArgumentException("offers are required");
        LinkedHashMap<String, CanonicalShopOffer> indexed = new LinkedHashMap<>();
        for (CanonicalShopOffer offer : offers) {
            if (offer == null) throw new IllegalArgumentException("shop offer must not be null");
            if (indexed.putIfAbsent(offer.offerId(), offer) != null) {
                throw new IllegalArgumentException("duplicate shop offerId: " + offer.offerId());
            }
        }
        this.offers = List.copyOf(indexed.values());
        this.byId = Map.copyOf(indexed);
    }

    public List<CanonicalShopOffer> offers() {
        return offers;
    }

    public Optional<CanonicalShopOffer> findOffer(String offerId) {
        if (offerId == null || offerId.isBlank()) return Optional.empty();
        return Optional.ofNullable(byId.get(offerId));
    }
}

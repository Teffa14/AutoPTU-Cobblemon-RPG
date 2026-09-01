package io.autoptu.cobblemon.authority;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Canonical identity for named NPC partner Pokemon.
 *
 * This catalogue records narrative identity only. It does not author PTU battle sheets, HP, moves,
 * abilities, status, capture legality, ownership transfers, or battle outcomes.
 */
public final class CanonicalNpcPokemonCatalogue {
    public static final CanonicalNpcPokemonCatalogue DEFAULT = fromNpcCatalogue(CanonicalNpcCatalogue.DEFAULT);

    private final Map<String, PartnerPokemon> partners;
    private final Map<String, PartnerPokemon> byNpc;

    public CanonicalNpcPokemonCatalogue(List<PartnerPokemon> partners) {
        Objects.requireNonNull(partners, "partners");
        LinkedHashMap<String, PartnerPokemon> indexed = new LinkedHashMap<>();
        LinkedHashMap<String, PartnerPokemon> npcIndexed = new LinkedHashMap<>();
        for (PartnerPokemon partner : partners) {
            Objects.requireNonNull(partner, "partner");
            if (indexed.putIfAbsent(partner.partnerId(), partner) != null) {
                throw new IllegalArgumentException("duplicate partnerId: " + partner.partnerId());
            }
            if (npcIndexed.putIfAbsent(partner.npcId(), partner) != null) {
                throw new IllegalArgumentException("NPC has multiple primary named partners: " + partner.npcId());
            }
        }
        this.partners = Map.copyOf(indexed);
        this.byNpc = Map.copyOf(npcIndexed);
    }

    public Optional<PartnerPokemon> partner(String partnerId) {
        if (partnerId == null || partnerId.isBlank()) return Optional.empty();
        return Optional.ofNullable(partners.get(partnerId.strip()));
    }

    public Optional<PartnerPokemon> partnerForNpc(String npcId) {
        if (npcId == null || npcId.isBlank()) return Optional.empty();
        return Optional.ofNullable(byNpc.get(npcId.strip()));
    }

    public List<PartnerPokemon> partners() {
        return List.copyOf(partners.values());
    }

    private static CanonicalNpcPokemonCatalogue fromNpcCatalogue(CanonicalNpcCatalogue catalogue) {
        return new CanonicalNpcPokemonCatalogue(catalogue.npcs().stream()
                .map(npc -> new PartnerPokemon(
                        npc.npcId() + ".partner",
                        npc.npcId(),
                        npc.companionSpeciesId(),
                        npc.companionName()
                ))
                .toList());
    }

    public record PartnerPokemon(String partnerId, String npcId, String speciesId, String displayName) {
        public PartnerPokemon {
            partnerId = requireText(partnerId, "partnerId");
            npcId = requireText(npcId, "npcId");
            speciesId = requireText(speciesId, "speciesId");
            displayName = requireText(displayName, "displayName");
        }
    }

    private static String requireText(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}

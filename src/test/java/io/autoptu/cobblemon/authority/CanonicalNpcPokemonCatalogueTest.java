package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalNpcPokemonCatalogueTest {
    @Test
    void everyCanonicalNpcHasExactlyOneNamedPartnerIdentity() {
        var npcs = CanonicalNpcCatalogue.DEFAULT.npcs();
        var partners = CanonicalNpcPokemonCatalogue.DEFAULT.partners();

        assertEquals(npcs.size(), partners.size());
        for (var npc : npcs) {
            var partner = CanonicalNpcPokemonCatalogue.DEFAULT.partnerForNpc(npc.npcId());
            assertTrue(partner.isPresent(), () -> "missing partner identity for " + npc.npcId());
            assertEquals(npc.companionSpeciesId(), partner.orElseThrow().speciesId());
            assertEquals(npc.companionName(), partner.orElseThrow().displayName());
            assertTrue(partner.orElseThrow().partnerId().startsWith(npc.npcId() + "."));
        }
    }
}

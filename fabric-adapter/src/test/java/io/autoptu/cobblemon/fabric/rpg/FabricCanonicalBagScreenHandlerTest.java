package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalItemUseService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FabricCanonicalBagScreenHandlerTest {
    @Test
    void formatsServerAuthoredReadyStateWithoutInventingItemEffects() {
        var decision = CanonicalItemUseService.Decision.allowed(
                "item-1", "field_ration", "player-1", "bag_inspection", 3);

        assertEquals(" | use ready", FabricCanonicalBagScreenHandler.readinessLabel(decision));
    }

    @Test
    void formatsExactServerBlockingReason() {
        var decision = CanonicalItemUseService.Decision.denied(
                "canonical item is locked by an authoritative transaction");

        assertEquals(
                " | use blocked: canonical item is locked by an authoritative transaction",
                FabricCanonicalBagScreenHandler.readinessLabel(decision));
    }
}

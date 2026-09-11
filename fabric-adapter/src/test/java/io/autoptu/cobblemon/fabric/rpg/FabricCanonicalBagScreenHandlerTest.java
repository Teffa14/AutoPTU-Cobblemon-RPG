package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalBagQueryService;
import io.autoptu.cobblemon.authority.CanonicalItemUseService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FabricCanonicalBagScreenHandlerTest {
    @Test
    void displaysServerAuthoredReadyStateWithoutInventingItemEffects() {
        var entry = new CanonicalBagQueryService.BagEntry(
                "item-1", "field_ration", 3, 3, 0, null, false, 1L);
        var decision = CanonicalItemUseService.Decision.allowed(
                "item-1", "field_ration", "player-1", "bag_inspection", 3);

        String label = FabricCanonicalBagScreenHandler.displayName(entry, decision);

        assertTrue(label.contains("Field Ration x3"));
        assertTrue(label.contains("available 3"));
        assertTrue(label.contains("use ready"));
    }

    @Test
    void displaysExactServerBlockingReason() {
        var entry = new CanonicalBagQueryService.BagEntry(
                "item-2", "basic_bandage", 2, 0, 2, "reservation-7", false, 4L);
        var decision = CanonicalItemUseService.Decision.denied(
                "canonical item is locked by an authoritative transaction");

        String label = FabricCanonicalBagScreenHandler.displayName(entry, decision);

        assertTrue(label.contains("Basic Bandage x2"));
        assertTrue(label.contains("reserved 2"));
        assertTrue(label.contains("locked"));
        assertTrue(label.contains("use blocked: canonical item is locked by an authoritative transaction"));
    }
}

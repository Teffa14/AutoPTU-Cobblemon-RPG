package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WorldEncounterTriggerRequestServiceTest {
    @Test
    void createsOnePendingRequestPerCanonicalPlayerUntilConsumed() {
        WorldEncounterTriggerRequestService service = new WorldEncounterTriggerRequestService();

        WorldEncounterTriggerRequestService.Decision first = service.request(
                "player:abc", "overworld_wilds", "grass_walk", "minecraft:overworld", 10, 64, 12, 100
        );
        WorldEncounterTriggerRequestService.Decision duplicate = service.request(
                "player:abc", "overworld_wilds", "grass_walk", "minecraft:overworld", 20, 64, 22, 120
        );

        assertEquals(WorldEncounterTriggerRequestService.Outcome.CREATED, first.outcome());
        assertEquals(WorldEncounterTriggerRequestService.Outcome.ALREADY_PENDING, duplicate.outcome());
        assertEquals(first.request(), duplicate.request());
        assertEquals("player:abc", first.request().canonicalPlayerId());
        assertEquals("overworld_wilds", first.request().zoneId());
        assertEquals("grass_walk", first.request().contextId());
        assertEquals("minecraft:overworld", first.request().dimensionId());
        assertTrue(service.pendingForPlayer("player:abc").isPresent());

        assertTrue(service.clearForPlayer("player:abc"));
        assertTrue(service.pendingForPlayer("player:abc").isEmpty());

        WorldEncounterTriggerRequestService.Decision second = service.request(
                "player:abc", "overworld_wilds", "grass_walk", "minecraft:overworld", 20, 64, 22, 130
        );
        assertEquals(WorldEncounterTriggerRequestService.Outcome.CREATED, second.outcome());
        assertNotEquals(first.request().canonicalEncounterId(), second.request().canonicalEncounterId());
    }

    @Test
    void rejectsMissingCanonicalPlayerIdentity() {
        WorldEncounterTriggerRequestService service = new WorldEncounterTriggerRequestService();
        assertThrows(IllegalArgumentException.class, () -> service.request(
                " ", "overworld_wilds", "grass_walk", "minecraft:overworld", 0, 64, 0, 0
        ));
    }
}

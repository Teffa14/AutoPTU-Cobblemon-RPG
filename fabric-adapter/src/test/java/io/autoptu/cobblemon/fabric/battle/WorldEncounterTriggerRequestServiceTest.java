package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WorldEncounterTriggerRequestServiceTest {
    @Test
    void preservesPreboundEncounterAndVisibleActorIdentity() {
        WorldEncounterTriggerRequestService service = new WorldEncounterTriggerRequestService();

        WorldEncounterTriggerRequestService.Decision first = service.requestBoundEncounter(
                "world-wild:actor-42",
                "player:abc",
                "actor-42",
                "cedar_meadow",
                "visible_roaming_wild",
                "minecraft:overworld",
                10,
                64,
                12,
                100L
        );
        WorldEncounterTriggerRequestService.Decision duplicate = service.requestBoundEncounter(
                "world-wild:actor-99",
                "player:abc",
                "actor-99",
                "cedar_meadow",
                "visible_roaming_wild",
                "minecraft:overworld",
                20,
                64,
                22,
                120L
        );

        assertEquals(WorldEncounterTriggerRequestService.Outcome.CREATED, first.outcome());
        assertEquals(WorldEncounterTriggerRequestService.Outcome.ALREADY_PENDING, duplicate.outcome());
        assertEquals(first.request(), duplicate.request());
        assertEquals("world-wild:actor-42", first.request().canonicalEncounterId());
        assertEquals("actor-42", first.request().externalWildActorId());
        assertEquals("player:abc", first.request().canonicalPlayerId());
        assertEquals("cedar_meadow", first.request().zoneId());
        assertEquals("visible_roaming_wild", first.request().contextId());
        assertEquals("minecraft:overworld", first.request().dimensionId());
        assertTrue(service.pendingForPlayer("player:abc").isPresent());

        assertTrue(service.clearForPlayer("player:abc"));
        assertTrue(service.pendingForPlayer("player:abc").isEmpty());
    }

    @Test
    void legacyFallbackStillGeneratesServerOwnedIdentityWithoutActorCorrelation() {
        WorldEncounterTriggerRequestService service = new WorldEncounterTriggerRequestService();
        WorldEncounterTriggerRequestService.Decision decision = service.request(
                "player:abc", "admin_test", "fallback", "minecraft:overworld", 0, 64, 0, 1L
        );

        assertEquals(WorldEncounterTriggerRequestService.Outcome.CREATED, decision.outcome());
        assertNull(decision.request().externalWildActorId());
        assertTrue(decision.request().canonicalEncounterId().startsWith("world-encounter:player:abc:"));
    }

    @Test
    void rejectsMissingServerOwnedVisibleActorIdentity() {
        WorldEncounterTriggerRequestService service = new WorldEncounterTriggerRequestService();
        assertThrows(IllegalArgumentException.class, () -> service.requestBoundEncounter(
                "world-wild:actor-42",
                "player:abc",
                " ",
                "cedar_meadow",
                "visible_roaming_wild",
                "minecraft:overworld",
                0,
                64,
                0,
                1L
        ));
    }
}

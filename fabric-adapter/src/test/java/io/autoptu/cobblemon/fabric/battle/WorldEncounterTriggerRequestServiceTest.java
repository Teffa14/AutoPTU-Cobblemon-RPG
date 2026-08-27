package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class WorldEncounterTriggerRequestServiceTest {
    @Test
    void preservesCanonicalEncounterVisibleSpeciesAndOpaqueActorCorrelationSeparately() {
        WorldEncounterTriggerRequestService service = new WorldEncounterTriggerRequestService();

        WorldEncounterTriggerRequestService.Decision first = service.requestBoundEncounter(
                "world-wild:cedar-001",
                "player:abc",
                "minecraft-entity-42",
                "sentret",
                "cedar_meadow",
                "visible_roaming_wild",
                "minecraft:overworld",
                10,
                64,
                12,
                100L
        );
        WorldEncounterTriggerRequestService.Decision duplicate = service.requestBoundEncounter(
                "world-wild:cedar-002",
                "player:abc",
                "minecraft-entity-99",
                "hoppip",
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
        assertEquals("world-wild:cedar-001", first.request().canonicalEncounterId());
        assertEquals("minecraft-entity-42", first.request().externalWildActorId());
        assertNotEquals(first.request().canonicalEncounterId(), first.request().externalWildActorId());
        assertEquals("sentret", first.request().canonicalWildSpeciesId());
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
        assertNull(decision.request().canonicalWildSpeciesId());
        assertTrue(decision.request().canonicalEncounterId().startsWith("world-encounter:player:abc:"));
    }

    @Test
    void rejectsMissingServerOwnedVisibleActorOrSpeciesIdentity() {
        WorldEncounterTriggerRequestService service = new WorldEncounterTriggerRequestService();
        assertThrows(IllegalArgumentException.class, () -> service.requestBoundEncounter(
                "world-wild:cedar-001",
                "player:abc",
                " ",
                "sentret",
                "cedar_meadow",
                "visible_roaming_wild",
                "minecraft:overworld",
                0,
                64,
                0,
                1L
        ));
        assertThrows(IllegalArgumentException.class, () -> service.requestBoundEncounter(
                "world-wild:cedar-001",
                "player:abc",
                "minecraft-entity-42",
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

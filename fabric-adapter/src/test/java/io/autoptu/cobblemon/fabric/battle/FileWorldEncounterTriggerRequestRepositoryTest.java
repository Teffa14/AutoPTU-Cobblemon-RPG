package io.autoptu.cobblemon.fabric.battle;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

final class FileWorldEncounterTriggerRequestRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void persistsPendingVisibleEncounterAcrossRepositoryReopen() {
        FileWorldEncounterTriggerRequestRepository firstRepository =
                new FileWorldEncounterTriggerRequestRepository(tempDir);
        WorldEncounterTriggerRequestService firstService = new WorldEncounterTriggerRequestService(firstRepository);

        WorldEncounterTriggerRequestService.Decision created = firstService.requestBoundEncounter(
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
        assertEquals(WorldEncounterTriggerRequestService.Outcome.CREATED, created.outcome());

        WorldEncounterTriggerRequestService reopenedService = new WorldEncounterTriggerRequestService(
                new FileWorldEncounterTriggerRequestRepository(tempDir)
        );
        WorldEncounterTriggerRequestService.Request reopened = reopenedService.pendingForPlayer("player:abc").orElseThrow();
        assertEquals(created.request(), reopened);

        WorldEncounterTriggerRequestService.Decision duplicate = reopenedService.requestBoundEncounter(
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
        assertEquals(WorldEncounterTriggerRequestService.Outcome.ALREADY_PENDING, duplicate.outcome());
        assertEquals(created.request(), duplicate.request());
    }

    @Test
    void isolatesOwnersAndAllowsNewSessionOnlyAfterExplicitClear() {
        FileWorldEncounterTriggerRequestRepository repository = new FileWorldEncounterTriggerRequestRepository(tempDir);
        WorldEncounterTriggerRequestService service = new WorldEncounterTriggerRequestService(repository);

        service.requestBoundEncounter(
                "world-wild:a", "player:a", "actor-a", "zone", "visible", "minecraft:overworld", 1, 64, 1, 1L
        );
        service.requestBoundEncounter(
                "world-wild:b", "player:b", "actor-b", "zone", "visible", "minecraft:overworld", 2, 64, 2, 2L
        );

        assertEquals("world-wild:a", service.pendingForPlayer("player:a").orElseThrow().canonicalEncounterId());
        assertEquals("world-wild:b", service.pendingForPlayer("player:b").orElseThrow().canonicalEncounterId());
        assertFalse(service.clearForPlayer("player:missing"));
        assertTrue(service.clearForPlayer("player:a"));
        assertTrue(service.pendingForPlayer("player:a").isEmpty());
        assertTrue(service.pendingForPlayer("player:b").isPresent());

        WorldEncounterTriggerRequestService.Decision replacement = service.requestBoundEncounter(
                "world-wild:a2", "player:a", "actor-a2", "zone", "visible", "minecraft:overworld", 3, 64, 3, 3L
        );
        assertEquals(WorldEncounterTriggerRequestService.Outcome.CREATED, replacement.outcome());
        assertEquals("world-wild:a2", replacement.request().canonicalEncounterId());
    }
}

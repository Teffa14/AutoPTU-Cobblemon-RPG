package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalWorldEventObjectServiceTest {
    @TempDir Path tempDir;

    @Test
    void activatesAuthoredWorldEventOnceAndPersistsAcrossRepositoryReopen() {
        var players = new FileVersionedCanonicalStateRepository(tempDir);
        assertTrue(players.createPlayerIfAbsent(new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 0L)));
        var events = new FileCanonicalWorldEventObjectRepository(tempDir);
        var service = new CanonicalWorldEventObjectService(players, events);

        var first = service.activateShrine("player-1", "minecraft:overworld:10:64:10");
        assertTrue(first.allowed());
        assertTrue(first.newlyActivated());
        assertEquals(FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED, first.state().phase());
        assertEquals(1L, first.state().revision());

        var repeat = service.activateShrine("player-1", "minecraft:overworld:10:64:10");
        assertTrue(repeat.allowed());
        assertFalse(repeat.newlyActivated());
        assertEquals(1L, repeat.state().revision());

        var reopened = new FileCanonicalWorldEventObjectRepository(tempDir)
                .find("minecraft:overworld:10:64:10").orElseThrow();
        assertEquals(CanonicalWorldEventObjectService.SHRINE_EVENT_KEY, reopened.eventKey());
        assertEquals(FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED, reopened.phase());
        assertEquals(1L, reopened.revision());
    }

    @Test
    void latchesAuthoredSwitchOnceAndPersistsAcrossRepositoryReopen() {
        var players = new FileVersionedCanonicalStateRepository(tempDir);
        assertTrue(players.createPlayerIfAbsent(new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 0L)));
        var events = new FileCanonicalWorldEventObjectRepository(tempDir);
        var service = new CanonicalWorldEventObjectService(players, events);

        var first = service.activateSwitch("player-1", "minecraft:overworld:12:64:10");
        assertTrue(first.allowed());
        assertTrue(first.newlyActivated());
        assertEquals(CanonicalWorldEventObjectService.SWITCH_EVENT_KEY, first.state().eventKey());
        assertEquals(FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED, first.state().phase());
        assertEquals(1L, first.state().revision());

        var repeat = service.activateSwitch("player-1", "minecraft:overworld:12:64:10");
        assertTrue(repeat.allowed());
        assertFalse(repeat.newlyActivated());
        assertEquals(1L, repeat.state().revision());

        var reopened = new FileCanonicalWorldEventObjectRepository(tempDir)
                .find("minecraft:overworld:12:64:10").orElseThrow();
        assertEquals(CanonicalWorldEventObjectService.SWITCH_EVENT_KEY, reopened.eventKey());
        assertEquals(FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED, reopened.phase());
        assertEquals(1L, reopened.revision());
    }

    @Test
    void opensAuthoredDoorOnceAndPersistsAcrossRepositoryReopen() {
        var players = new FileVersionedCanonicalStateRepository(tempDir);
        assertTrue(players.createPlayerIfAbsent(new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 0L)));
        var events = new FileCanonicalWorldEventObjectRepository(tempDir);
        var service = new CanonicalWorldEventObjectService(players, events);
        String objectId = "minecraft:overworld:16:64:10";

        var first = service.activateDoor("player-1", objectId);
        assertTrue(first.allowed());
        assertTrue(first.newlyActivated());
        assertEquals(CanonicalWorldEventObjectService.DOOR_EVENT_KEY, first.state().eventKey());
        assertEquals(FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED, first.state().phase());
        assertEquals(1L, first.state().revision());

        var repeat = service.activateDoor("player-1", objectId);
        assertTrue(repeat.allowed());
        assertFalse(repeat.newlyActivated());
        assertEquals(1L, repeat.state().revision());

        var reopened = new FileCanonicalWorldEventObjectRepository(tempDir).find(objectId).orElseThrow();
        assertEquals(CanonicalWorldEventObjectService.DOOR_EVENT_KEY, reopened.eventKey());
        assertEquals(FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED, reopened.phase());
        assertEquals(1L, reopened.revision());
    }

    @Test
    void refusesToReuseWorldObjectForDifferentCanonicalEvent() {
        var players = new FileVersionedCanonicalStateRepository(tempDir);
        assertTrue(players.createPlayerIfAbsent(new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 0L)));
        var events = new FileCanonicalWorldEventObjectRepository(tempDir);
        var service = new CanonicalWorldEventObjectService(players, events);
        String objectId = "minecraft:overworld:14:64:10";

        assertTrue(service.activateShrine("player-1", objectId).allowed());
        var denied = service.activateSwitch("player-1", objectId);
        assertFalse(denied.allowed());
        assertEquals(CanonicalWorldEventObjectService.SHRINE_EVENT_KEY,
                events.find(objectId).orElseThrow().eventKey());
    }

    @Test
    void durableSnapshotListsPersistedObjectsForRestartReconciliation() {
        var repository = new FileCanonicalWorldEventObjectRepository(tempDir);
        var first = repository.findOrCreate("minecraft:overworld:10:64:10", CanonicalWorldEventObjectService.SHRINE_EVENT_KEY);
        repository.activate(first.objectId(), first.eventKey(), first.revision());
        repository.findOrCreate("minecraft:overworld:20:64:20", "ouros_other_event");

        var reopened = new FileCanonicalWorldEventObjectRepository(tempDir).findAll();
        assertEquals(2, reopened.size());
        assertEquals("minecraft:overworld:10:64:10", reopened.get(0).objectId());
        assertEquals(FileCanonicalWorldEventObjectRepository.Phase.ACTIVATED, reopened.get(0).phase());
        assertEquals("minecraft:overworld:20:64:20", reopened.get(1).objectId());
        assertEquals(FileCanonicalWorldEventObjectRepository.Phase.DORMANT, reopened.get(1).phase());
    }

    @Test
    void rejectsMissingTrainerWithoutCreatingEventState() {
        var players = new FileVersionedCanonicalStateRepository(tempDir);
        var events = new FileCanonicalWorldEventObjectRepository(tempDir);
        var service = new CanonicalWorldEventObjectService(players, events);

        var denied = service.activateShrine("missing-player", "minecraft:overworld:10:64:10");
        assertFalse(denied.allowed());
        assertTrue(events.find("minecraft:overworld:10:64:10").isEmpty());
    }

    @Test
    void staleRevisionCannotOverwriteActivatedState() {
        var repository = new FileCanonicalWorldEventObjectRepository(tempDir);
        var initial = repository.findOrCreate("minecraft:overworld:10:64:10", CanonicalWorldEventObjectService.SHRINE_EVENT_KEY);
        assertEquals(0L, initial.revision());

        var activated = repository.activate(initial.objectId(), initial.eventKey(), 0L);
        assertEquals(FileCanonicalWorldEventObjectRepository.Status.ACTIVATED, activated.status());

        var stale = repository.activate(initial.objectId(), initial.eventKey(), 0L);
        assertEquals(FileCanonicalWorldEventObjectRepository.Status.ALREADY_ACTIVE, stale.status());
        assertEquals(1L, stale.state().revision());
    }
}

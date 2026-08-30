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

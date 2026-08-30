package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CanonicalLocationDiscoveryServiceTest {
    @TempDir Path tempDir;

    @Test
    void discoversAuthoredLocationOnceAndPersistsAcrossRepositoryReopen() {
        var players = new FileVersionedCanonicalStateRepository(tempDir);
        assertTrue(players.createPlayerIfAbsent(new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 0L)));
        var discoveries = new FileCanonicalLocationDiscoveryRepository(tempDir);
        var service = new CanonicalLocationDiscoveryService(CanonicalLocationCatalogue.DEFAULT, players, discoveries);

        var first = service.observe("player-1", "overworld_spawn");
        assertTrue(first.allowed());
        assertTrue(first.newlyDiscovered());
        assertEquals(1L, first.revision());

        var retry = service.observe("player-1", "overworld_spawn");
        assertTrue(retry.allowed());
        assertFalse(retry.newlyDiscovered());
        assertEquals(1L, retry.revision());

        var reopened = new FileCanonicalLocationDiscoveryRepository(tempDir).findOrCreate("player-1");
        assertEquals(1L, reopened.revision());
        assertEquals(Set.of("overworld_spawn"), reopened.locationIds());
    }

    @Test
    void rejectsUnknownLocationAndPlayerWithoutCanonicalTrainer() {
        var players = new FileVersionedCanonicalStateRepository(tempDir);
        var discoveries = new FileCanonicalLocationDiscoveryRepository(tempDir);
        var service = new CanonicalLocationDiscoveryService(CanonicalLocationCatalogue.DEFAULT, players, discoveries);

        var missingPlayer = service.observe("missing-player", "overworld_spawn");
        assertFalse(missingPlayer.allowed());
        assertTrue(discoveries.find("missing-player").isEmpty());

        assertTrue(players.createPlayerIfAbsent(new CanonicalPlayerState(
                "player-1", Set.of(), Map.of(), Set.of(), 0L)));
        var unknown = service.observe("player-1", "client-invented-location");
        assertFalse(unknown.allowed());
        assertTrue(discoveries.find("player-1").isEmpty());
    }

    @Test
    void staleRepositoryWriteCannotReplaceNewerDiscoveryState() {
        var repository = new FileCanonicalLocationDiscoveryRepository(tempDir);
        var initial = repository.findOrCreate("player-1");
        assertEquals(0L, initial.revision());

        var committed = repository.discover("player-1", "overworld_spawn", 0L);
        assertEquals(FileCanonicalLocationDiscoveryRepository.Status.DISCOVERED, committed.status());

        var stale = repository.discover("player-1", "second-location", 0L);
        assertEquals(FileCanonicalLocationDiscoveryRepository.Status.STALE_REVISION, stale.status());
        assertFalse(stale.state().locationIds().contains("second-location"));
    }
}

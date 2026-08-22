package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.DataOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileVersionedCanonicalStateRepositoryTest {
    @TempDir
    Path tempDir;

    @Test
    void survivesRepositoryRestartWithExactCanonicalState() {
        CanonicalPlayerState initial = state(0, 3, 7, 41, "team-red");
        FileVersionedCanonicalStateRepository writer = new FileVersionedCanonicalStateRepository(tempDir);
        assertTrue(writer.createPlayerIfAbsent(initial));

        FileVersionedCanonicalStateRepository afterRestart = new FileVersionedCanonicalStateRepository(tempDir);
        assertEquals(initial, afterRestart.findPlayer(initial.playerId()).orElseThrow());
        assertFalse(afterRestart.createPlayerIfAbsent(state(0, 99, 99, 99, "forged")));
        assertEquals(initial, afterRestart.findPlayer(initial.playerId()).orElseThrow());
    }

    @Test
    void compareAndSetPersistsExactlyOneRevisionAdvance() {
        FileVersionedCanonicalStateRepository repository = new FileVersionedCanonicalStateRepository(tempDir);
        CanonicalPlayerState initial = state(4, 2, 5, 35, "team-blue");
        CanonicalPlayerState replacement = state(5, 4, 8, 39, "team-blue");
        assertTrue(repository.createPlayerIfAbsent(initial));

        assertTrue(repository.replacePlayerIfRevision(initial.playerId(), 4, replacement));
        assertFalse(repository.replacePlayerIfRevision(initial.playerId(), 4, state(5, 10, 10, 10, "team-blue")));

        FileVersionedCanonicalStateRepository afterRestart = new FileVersionedCanonicalStateRepository(tempDir);
        assertEquals(replacement, afterRestart.findPlayer(initial.playerId()).orElseThrow());
    }

    @Test
    void twoIndependentWritersCannotBothWinTheSameRevision() throws Exception {
        FileVersionedCanonicalStateRepository first = new FileVersionedCanonicalStateRepository(tempDir);
        FileVersionedCanonicalStateRepository second = new FileVersionedCanonicalStateRepository(tempDir);
        assertTrue(first.createPlayerIfAbsent(state(10, 1, 2, 30, "team-race")));

        CanonicalPlayerState candidateA = state(11, 2, 2, 30, "team-race");
        CanonicalPlayerState candidateB = state(11, 1, 3, 30, "team-race");
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            Future<Boolean> writeA = executor.submit(() -> {
                start.await();
                return first.replacePlayerIfRevision("player-1", 10, candidateA);
            });
            Future<Boolean> writeB = executor.submit(() -> {
                start.await();
                return second.replacePlayerIfRevision("player-1", 10, candidateB);
            });
            start.countDown();

            boolean a = writeA.get();
            boolean b = writeB.get();
            assertEquals(1, (a ? 1 : 0) + (b ? 1 : 0));
        }

        CanonicalPlayerState persisted = new FileVersionedCanonicalStateRepository(tempDir)
                .findPlayer("player-1").orElseThrow();
        assertEquals(11, persisted.revision());
        assertTrue(persisted.equals(candidateA) || persisted.equals(candidateB));
    }

    @Test
    void repositoryRejectsIdentityAndRevisionForgeryBeforeWriting() {
        FileVersionedCanonicalStateRepository repository = new FileVersionedCanonicalStateRepository(tempDir);
        CanonicalPlayerState initial = state(2, 1, 1, 30, "team-safe");
        assertTrue(repository.createPlayerIfAbsent(initial));

        CanonicalPlayerState wrongIdentity = new CanonicalPlayerState(
                "other-player",
                initial.trainerClasses(), initial.skillRanks(), initial.availablePokemonCapabilities(),
                initial.trainerFeatures(), initial.actionPoints(), initial.initiativeModifier(),
                initial.explicitInitiativeSpeed(), initial.teamId(), 3);
        assertThrows(IllegalArgumentException.class,
                () -> repository.replacePlayerIfRevision("player-1", 2, wrongIdentity));
        assertThrows(IllegalArgumentException.class,
                () -> repository.replacePlayerIfRevision("player-1", 2, state(4, 1, 1, 30, "team-safe")));
        assertEquals(initial, repository.findPlayer("player-1").orElseThrow());
    }

    @Test
    void unsupportedSchemaFailsClosed() throws Exception {
        FileVersionedCanonicalStateRepository repository = new FileVersionedCanonicalStateRepository(tempDir);
        CanonicalPlayerState initial = state(0, 1, 1, 30, "team-schema");
        assertTrue(repository.createPlayerIfAbsent(initial));

        Path stateFile;
        try (var files = Files.list(tempDir.resolve("players"))) {
            stateFile = files.filter(path -> path.getFileName().toString().endsWith(".bin"))
                    .findFirst().orElseThrow();
        }
        try (DataOutputStream output = new DataOutputStream(Files.newOutputStream(stateFile))) {
            output.writeInt(0x41505455);
            output.writeInt(FileVersionedCanonicalStateRepository.SCHEMA_VERSION + 1);
        }

        assertThrows(IllegalStateException.class, () -> repository.findPlayer("player-1"));
    }

    private static CanonicalPlayerState state(
            long revision,
            int actionPoints,
            int athletics,
            int initiativeSpeed,
            String teamId
    ) {
        return new CanonicalPlayerState(
                "player-1",
                Set.of("Ace Trainer", "Commander"),
                Map.of("athletics", athletics, "command", 6),
                Set.of("ride", "swim"),
                Set.of("Orders", "Focused Training"),
                actionPoints,
                2,
                initiativeSpeed,
                teamId,
                revision
        );
    }
}

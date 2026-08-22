package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FileCanonicalPlayerEncounterProfileRepositoryTest {
    @TempDir
    Path root;

    @Test
    void profileSurvivesRepositoryRecreation() {
        CanonicalPlayerEncounterProfile expected = profile(4L);
        FileCanonicalPlayerEncounterProfileRepository first =
                new FileCanonicalPlayerEncounterProfileRepository(root);

        assertTrue(first.createProfileIfAbsent(expected));

        FileCanonicalPlayerEncounterProfileRepository reopened =
                new FileCanonicalPlayerEncounterProfileRepository(root);
        assertEquals(expected, reopened.findProfile("player-1").orElseThrow());
    }

    @Test
    void staleRevisionCannotReplaceCurrentProfile() {
        FileCanonicalPlayerEncounterProfileRepository repository =
                new FileCanonicalPlayerEncounterProfileRepository(root);
        assertTrue(repository.createProfileIfAbsent(profile(4L)));

        CanonicalPlayerEncounterProfile replacement = new CanonicalPlayerEncounterProfile(
                "player-1",
                List.of("pokemon-2"),
                Map.of(),
                arena(),
                5L
        );
        assertFalse(repository.replaceProfileIfRevision("player-1", 3L, replacement));
        assertEquals(profile(4L), repository.findProfile("player-1").orElseThrow());
    }

    @Test
    void exactlyOneRepositoryInstanceWinsSameRevisionRace() throws Exception {
        FileCanonicalPlayerEncounterProfileRepository first =
                new FileCanonicalPlayerEncounterProfileRepository(root);
        FileCanonicalPlayerEncounterProfileRepository second =
                new FileCanonicalPlayerEncounterProfileRepository(root);
        assertTrue(first.createProfileIfAbsent(profile(10L)));

        CanonicalPlayerEncounterProfile replacementA = new CanonicalPlayerEncounterProfile(
                "player-1", List.of("pokemon-a"), Map.of("item-a", 1), arena(), 11L);
        CanonicalPlayerEncounterProfile replacementB = new CanonicalPlayerEncounterProfile(
                "player-1", List.of("pokemon-b"), Map.of("item-b", 2), arena(), 11L);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean a = new AtomicBoolean();
        AtomicBoolean b = new AtomicBoolean();

        Thread writerA = new Thread(() -> awaitAndReplace(start, first, replacementA, a));
        Thread writerB = new Thread(() -> awaitAndReplace(start, second, replacementB, b));
        writerA.start();
        writerB.start();
        start.countDown();
        writerA.join();
        writerB.join();

        assertTrue(a.get() ^ b.get());
        CanonicalPlayerEncounterProfile stored = first.findProfile("player-1").orElseThrow();
        assertEquals(11L, stored.revision());
        assertTrue(stored.equals(replacementA) || stored.equals(replacementB));
    }

    private static void awaitAndReplace(
            CountDownLatch start,
            FileCanonicalPlayerEncounterProfileRepository repository,
            CanonicalPlayerEncounterProfile replacement,
            AtomicBoolean result
    ) {
        try {
            start.await();
            result.set(repository.replaceProfileIfRevision("player-1", 10L, replacement));
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new AssertionError(interrupted);
        }
    }

    private static CanonicalPlayerEncounterProfile profile(long revision) {
        return new CanonicalPlayerEncounterProfile(
                "player-1",
                List.of("pokemon-1"),
                Map.of("item-1", 2),
                arena(),
                revision
        );
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 12, 64, -8, 1, 0, 0, 1);
    }
}

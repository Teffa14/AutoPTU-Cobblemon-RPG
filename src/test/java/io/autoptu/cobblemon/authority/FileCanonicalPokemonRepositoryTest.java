package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
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

class FileCanonicalPokemonRepositoryTest {
    @TempDir Path tempDir;

    @Test
    void fullAggregateSurvivesRepositoryRestartWithoutLosingStackedStatusMetadata() {
        CanonicalPokemonState expected = fullState(7L, 41, "item-held-1");
        FileCanonicalPokemonRepository first = new FileCanonicalPokemonRepository(tempDir);

        assertTrue(first.createPokemonIfAbsent(expected));
        assertFalse(first.createPokemonIfAbsent(expected));

        FileCanonicalPokemonRepository reopened = new FileCanonicalPokemonRepository(tempDir);
        CanonicalPokemonState actual = reopened.findPokemon("pkmn-1").orElseThrow();

        assertEquals(expected, actual);
        assertEquals(List.of("poisoned", "burned", "poisoned"),
                actual.statusState().entries().stream().map(CanonicalStatusEntry::name).toList());
        assertEquals(Set.of("poisoned", "burned"), actual.statuses());
        assertEquals(3L, actual.statusState().entries().get(0).payload().get("remaining"));
        assertEquals(Boolean.TRUE, actual.statusState().entries().get(0).payload().get("severe"));
        assertEquals(null, actual.statusState().entries().get(0).payload().get("nullable"));
    }

    @Test
    void revisionCasRejectsStaleIdentityAndRevisionForgery() {
        FileCanonicalPokemonRepository repository = new FileCanonicalPokemonRepository(tempDir);
        CanonicalPokemonState initial = fullState(3L, 25, null);
        assertTrue(repository.createPokemonIfAbsent(initial));

        CanonicalPokemonState replacement = fullState(4L, 19, "item-held-2");
        assertTrue(repository.replacePokemonIfRevision("pkmn-1", 3L, replacement));
        assertFalse(repository.replacePokemonIfRevision("pkmn-1", 3L, replacement));
        assertEquals(replacement, repository.findPokemon("pkmn-1").orElseThrow());

        CanonicalPokemonState wrongIdentity = new CanonicalPokemonState(
                "pkmn-other", replacement.ownerPlayerId(), replacement.speciesId(), replacement.level(),
                replacement.capabilities(), replacement.statuses(), replacement.statusState(), replacement.combatStats(),
                replacement.health(), replacement.moveLoadout(), replacement.baseMovement(), replacement.battleTraits(),
                replacement.accuracyEvasion(), replacement.injuryState(), replacement.heldItemInstanceId(), 5L);
        assertThrows(IllegalArgumentException.class,
                () -> repository.replacePokemonIfRevision("pkmn-1", 4L, wrongIdentity));

        CanonicalPokemonState skippedRevision = fullState(6L, 18, null);
        assertThrows(IllegalArgumentException.class,
                () -> repository.replacePokemonIfRevision("pkmn-1", 4L, skippedRevision));
    }

    @Test
    void independentRepositoryInstancesAllowExactlyOneWriterForOneRevision() throws Exception {
        FileCanonicalPokemonRepository first = new FileCanonicalPokemonRepository(tempDir);
        FileCanonicalPokemonRepository second = new FileCanonicalPokemonRepository(tempDir);
        assertTrue(first.createPokemonIfAbsent(fullState(10L, 30, null)));

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> a = pool.submit(() -> raceWrite(first, ready, start, 21));
            Future<Boolean> b = pool.submit(() -> raceWrite(second, ready, start, 22));
            ready.await();
            start.countDown();

            boolean firstWon = a.get();
            boolean secondWon = b.get();
            assertEquals(1, (firstWon ? 1 : 0) + (secondWon ? 1 : 0));

            CanonicalPokemonState durable = new FileCanonicalPokemonRepository(tempDir)
                    .findPokemon("pkmn-1").orElseThrow();
            assertEquals(11L, durable.revision());
            assertTrue(durable.health().currentHp() == 21 || durable.health().currentHp() == 22);
        } finally {
            pool.shutdownNow();
        }
    }

    private static boolean raceWrite(
            FileCanonicalPokemonRepository repository,
            CountDownLatch ready,
            CountDownLatch start,
            int hp
    ) throws InterruptedException {
        ready.countDown();
        start.await();
        return repository.replacePokemonIfRevision("pkmn-1", 10L, fullState(11L, hp, null));
    }

    private static CanonicalPokemonState fullState(long revision, int currentHp, String heldItem) {
        LinkedHashMap<String, Object> firstPayload = new LinkedHashMap<>();
        firstPayload.put("source", "move:a");
        firstPayload.put("remaining", 3L);
        firstPayload.put("ratio", 1.5d);
        firstPayload.put("severe", true);
        firstPayload.put("nullable", null);
        CanonicalStatusState statusState = new CanonicalStatusState(List.of(
                new CanonicalStatusEntry("Poisoned", firstPayload),
                new CanonicalStatusEntry("Burned", Map.of("applied_round", 4)),
                new CanonicalStatusEntry("POISONED", Map.of("source", "trainer_feature:stack"))
        ));
        return new CanonicalPokemonState(
                "pkmn-1",
                "player-1",
                "pikachu",
                28,
                Set.of("tracker", "mountable"),
                statusState.names(),
                statusState,
                new CanonicalCombatStats(12, 13, 14, 15, 16),
                new CanonicalHealth(currentHp, 50),
                new CanonicalMoveLoadout(List.of("thunder-shock", "quick-attack")),
                new CanonicalBaseMovement(6, 2, 0, 2, 1),
                new CanonicalBattleTraits(List.of("Electric"), List.of("Static", "Lightning Rod")),
                new CanonicalAccuracyEvasion(1, 2, 3, 4),
                new CanonicalInjuryState(2),
                heldItem,
                revision
        );
    }
}

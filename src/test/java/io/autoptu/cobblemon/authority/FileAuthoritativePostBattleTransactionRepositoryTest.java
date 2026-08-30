package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileAuthoritativePostBattleTransactionRepositoryTest {
    @TempDir Path tempDirectory;

    @Test
    void preparedPayloadSurvivesRepositoryRecreationAndCommittedMarkerPersists() {
        AuthoritativePostBattleTransaction prepared = AuthoritativePostBattleTransaction.prepared(
                "battle-1",
                "player-1",
                "sha256:engine-final",
                Map.of("item-potion", 2),
                List.of(
                        new AuthoritativePostBattlePokemonFinalState(
                                "pokemon-1",
                                7,
                                new CanonicalHealth(9, 35),
                                new CanonicalStatusState(List.of(new CanonicalStatusEntry(
                                        "burned", Map.of("source", "engine", "ticks", 2L)))),
                                new CanonicalInjuryState(1)),
                        new AuthoritativePostBattlePokemonFinalState(
                                "pokemon-2",
                                11,
                                new CanonicalHealth(0, 28),
                                CanonicalStatusState.fromNames(java.util.Set.of("poisoned")),
                                new CanonicalInjuryState(2))));

        FileAuthoritativePostBattleTransactionRepository first =
                new FileAuthoritativePostBattleTransactionRepository(tempDirectory);
        assertTrue(first.createIfAbsent(prepared));

        FileAuthoritativePostBattleTransactionRepository restarted =
                new FileAuthoritativePostBattleTransactionRepository(tempDirectory);
        assertEquals(prepared, restarted.find("battle-1").orElseThrow());
        assertEquals(List.of(prepared), restarted.findPending());

        assertTrue(restarted.markCommitted("battle-1"));
        FileAuthoritativePostBattleTransactionRepository restartedAgain =
                new FileAuthoritativePostBattleTransactionRepository(tempDirectory);
        assertEquals(AuthoritativePostBattleTransaction.Phase.COMMITTED,
                restartedAgain.find("battle-1").orElseThrow().phase());
        assertTrue(restartedAgain.findPending().isEmpty());
    }
}

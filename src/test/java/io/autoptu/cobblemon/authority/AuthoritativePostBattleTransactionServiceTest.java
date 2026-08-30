package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthoritativePostBattleTransactionServiceTest {
    @TempDir Path tempDirectory;

    @Test
    void commitsTrustedItemsAndWholePokemonRosterExactlyOnce() {
        Fixture fixture = fixture();
        AuthoritativePostBattleTransactionService service = fixture.service();

        AuthoritativePostBattleTransactionDecision first = service.commit(
                "player-1", "battle-1", "sha256:engine-final", Map.of("item-potion", 1), finalStates());
        AuthoritativePostBattleTransactionDecision replay = service.commit(
                "player-1", "battle-1", "sha256:engine-final", Map.of("item-potion", 1), finalStates());

        assertTrue(first.accepted());
        assertTrue(replay.accepted());
        assertTrue(replay.idempotent());
        assertEquals(1, fixture.authority.outcomeCommitCount);
        assertEquals(8, fixture.pokemon.findPokemon("pokemon-1").orElseThrow().revision());
        assertEquals(12, fixture.pokemon.findPokemon("pokemon-2").orElseThrow().revision());
        assertEquals(new CanonicalHealth(9, 35), fixture.pokemon.findPokemon("pokemon-1").orElseThrow().health());
        assertEquals(new CanonicalHealth(0, 28), fixture.pokemon.findPokemon("pokemon-2").orElseThrow().health());
        assertEquals(AuthoritativePostBattleTransaction.Phase.COMMITTED,
                fixture.transactions.find("battle-1").orElseThrow().phase());
    }

    @Test
    void restartRecoversPreparedTransactionAfterOutcomeReceiptWasAlreadyCommitted() {
        Fixture fixture = fixture();
        AuthoritativePostBattleTransaction prepared = AuthoritativePostBattleTransaction.prepared(
                "battle-1", "player-1", "sha256:engine-final", Map.of("item-potion", 1), finalStates());
        assertTrue(fixture.transactions.createIfAbsent(prepared));

        BattleOutcomeDecision outcome = new BattleOutcomeCommitService(fixture.authority, fixture.authority)
                .commitEngineOutcome("player-1", "battle-1", "sha256:engine-final", Map.of("item-potion", 1));
        assertTrue(outcome.accepted());
        assertEquals(1, fixture.authority.outcomeCommitCount);
        assertEquals(7, fixture.pokemon.findPokemon("pokemon-1").orElseThrow().revision());

        AuthoritativePostBattleTransactionService restarted = new AuthoritativePostBattleTransactionService(
                fixture.authority,
                fixture.authority,
                new FileCanonicalPokemonRepository(tempDirectory.resolve("pokemon")),
                new FileAuthoritativePostBattleTransactionRepository(tempDirectory.resolve("transactions")));
        List<AuthoritativePostBattleTransactionDecision> recovered = restarted.recoverPending();

        assertEquals(1, recovered.size());
        assertTrue(recovered.get(0).accepted());
        assertEquals(1, fixture.authority.outcomeCommitCount);
        FileCanonicalPokemonRepository afterRestart = new FileCanonicalPokemonRepository(tempDirectory.resolve("pokemon"));
        assertEquals(8, afterRestart.findPokemon("pokemon-1").orElseThrow().revision());
        assertEquals(12, afterRestart.findPokemon("pokemon-2").orElseThrow().revision());
        assertTrue(new FileAuthoritativePostBattleTransactionRepository(tempDirectory.resolve("transactions"))
                .findPending().isEmpty());
    }

    @Test
    void conflictingOrIncompletePayloadFailsBeforeMutation() {
        Fixture fixture = fixture();
        AuthoritativePostBattleTransactionService service = fixture.service();
        List<AuthoritativePostBattlePokemonFinalState> incomplete = List.of(finalStates().get(0));

        AuthoritativePostBattleTransactionDecision incompleteDecision = service.commit(
                "player-1", "battle-1", "sha256:engine-final", Map.of("item-potion", 1), incomplete);
        assertFalse(incompleteDecision.accepted());
        assertEquals("post_battle_pokemon_roster_incomplete", incompleteDecision.reason());
        assertEquals(0, fixture.authority.outcomeCommitCount);
        assertTrue(fixture.transactions.find("battle-1").isEmpty());

        assertTrue(service.commit(
                "player-1", "battle-1", "sha256:engine-final", Map.of("item-potion", 1), finalStates()).accepted());
        AuthoritativePostBattleTransactionDecision conflict = service.commit(
                "player-1", "battle-1", "sha256:different", Map.of("item-potion", 1), finalStates());
        assertFalse(conflict.accepted());
        assertEquals("post_battle_transaction_payload_conflict", conflict.reason());
        assertEquals(1, fixture.authority.outcomeCommitCount);
    }

    private Fixture fixture() {
        Path pokemonRoot = tempDirectory.resolve("pokemon");
        Path transactionRoot = tempDirectory.resolve("transactions");
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(pokemonRoot);
        CanonicalPokemonState first = pokemon("pokemon-1", 7, 35);
        CanonicalPokemonState second = pokemon("pokemon-2", 11, 28);
        assertTrue(pokemon.createPokemonIfAbsent(first));
        assertTrue(pokemon.createPokemonIfAbsent(second));

        BattleAuthoritySnapshot snapshot = new BattleAuthoritySnapshot(
                "battle-1",
                "player-1",
                new BattleTrainerSnapshot("player-1", Set.of("Ace Trainer"), Map.of("Command", 4), 17),
                List.of(BattlePokemonSnapshot.from(first), BattlePokemonSnapshot.from(second)),
                List.of(new BattleItemSnapshot("item-potion", "player-1", "autoptu:hyper_potion", 2, 44, false)),
                991L);
        FakeBattleAuthority authority = new FakeBattleAuthority(snapshot);
        FileAuthoritativePostBattleTransactionRepository transactions =
                new FileAuthoritativePostBattleTransactionRepository(transactionRoot);
        return new Fixture(authority, pokemon, transactions);
    }

    private static List<AuthoritativePostBattlePokemonFinalState> finalStates() {
        return List.of(
                new AuthoritativePostBattlePokemonFinalState(
                        "pokemon-1", 7, new CanonicalHealth(9, 35),
                        CanonicalStatusState.fromNames(Set.of("burned")), new CanonicalInjuryState(1)),
                new AuthoritativePostBattlePokemonFinalState(
                        "pokemon-2", 11, new CanonicalHealth(0, 28),
                        CanonicalStatusState.fromNames(Set.of("poisoned")), new CanonicalInjuryState(2)));
    }

    private static CanonicalPokemonState pokemon(String pokemonId, long revision, int maxHp) {
        return new CanonicalPokemonState(
                pokemonId, "player-1", "cobblemon:eevee", 20,
                Set.of("Run Up"), Set.of(), CanonicalStatusState.fromNames(Set.of()),
                new CanonicalCombatStats(25, 25, 25, 25, 25), new CanonicalHealth(maxHp, maxHp),
                new CanonicalMoveLoadout(List.of("tackle")), new CanonicalBaseMovement(6, 4, 0, 0, 0),
                null, null, new CanonicalInjuryState(0), null, revision);
    }

    private record Fixture(
            FakeBattleAuthority authority,
            FileCanonicalPokemonRepository pokemon,
            FileAuthoritativePostBattleTransactionRepository transactions
    ) {
        AuthoritativePostBattleTransactionService service() {
            return new AuthoritativePostBattleTransactionService(authority, authority, pokemon, transactions);
        }
    }

    private static final class FakeBattleAuthority implements BattleSnapshotRepository, BattleOutcomeRepository {
        private BattleAuthoritySnapshot snapshot;
        private final Map<String, BattleOutcomeCommit> outcomes = new LinkedHashMap<>();
        private int outcomeCommitCount;

        private FakeBattleAuthority(BattleAuthoritySnapshot snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public Optional<BattleAuthoritySnapshot> findSnapshot(String reservationId) {
            return snapshot != null && snapshot.reservationId().equals(reservationId)
                    ? Optional.of(snapshot)
                    : Optional.empty();
        }

        @Override
        public boolean tryReserveSnapshot(BattleAuthoritySnapshot requested) {
            return false;
        }

        @Override
        public boolean releaseSnapshot(String reservationId, String playerId) {
            return false;
        }

        @Override
        public Optional<BattleOutcomeCommit> findCommittedOutcome(String reservationId) {
            return Optional.ofNullable(outcomes.get(reservationId));
        }

        @Override
        public synchronized boolean tryCommitOutcome(BattleAuthoritySnapshot expected, BattleOutcomeCommit outcome) {
            if (snapshot == null || !snapshot.equals(expected) || outcomes.containsKey(expected.reservationId())) {
                return false;
            }
            outcomes.put(expected.reservationId(), outcome);
            snapshot = null;
            outcomeCommitCount++;
            return true;
        }
    }
}

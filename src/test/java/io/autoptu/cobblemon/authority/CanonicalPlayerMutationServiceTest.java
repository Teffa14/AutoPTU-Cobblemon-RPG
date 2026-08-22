package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalPlayerMutationServiceTest {
    @Test
    void appliesServerMutationOnlyWhenRevisionStillMatches() {
        FakeRepository repository = new FakeRepository(player(4, 2));
        CanonicalPlayerMutationService service = new CanonicalPlayerMutationService(repository);

        CanonicalPlayerMutationDecision decision = service.mutate("player-1", 4, current ->
                new CanonicalPlayerState(
                        current.playerId(), current.trainerClasses(), current.skillRanks(),
                        current.availablePokemonCapabilities(), current.trainerFeatures(), 3,
                        current.initiativeModifier(), current.explicitInitiativeSpeed(), current.teamId(),
                        current.revision() + 1
                ));

        assertTrue(decision.applied());
        assertEquals(5, decision.state().revision());
        assertEquals(3, decision.state().actionPoints());
        assertEquals(decision.state(), repository.state);
    }

    @Test
    void rejectsStaleRevisionBeforeMutationRuns() {
        FakeRepository repository = new FakeRepository(player(7, 2));
        CanonicalPlayerMutationService service = new CanonicalPlayerMutationService(repository);
        AtomicBoolean mutationRan = new AtomicBoolean(false);

        CanonicalPlayerMutationDecision decision = service.mutate("player-1", 6, current -> {
            mutationRan.set(true);
            return current;
        });

        assertEquals(CanonicalPlayerMutationDecision.Outcome.STALE_REVISION, decision.outcome());
        assertFalse(mutationRan.get());
        assertEquals(7, decision.state().revision());
    }

    @Test
    void rejectsIdentityChangeOrSkippedRevision() {
        FakeRepository repository = new FakeRepository(player(2, 1));
        CanonicalPlayerMutationService service = new CanonicalPlayerMutationService(repository);

        CanonicalPlayerMutationDecision changedIdentity = service.mutate("player-1", 2, current ->
                player("other-player", 3, 1));
        assertEquals(CanonicalPlayerMutationDecision.Outcome.INVALID_MUTATION, changedIdentity.outcome());

        CanonicalPlayerMutationDecision skippedRevision = service.mutate("player-1", 2, current ->
                player(4, 1));
        assertEquals(CanonicalPlayerMutationDecision.Outcome.INVALID_MUTATION, skippedRevision.outcome());
        assertEquals(2, repository.state.revision());
    }

    @Test
    void reportsConcurrentWriteWhenRepositoryCasLosesRace() {
        FakeRepository repository = new FakeRepository(player(3, 1));
        repository.loseNextCompareAndSet = true;
        CanonicalPlayerMutationService service = new CanonicalPlayerMutationService(repository);

        CanonicalPlayerMutationDecision decision = service.mutate("player-1", 3, current ->
                player(4, 2));

        assertEquals(CanonicalPlayerMutationDecision.Outcome.CONCURRENT_WRITE, decision.outcome());
        assertEquals(4, decision.state().revision());
        assertEquals(99, decision.state().actionPoints());
    }

    @Test
    void missingPlayerFailsClosed() {
        FakeRepository repository = new FakeRepository(null);
        CanonicalPlayerMutationDecision decision = new CanonicalPlayerMutationService(repository)
                .mutate("player-1", 0, current -> current);
        assertEquals(CanonicalPlayerMutationDecision.Outcome.PLAYER_NOT_FOUND, decision.outcome());
    }

    private static CanonicalPlayerState player(long revision, int actionPoints) {
        return player("player-1", revision, actionPoints);
    }

    private static CanonicalPlayerState player(String playerId, long revision, int actionPoints) {
        return new CanonicalPlayerState(
                playerId,
                Set.of("ace-trainer"),
                Map.of("command", 3),
                Set.of("overland"),
                Set.of("Agility Training"),
                actionPoints,
                1,
                12,
                "team-player",
                revision
        );
    }

    private static final class FakeRepository implements VersionedCanonicalStateRepository {
        private CanonicalPlayerState state;
        private boolean loseNextCompareAndSet;

        private FakeRepository(CanonicalPlayerState state) {
            this.state = state;
        }

        @Override
        public Optional<CanonicalPlayerState> findPlayer(String playerId) {
            if (state == null || !state.playerId().equals(playerId)) return Optional.empty();
            return Optional.of(state);
        }

        @Override
        public boolean replacePlayerIfRevision(
                String playerId,
                long expectedRevision,
                CanonicalPlayerState replacement
        ) {
            if (state == null || !state.playerId().equals(playerId) || state.revision() != expectedRevision) {
                return false;
            }
            if (loseNextCompareAndSet) {
                loseNextCompareAndSet = false;
                state = player(expectedRevision + 1, 99);
                return false;
            }
            state = replacement;
            return true;
        }
    }
}

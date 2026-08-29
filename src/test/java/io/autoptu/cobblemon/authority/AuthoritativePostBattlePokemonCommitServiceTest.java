package io.autoptu.cobblemon.authority;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AuthoritativePostBattlePokemonCommitServiceTest {
    @TempDir
    Path tempDirectory;

    @Test
    void engineAuthoredHealthStatusAndInjuryPersistAcrossRepositoryRestart() {
        FileCanonicalPokemonRepository repository = new FileCanonicalPokemonRepository(tempDirectory);
        assertTrue(repository.createPokemonIfAbsent(initialPokemon("player-1", 7)));
        AuthoritativePostBattlePokemonCommitService service =
                new AuthoritativePostBattlePokemonCommitService(repository);

        CanonicalHealth finalHealth = new CanonicalHealth(9, 35);
        CanonicalStatusState finalStatuses = CanonicalStatusState.fromNames(Set.of("burned"));
        CanonicalInjuryState finalInjuries = new CanonicalInjuryState(2);
        AuthoritativePostBattlePokemonCommitDecision decision = service.commit(
                "player-1", "pokemon-1", 7, finalHealth, finalStatuses, finalInjuries);

        assertTrue(decision.accepted());
        assertFalse(decision.idempotent());
        assertEquals(8, decision.state().revision());

        FileCanonicalPokemonRepository restarted = new FileCanonicalPokemonRepository(tempDirectory);
        CanonicalPokemonState persisted = restarted.findPokemon("pokemon-1").orElseThrow();
        assertEquals(finalHealth, persisted.health());
        assertEquals(finalStatuses, persisted.statusState());
        assertEquals(finalInjuries, persisted.injuryState());
        assertEquals(8, persisted.revision());
        assertEquals("cobblemon:charizard", persisted.speciesId());
        assertEquals("item-charcoal", persisted.heldItemInstanceId());
    }

    @Test
    void exactReplayIsIdempotentAndCannotAdvanceRevisionTwice() {
        FileCanonicalPokemonRepository repository = new FileCanonicalPokemonRepository(tempDirectory);
        assertTrue(repository.createPokemonIfAbsent(initialPokemon("player-1", 12)));
        AuthoritativePostBattlePokemonCommitService service =
                new AuthoritativePostBattlePokemonCommitService(repository);
        CanonicalHealth finalHealth = new CanonicalHealth(0, 35);
        CanonicalStatusState finalStatuses = CanonicalStatusState.fromNames(Set.of("poisoned"));
        CanonicalInjuryState finalInjuries = new CanonicalInjuryState(1);

        AuthoritativePostBattlePokemonCommitDecision first = service.commit(
                "player-1", "pokemon-1", 12, finalHealth, finalStatuses, finalInjuries);
        AuthoritativePostBattlePokemonCommitDecision replay = service.commit(
                "player-1", "pokemon-1", 12, finalHealth, finalStatuses, finalInjuries);

        assertTrue(first.accepted());
        assertFalse(first.idempotent());
        assertTrue(replay.accepted());
        assertTrue(replay.idempotent());
        assertEquals(13, repository.findPokemon("pokemon-1").orElseThrow().revision());
    }

    @Test
    void differentReplayFailsClosed() {
        FileCanonicalPokemonRepository repository = new FileCanonicalPokemonRepository(tempDirectory);
        assertTrue(repository.createPokemonIfAbsent(initialPokemon("player-1", 3)));
        AuthoritativePostBattlePokemonCommitService service =
                new AuthoritativePostBattlePokemonCommitService(repository);
        service.commit(
                "player-1", "pokemon-1", 3,
                new CanonicalHealth(20, 35), CanonicalStatusState.fromNames(Set.of()), new CanonicalInjuryState(0));

        AuthoritativePostBattlePokemonCommitDecision forged = service.commit(
                "player-1", "pokemon-1", 3,
                new CanonicalHealth(1, 35), CanonicalStatusState.fromNames(Set.of()), new CanonicalInjuryState(0));

        assertFalse(forged.accepted());
        assertEquals("post_battle_state_already_changed", forged.reason());
        assertEquals(new CanonicalHealth(20, 35), repository.findPokemon("pokemon-1").orElseThrow().health());
    }

    @Test
    void wrongOwnerAndStaleRevisionCannotMutateCanonicalPokemon() {
        FileCanonicalPokemonRepository repository = new FileCanonicalPokemonRepository(tempDirectory);
        assertTrue(repository.createPokemonIfAbsent(initialPokemon("player-1", 9)));
        AuthoritativePostBattlePokemonCommitService service =
                new AuthoritativePostBattlePokemonCommitService(repository);

        AuthoritativePostBattlePokemonCommitDecision wrongOwner = service.commit(
                "player-2", "pokemon-1", 9,
                new CanonicalHealth(5, 35), CanonicalStatusState.fromNames(Set.of()), new CanonicalInjuryState(0));
        AuthoritativePostBattlePokemonCommitDecision stale = service.commit(
                "player-1", "pokemon-1", 8,
                new CanonicalHealth(5, 35), CanonicalStatusState.fromNames(Set.of()), new CanonicalInjuryState(0));

        assertFalse(wrongOwner.accepted());
        assertEquals("pokemon_not_owned", wrongOwner.reason());
        assertFalse(stale.accepted());
        assertEquals("pokemon_revision_changed", stale.reason());
        assertEquals(9, repository.findPokemon("pokemon-1").orElseThrow().revision());
        assertEquals(new CanonicalHealth(35, 35), repository.findPokemon("pokemon-1").orElseThrow().health());
    }

    private static CanonicalPokemonState initialPokemon(String owner, long revision) {
        return new CanonicalPokemonState(
                "pokemon-1",
                owner,
                "cobblemon:charizard",
                25,
                Set.of("Sky"),
                Set.of(),
                CanonicalStatusState.fromNames(Set.of()),
                new CanonicalCombatStats(30, 25, 40, 30, 35),
                new CanonicalHealth(35, 35),
                new CanonicalMoveLoadout(java.util.List.of("ember")),
                new CanonicalBaseMovement(6, 4, 8, 2, 1),
                null,
                null,
                new CanonicalInjuryState(0),
                "item-charcoal",
                revision);
    }
}

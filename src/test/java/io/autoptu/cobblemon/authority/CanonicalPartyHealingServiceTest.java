package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class CanonicalPartyHealingServiceTest {
    @Test
    void healsCanonicalPartyHpAndPreservesStatusesAndInjuries() {
        String playerId = "minecraft-player:abc";
        CanonicalPlayerEncounterProfile profile = profile(playerId, List.of("poke-1", "poke-2"));
        FakePartyRepository parties = new FakePartyRepository(profile);
        FakePokemonRepository pokemon = new FakePokemonRepository(Map.of(
                "poke-1", pokemon("poke-1", playerId, 4, 20, Set.of("burned"), 2),
                "poke-2", pokemon("poke-2", playerId, 15, 15, Set.of(), 0)
        ));

        CanonicalPartyHealingDecision decision = new CanonicalPartyHealingService(parties, pokemon)
                .healParty(playerId);

        assertEquals(CanonicalPartyHealingDecision.Outcome.APPLIED, decision.outcome());
        assertEquals(1, decision.healedPokemon());
        assertEquals(1, decision.alreadyFullPokemon());
        CanonicalPokemonState healed = pokemon.states.get("poke-1");
        assertEquals(new CanonicalHealth(20, 20), healed.health());
        assertEquals(Set.of("burned"), healed.statuses());
        assertEquals(2, healed.injuryState().injuries());
        assertEquals(1, healed.revision());
    }

    @Test
    void refusesPokemonThatIsNotOwnedByAuthenticatedPlayer() {
        String playerId = "minecraft-player:abc";
        FakePartyRepository parties = new FakePartyRepository(profile(playerId, List.of("foreign")));
        CanonicalPokemonState foreign = pokemon("foreign", "minecraft-player:other", 1, 10, Set.of(), 0);
        FakePokemonRepository pokemon = new FakePokemonRepository(Map.of("foreign", foreign));

        CanonicalPartyHealingDecision decision = new CanonicalPartyHealingService(parties, pokemon)
                .healParty(playerId);

        assertEquals(CanonicalPartyHealingDecision.Outcome.PARTIAL, decision.outcome());
        assertEquals(List.of("foreign"), decision.failedPokemonIds());
        assertSame(foreign, pokemon.states.get("foreign"));
    }

    @Test
    void failsClosedWhenPlayerHasNoCanonicalParty() {
        CanonicalPartyHealingDecision decision = new CanonicalPartyHealingService(
                new FakePartyRepository(null), new FakePokemonRepository(Map.of()))
                .healParty("minecraft-player:abc");

        assertEquals(CanonicalPartyHealingDecision.Outcome.NO_PARTY, decision.outcome());
        assertEquals(0, decision.healedPokemon());
    }

    private static CanonicalPlayerEncounterProfile profile(String playerId, List<String> pokemonIds) {
        return new CanonicalPlayerEncounterProfile(
                playerId,
                pokemonIds,
                Map.of(),
                new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1),
                0
        );
    }

    private static CanonicalPokemonState pokemon(
            String pokemonId,
            String owner,
            int currentHp,
            int maxHp,
            Set<String> statuses,
            int injuries
    ) {
        return new CanonicalPokemonState(
                pokemonId,
                owner,
                "bulbasaur",
                5,
                Set.of(),
                statuses,
                CanonicalStatusState.fromNames(statuses),
                null,
                new CanonicalHealth(currentHp, maxHp),
                null,
                null,
                null,
                null,
                new CanonicalInjuryState(injuries),
                null,
                0
        );
    }

    private static final class FakePartyRepository implements VersionedCanonicalPlayerEncounterProfileRepository {
        private final CanonicalPlayerEncounterProfile profile;

        private FakePartyRepository(CanonicalPlayerEncounterProfile profile) {
            this.profile = profile;
        }

        @Override
        public Optional<CanonicalPlayerEncounterProfile> findProfile(String playerId) {
            return profile == null ? Optional.empty() : Optional.of(profile);
        }

        @Override
        public boolean createProfileIfAbsent(CanonicalPlayerEncounterProfile initialProfile) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean replaceProfileIfRevision(
                String playerId,
                long expectedRevision,
                CanonicalPlayerEncounterProfile replacement
        ) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FakePokemonRepository implements VersionedCanonicalPokemonRepository {
        private final Map<String, CanonicalPokemonState> states;

        private FakePokemonRepository(Map<String, CanonicalPokemonState> initial) {
            states = new LinkedHashMap<>(initial);
        }

        @Override
        public Optional<CanonicalPokemonState> findPokemon(String pokemonId) {
            return Optional.ofNullable(states.get(pokemonId));
        }

        @Override
        public boolean createPokemonIfAbsent(CanonicalPokemonState initialState) {
            return states.putIfAbsent(initialState.pokemonId(), initialState) == null;
        }

        @Override
        public boolean replacePokemonIfRevision(
                String pokemonId,
                long expectedRevision,
                CanonicalPokemonState replacement
        ) {
            CanonicalPokemonState current = states.get(pokemonId);
            if (current == null || current.revision() != expectedRevision) return false;
            states.put(pokemonId, replacement);
            return true;
        }
    }
}

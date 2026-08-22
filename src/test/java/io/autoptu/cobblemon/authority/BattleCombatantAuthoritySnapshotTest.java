package io.autoptu.cobblemon.authority;

import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.util.Arrays;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BattleCombatantAuthoritySnapshotTest {
    @Test
    void freezesPlayerPokemonWithoutUsingPersistenceOwnerAsTeamIdentity() {
        CanonicalPokemonState playerPokemon = new CanonicalPokemonState(
                "pokemon-player-1",
                "inventory-owner-1",
                "cobblemon:charizard",
                42,
                Set.of("Sky", "Power 5"),
                8
        );

        BattleCombatantAuthoritySnapshot snapshot = BattleCombatantAuthoritySnapshot.from(
                new PlayerCanonicalBattlePokemonView(playerPokemon),
                "trainer-combatant-1",
                "team-red",
                BattleParticipantKind.PLAYER
        );

        assertEquals("pokemon-player-1", snapshot.combatantId());
        assertEquals("trainer-combatant-1", snapshot.participantId());
        assertEquals("team-red", snapshot.teamId());
        assertEquals(BattleParticipantKind.PLAYER, snapshot.participantKind());
        assertEquals("cobblemon:charizard", snapshot.speciesId());
        assertEquals(42, snapshot.level());
        assertEquals(8, snapshot.revision());
    }

    @Test
    void freezesWildEncounterPokemonWithoutInventingPlayerOwnership() {
        CanonicalEncounterPokemonState wildPokemon = new CanonicalEncounterPokemonState(
                "encounter-pokemon-7",
                "cobblemon:pikachu",
                12,
                Set.of("Overland"),
                Set.of("Burned"),
                null,
                null,
                new CanonicalHealth(24, 24),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                3
        );

        BattleCombatantAuthoritySnapshot snapshot = BattleCombatantAuthoritySnapshot.from(
                wildPokemon,
                "wild-pack-7",
                "team-wild",
                BattleParticipantKind.WILD
        );

        assertEquals("encounter-pokemon-7", snapshot.combatantId());
        assertEquals("wild-pack-7", snapshot.participantId());
        assertEquals("team-wild", snapshot.teamId());
        assertEquals(BattleParticipantKind.WILD, snapshot.participantKind());
        assertEquals(Set.of("burned"), snapshot.statuses());
        assertEquals(24, snapshot.health().currentHp());

        assertFalse(Arrays.stream(CanonicalEncounterPokemonState.class.getRecordComponents())
                .map(RecordComponent::getName)
                .anyMatch("ownerPlayerId"::equals));
    }

    @Test
    void rejectsMissingExplicitBattleAffiliation() {
        CanonicalEncounterPokemonState wildPokemon = new CanonicalEncounterPokemonState(
                "wild-1", "cobblemon:pikachu", 5, Set.of(), Set.of(), null,
                null, null, null, null, null, null, null, null, 1);

        assertThrows(IllegalArgumentException.class, () -> BattleCombatantAuthoritySnapshot.from(
                wildPokemon, "participant", " ", BattleParticipantKind.WILD));
        assertThrows(IllegalArgumentException.class, () -> BattleCombatantAuthoritySnapshot.from(
                wildPokemon, "participant", "team", null));
    }
}

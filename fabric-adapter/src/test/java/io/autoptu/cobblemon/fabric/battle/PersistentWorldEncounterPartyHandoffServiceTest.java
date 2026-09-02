package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalAccuracyEvasion;
import io.autoptu.cobblemon.authority.CanonicalBaseMovement;
import io.autoptu.cobblemon.authority.CanonicalBattleTraits;
import io.autoptu.cobblemon.authority.CanonicalCombatStats;
import io.autoptu.cobblemon.authority.CanonicalHealth;
import io.autoptu.cobblemon.authority.CanonicalInjuryState;
import io.autoptu.cobblemon.authority.CanonicalMoveLoadout;
import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.CanonicalStatusState;
import io.autoptu.cobblemon.authority.VersionedCanonicalPlayerEncounterProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentWorldEncounterPartyHandoffServiceTest {
    @Test
    void reResolvesPersistentPartyAndFreezesExactRegistryBlueprint() {
        var profile = new CanonicalPlayerEncounterProfile(
                "player-1", List.of("pokemon-a", "pokemon-b"), Map.of("potion", 2), arena(), 4L);
        var blueprint = new CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint(
                "marea-fletchling-1", 1, List.of(seed()));
        var service = new PersistentWorldEncounterPartyHandoffService(
                id -> Optional.of(new CanonicalPlayerState(id, Set.of(), Map.of(), Set.of(), 1L)),
                profiles(profile),
                id -> id.equals("marea-fletchling-1") ? Optional.of(blueprint) : Optional.empty()
        );

        var decision = service.reserve(request());

        assertTrue(decision.ready());
        assertEquals(PersistentWorldEncounterPartyHandoffService.Outcome.CREATED, decision.outcome());
        assertEquals(List.of("pokemon-a", "pokemon-b"), decision.reservation().canonicalPlayerPokemonIds());
        assertEquals(Map.of("potion", 2), decision.reservation().consumableQuantities());
        assertEquals(blueprint, decision.reservation().wildBlueprint());
        assertEquals("visible-actor-uuid", decision.reservation().externalWildActorId());
    }

    @Test
    void missingPersistentProfileFailsBeforeCreatingHandoff() {
        var service = new PersistentWorldEncounterPartyHandoffService(
                id -> Optional.of(new CanonicalPlayerState(id, Set.of(), Map.of(), Set.of(), 1L)),
                profiles(null),
                id -> Optional.empty()
        );

        var decision = service.reserve(request());

        assertFalse(decision.ready());
        assertEquals(PersistentWorldEncounterPartyHandoffService.Outcome.ENCOUNTER_PROFILE_MISSING, decision.outcome());
        assertTrue(service.findByPlayerId("player-1").isEmpty());
    }

    private static VersionedCanonicalPlayerEncounterProfileRepository profiles(CanonicalPlayerEncounterProfile profile) {
        return new VersionedCanonicalPlayerEncounterProfileRepository() {
            @Override public Optional<CanonicalPlayerEncounterProfile> findProfile(String playerId) {
                return Optional.ofNullable(profile);
            }
            @Override public boolean createProfileIfAbsent(CanonicalPlayerEncounterProfile initialProfile) { return false; }
            @Override public boolean replaceProfileIfRevision(
                    String playerId, long expectedRevision, CanonicalPlayerEncounterProfile replacement) { return false; }
        };
    }

    private static WorldEncounterTriggerRequestService.Request request() {
        return new WorldEncounterTriggerRequestService.Request(
                "marea-fletchling-1", "player-1", "visible-actor-uuid", "marea", "visible_roaming_wild",
                "minecraft:overworld", 12, 64, 18, 900L);
    }

    private static ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint seed() {
        return new ServerOwnedWildEncounterProvisioningService.WildPokemonBlueprint(
                "fletchling", 8, Set.of("tracker"), Set.of(), new CanonicalStatusState(List.of()),
                new CanonicalCombatStats(8, 9, 10, 11, 12), new CanonicalHealth(31, 31),
                new CanonicalMoveLoadout(List.of("tackle")), new CanonicalBaseMovement(5, 2, 0, 1, 1),
                new CanonicalBattleTraits(List.of("normal", "flying"), List.of("big-pecks")),
                new CanonicalAccuracyEvasion(0, 0, 0, 0), new CanonicalInjuryState(0), null, 1L);
    }

    private static BattleArenaSnapshot arena() {
        return new BattleArenaSnapshot("minecraft:overworld", 0, 64, 0, 1, 0, 0, 1);
    }
}

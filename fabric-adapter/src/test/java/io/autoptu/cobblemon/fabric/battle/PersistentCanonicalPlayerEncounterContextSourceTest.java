package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.FileCanonicalPlayerEncounterProfileRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentCanonicalPlayerEncounterContextSourceTest {
    @TempDir
    Path root;

    @Test
    void resolvesAuthenticatedUuidThroughCanonicalIdentityAndDurableProfile() {
        UUID externalPlayerUuid = UUID.randomUUID();
        CobblemonCanonicalEncounterIdentityRegistry identities = registry(externalPlayerUuid);
        CanonicalPlayerState player = new CanonicalPlayerState(
                "player-canonical", Set.of(), Map.of(), Set.of(), 7L);
        FileCanonicalPlayerEncounterProfileRepository profiles =
                new FileCanonicalPlayerEncounterProfileRepository(root);
        CanonicalPlayerEncounterProfile profile = profile();
        assertTrue(profiles.createProfileIfAbsent(profile));

        PersistentCanonicalPlayerEncounterContextSource source =
                new PersistentCanonicalPlayerEncounterContextSource(
                        identities,
                        playerId -> playerId.equals(player.playerId()) ? Optional.of(player) : Optional.empty(),
                        profiles
                );

        CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext context =
                source.resolve(externalPlayerUuid).orElseThrow();
        assertEquals("player-canonical", context.canonicalPlayerId());
        assertEquals(List.of("pokemon-canonical"), context.canonicalPokemonIds());
        assertEquals(Map.of("item-canonical", 2), context.consumableQuantities());
        assertEquals(profile.arena(), context.arena());
    }

    @Test
    void failsClosedWhenCanonicalPlayerRecordIsMissing() {
        UUID externalPlayerUuid = UUID.randomUUID();
        FileCanonicalPlayerEncounterProfileRepository profiles =
                new FileCanonicalPlayerEncounterProfileRepository(root);
        assertTrue(profiles.createProfileIfAbsent(profile()));
        PersistentCanonicalPlayerEncounterContextSource source =
                new PersistentCanonicalPlayerEncounterContextSource(
                        registry(externalPlayerUuid),
                        ignored -> Optional.empty(),
                        profiles
                );

        assertTrue(source.resolve(externalPlayerUuid).isEmpty());
    }

    @Test
    void failsClosedWhenExternalIdentityIsNotRegistered() {
        UUID externalPlayerUuid = UUID.randomUUID();
        CanonicalPlayerState player = new CanonicalPlayerState(
                "player-canonical", Set.of(), Map.of(), Set.of(), 7L);
        FileCanonicalPlayerEncounterProfileRepository profiles =
                new FileCanonicalPlayerEncounterProfileRepository(root);
        assertTrue(profiles.createProfileIfAbsent(profile()));
        PersistentCanonicalPlayerEncounterContextSource source =
                new PersistentCanonicalPlayerEncounterContextSource(
                        new CobblemonCanonicalEncounterIdentityRegistry(),
                        ignored -> Optional.of(player),
                        profiles
                );

        assertTrue(source.resolve(externalPlayerUuid).isEmpty());
    }

    private static CobblemonCanonicalEncounterIdentityRegistry registry(UUID externalPlayerUuid) {
        CobblemonCanonicalEncounterIdentityRegistry identities =
                new CobblemonCanonicalEncounterIdentityRegistry();
        LinkedHashMap<String, String> pokemon = new LinkedHashMap<>();
        pokemon.put(UUID.randomUUID().toString(), "pokemon-canonical");
        identities.register(
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                externalPlayerUuid.toString(),
                "player-canonical",
                pokemon
        );
        return identities;
    }

    private static CanonicalPlayerEncounterProfile profile() {
        return new CanonicalPlayerEncounterProfile(
                "player-canonical",
                List.of("pokemon-canonical"),
                Map.of("item-canonical", 2),
                new BattleArenaSnapshot("minecraft:overworld", 4, 70, 9, 1, 0, 0, 1),
                3L
        );
    }
}

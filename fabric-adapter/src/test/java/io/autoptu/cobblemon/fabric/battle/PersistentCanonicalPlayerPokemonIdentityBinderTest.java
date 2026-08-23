package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.BattleArenaSnapshot;
import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.CanonicalPokemonState;
import io.autoptu.cobblemon.authority.FileCanonicalPlayerEncounterProfileRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPokemonRepository;
import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PersistentCanonicalPlayerPokemonIdentityBinderTest {
    private static final UUID PLAYER_UUID = UUID.fromString("b6b5fca3-4fc5-4fc8-b61e-d0a9057e01a4");
    private static final String PLAYER_ID = "minecraft-player:" + PLAYER_UUID;
    private static final String EXTERNAL_ONE = "73d1f324-c44d-42e6-89e8-c17fd8eea57d";
    private static final String EXTERNAL_TWO = "0244a6a0-2dc2-41fc-9fe3-c53054454e38";
    private static final BattleArenaSnapshot ARENA = new BattleArenaSnapshot(
            "minecraft:overworld", 20, 64, 30, 1, 0, 0, 1
    );

    @TempDir Path tempDir;

    @Test
    void bindsOnlyOpaqueExternalPokemonIdsToOwnedDurableCanonicalRoster() {
        Stores stores = seed(List.of("pokemon-1"), PLAYER_ID);
        CobblemonCanonicalEncounterIdentityRegistry registry = new CobblemonCanonicalEncounterIdentityRegistry();
        PersistentCanonicalPlayerPokemonIdentityBinder binder = binder(true, registry, stores);

        assertTrue(binder.bind(PLAYER_UUID.toString(), List.of(EXTERNAL_ONE)));
        var resolved = registry.resolve(new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1,
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                PLAYER_UUID.toString(),
                List.of(EXTERNAL_ONE)
        )).orElseThrow();

        assertEquals(PLAYER_ID, resolved.participantId());
        assertEquals(List.of("pokemon-1"), resolved.combatantIds());
    }

    @Test
    void rejectsOfflineMalformedRosterMismatchAndForeignOwnership() {
        Stores stores = seed(List.of("pokemon-1"), PLAYER_ID);
        CobblemonCanonicalEncounterIdentityRegistry registry = new CobblemonCanonicalEncounterIdentityRegistry();

        assertFalse(binder(false, registry, stores).bind(PLAYER_UUID.toString(), List.of(EXTERNAL_ONE)));
        assertFalse(binder(true, registry, stores).bind("not-a-uuid", List.of(EXTERNAL_ONE)));
        assertFalse(binder(true, registry, stores).bind(PLAYER_UUID.toString(), List.of(EXTERNAL_ONE, EXTERNAL_TWO)));

        Stores foreign = seed(List.of("pokemon-foreign"), "another-player");
        assertFalse(binder(true, new CobblemonCanonicalEncounterIdentityRegistry(), foreign)
                .bind(PLAYER_UUID.toString(), List.of(EXTERNAL_ONE)));
    }

    @Test
    void refreshesSamePlayerBindingWhenDurableRosterChanges() {
        Stores stores = seed(List.of("pokemon-1"), PLAYER_ID);
        CobblemonCanonicalEncounterIdentityRegistry registry = new CobblemonCanonicalEncounterIdentityRegistry();
        PersistentCanonicalPlayerPokemonIdentityBinder binder = binder(true, registry, stores);
        assertTrue(binder.bind(PLAYER_UUID.toString(), List.of(EXTERNAL_ONE)));

        stores.pokemon().createPokemonIfAbsent(pokemon("pokemon-2", PLAYER_ID));
        CanonicalPlayerEncounterProfile replacement = new CanonicalPlayerEncounterProfile(
                PLAYER_ID, List.of("pokemon-2"), Map.of(), ARENA, 1L
        );
        assertTrue(stores.profiles().replaceProfileIfRevision(PLAYER_ID, 0L, replacement));

        assertTrue(binder.bind(PLAYER_UUID.toString(), List.of(EXTERNAL_TWO)));
        assertTrue(registry.resolve(new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1,
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                PLAYER_UUID.toString(),
                List.of(EXTERNAL_ONE)
        )).isEmpty());
        assertEquals(List.of("pokemon-2"), registry.resolve(new CobblemonBattleStartInterceptor.ParticipantIdentity(
                1,
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                PLAYER_UUID.toString(),
                List.of(EXTERNAL_TWO)
        )).orElseThrow().combatantIds());
    }

    private PersistentCanonicalPlayerPokemonIdentityBinder binder(
            boolean online,
            CobblemonCanonicalEncounterIdentityRegistry registry,
            Stores stores
    ) {
        return new PersistentCanonicalPlayerPokemonIdentityBinder(
                ignored -> online,
                registry,
                stores.players(),
                stores.profiles(),
                stores.pokemon()
        );
    }

    private Stores seed(List<String> roster, String pokemonOwner) {
        FileVersionedCanonicalStateRepository players = new FileVersionedCanonicalStateRepository(tempDir);
        FileCanonicalPlayerEncounterProfileRepository profiles = new FileCanonicalPlayerEncounterProfileRepository(tempDir);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(tempDir);
        players.createPlayerIfAbsent(player());
        for (String pokemonId : roster) pokemon.createPokemonIfAbsent(pokemon(pokemonId, pokemonOwner));
        profiles.createProfileIfAbsent(new CanonicalPlayerEncounterProfile(
                PLAYER_ID, roster, Map.of(), ARENA, 0L
        ));
        return new Stores(players, profiles, pokemon);
    }

    private static CanonicalPlayerState player() {
        return new CanonicalPlayerState(
                FabricCanonicalPlayerProvisioning.canonicalPlayerId(PLAYER_UUID),
                Set.of(), Map.of(), Set.of(), Set.of(), 0, 0, null, "", 0L
        );
    }

    private static CanonicalPokemonState pokemon(String pokemonId, String ownerPlayerId) {
        return new CanonicalPokemonState(
                pokemonId, ownerPlayerId, "pikachu", 5, Set.of(), 0L
        );
    }

    private record Stores(
            FileVersionedCanonicalStateRepository players,
            FileCanonicalPlayerEncounterProfileRepository profiles,
            FileCanonicalPokemonRepository pokemon
    ) {}
}

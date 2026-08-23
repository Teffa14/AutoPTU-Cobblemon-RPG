package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.authority.CanonicalPlayerEncounterProfile;
import io.autoptu.cobblemon.authority.CanonicalPokemonState;
import io.autoptu.cobblemon.authority.CanonicalStateRepository;
import io.autoptu.cobblemon.authority.VersionedCanonicalPlayerEncounterProfileRepository;
import io.autoptu.cobblemon.authority.VersionedCanonicalPokemonRepository;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Binds live Cobblemon Pokemon UUIDs to an authenticated player's already-canonical durable roster.
 *
 * External UUIDs are identity keys only. Canonical Pokemon IDs and ownership come from the durable
 * encounter profile and Pokemon repository. Species, level, HP, stats, moves, abilities, held items
 * and every other Cobblemon entity value are deliberately absent from this boundary.
 */
public final class PersistentCanonicalPlayerPokemonIdentityBinder {
    @FunctionalInterface
    interface AuthenticatedSessionLookup {
        boolean isOnline(UUID playerUuid);
    }

    private final AuthenticatedSessionLookup sessionLookup;
    private final CobblemonCanonicalEncounterIdentityRegistry identityRegistry;
    private final CanonicalStateRepository playerRepository;
    private final VersionedCanonicalPlayerEncounterProfileRepository profileRepository;
    private final VersionedCanonicalPokemonRepository pokemonRepository;

    PersistentCanonicalPlayerPokemonIdentityBinder(
            AuthenticatedSessionLookup sessionLookup,
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry,
            CanonicalStateRepository playerRepository,
            VersionedCanonicalPlayerEncounterProfileRepository profileRepository,
            VersionedCanonicalPokemonRepository pokemonRepository
    ) {
        this.sessionLookup = Objects.requireNonNull(sessionLookup, "sessionLookup");
        this.identityRegistry = Objects.requireNonNull(identityRegistry, "identityRegistry");
        this.playerRepository = Objects.requireNonNull(playerRepository, "playerRepository");
        this.profileRepository = Objects.requireNonNull(profileRepository, "profileRepository");
        this.pokemonRepository = Objects.requireNonNull(pokemonRepository, "pokemonRepository");
    }

    public static PersistentCanonicalPlayerPokemonIdentityBinder fromWorldRuntime(
            MinecraftServer server,
            CobblemonCanonicalEncounterIdentityRegistry identityRegistry
    ) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(identityRegistry, "identityRegistry");
        return new PersistentCanonicalPlayerPokemonIdentityBinder(
                uuid -> server.getPlayerManager().getPlayer(uuid) != null,
                identityRegistry,
                FabricCanonicalPlayerStoreRuntime.requireRepository(server),
                FabricCanonicalPlayerStoreRuntime.requireEncounterProfileRepository(server),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(server)
        );
    }

    public boolean bind(String externalPlayerActorId, List<String> externalPokemonIds) {
        Optional<UUID> authenticatedUuid = parseUuid(externalPlayerActorId);
        if (authenticatedUuid.isEmpty() || !sessionLookup.isOnline(authenticatedUuid.get())) return false;
        if (externalPokemonIds == null || externalPokemonIds.isEmpty()) return false;

        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(authenticatedUuid.get());
        if (playerRepository.findPlayer(playerId).isEmpty()) return false;

        Optional<CanonicalPlayerEncounterProfile> profileResult = profileRepository.findProfile(playerId);
        if (profileResult.isEmpty()) return false;
        CanonicalPlayerEncounterProfile profile = profileResult.get();
        if (!profile.playerId().equals(playerId)) return false;
        if (profile.pokemonIds().size() != externalPokemonIds.size()) return false;

        LinkedHashMap<String, String> mappings = new LinkedHashMap<>();
        Set<String> externalIds = new HashSet<>();
        for (int index = 0; index < externalPokemonIds.size(); index++) {
            Optional<String> externalPokemonId = normalizeUuid(externalPokemonIds.get(index));
            if (externalPokemonId.isEmpty() || !externalIds.add(externalPokemonId.get())) return false;

            String canonicalPokemonId = profile.pokemonIds().get(index);
            Optional<CanonicalPokemonState> pokemonResult = pokemonRepository.findPokemon(canonicalPokemonId);
            if (pokemonResult.isEmpty()) return false;
            CanonicalPokemonState pokemon = pokemonResult.get();
            if (!pokemon.pokemonId().equals(canonicalPokemonId)) return false;
            if (!pokemon.ownerPlayerId().equals(playerId)) return false;
            mappings.put(externalPokemonId.get(), canonicalPokemonId);
        }

        identityRegistry.registerOrReplace(
                CobblemonBattleStartInterceptor.ParticipantKind.PLAYER,
                authenticatedUuid.get().toString(),
                playerId,
                mappings
        );
        return true;
    }

    private static Optional<UUID> parseUuid(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(value.strip()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    private static Optional<String> normalizeUuid(String value) {
        Optional<UUID> uuid = parseUuid(value);
        return uuid.map(UUID::toString);
    }
}

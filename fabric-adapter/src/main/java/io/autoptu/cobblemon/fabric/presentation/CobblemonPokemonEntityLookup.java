package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves an opaque presentation entity ID to a live Cobblemon PokemonEntity on the server.
 *
 * This boundary is presentation-only. It does not read Pokemon stats, HP, ownership, moves,
 * abilities or any other PTU-authoritative state from the entity.
 */
public final class CobblemonPokemonEntityLookup {
    public Optional<PokemonEntity> find(MinecraftServer server, String presentationEntityId) {
        Objects.requireNonNull(server, "server");
        UUID entityId = presentationUuid(presentationEntityId);
        for (ServerWorld world : server.getWorlds()) {
            Entity indexed = world.getEntity(entityId);
            if (indexed != null) {
                return requirePokemonEntity(entityId, indexed);
            }

            // A newly spawned entity can be present in the world's live entity iterable before
            // ServerWorld's UUID index exposes it. Keep the lookup server-side and UUID-based while
            // tolerating that indexing window instead of trusting a caller-supplied entity handle.
            for (Entity live : world.iterateEntities()) {
                if (!entityId.equals(live.getUuid())) continue;
                return requirePokemonEntity(entityId, live);
            }
        }
        return Optional.empty();
    }

    private static Optional<PokemonEntity> requirePokemonEntity(UUID entityId, Entity entity) {
        if (!(entity instanceof PokemonEntity pokemonEntity)) {
            throw new IllegalStateException(
                    "presentation entity " + entityId + " is not a Cobblemon PokemonEntity");
        }
        return Optional.of(pokemonEntity);
    }

    static UUID presentationUuid(String presentationEntityId) {
        if (presentationEntityId == null || presentationEntityId.isBlank()) {
            throw new IllegalArgumentException("presentationEntityId is required");
        }
        try {
            return UUID.fromString(presentationEntityId.trim());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("presentationEntityId must be a UUID", exception);
        }
    }
}

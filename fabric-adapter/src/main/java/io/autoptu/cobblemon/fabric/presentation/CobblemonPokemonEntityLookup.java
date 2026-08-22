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
            Entity entity = world.getEntity(entityId);
            if (entity == null) continue;
            if (!(entity instanceof PokemonEntity pokemonEntity)) {
                throw new IllegalStateException(
                        "presentation entity " + entityId + " is not a Cobblemon PokemonEntity");
            }
            return Optional.of(pokemonEntity);
        }
        return Optional.empty();
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

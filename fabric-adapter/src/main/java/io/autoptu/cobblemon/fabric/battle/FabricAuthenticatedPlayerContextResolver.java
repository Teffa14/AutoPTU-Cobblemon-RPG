package io.autoptu.cobblemon.fabric.battle;

import net.minecraft.server.MinecraftServer;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Resolves a Cobblemon PLAYER actor identity only after Minecraft confirms that the UUID belongs
 * to a currently connected server player.
 *
 * The Minecraft session proves only authentication/presence. Canonical Trainer, Pokemon, item and
 * arena state still comes from CanonicalPlayerEncounterContextSource and must be server-owned.
 * No ServerPlayerEntity fields become PTU stats, inventory truth, modifiers, legality or outcomes.
 */
public final class FabricAuthenticatedPlayerContextResolver
        implements CobblemonPlayerVsWildClaimCoordinator.AuthenticatedPlayerContextResolver {

    @FunctionalInterface
    public interface CanonicalPlayerEncounterContextSource {
        Optional<CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext> resolve(UUID authenticatedPlayerUuid);
    }

    @FunctionalInterface
    interface OnlinePlayerLookup {
        boolean isOnline(UUID playerUuid);
    }

    private final OnlinePlayerLookup onlinePlayerLookup;
    private final CanonicalPlayerEncounterContextSource canonicalContextSource;

    public FabricAuthenticatedPlayerContextResolver(
            MinecraftServer server,
            CanonicalPlayerEncounterContextSource canonicalContextSource
    ) {
        this(serverLookup(server), canonicalContextSource);
    }

    FabricAuthenticatedPlayerContextResolver(
            OnlinePlayerLookup onlinePlayerLookup,
            CanonicalPlayerEncounterContextSource canonicalContextSource
    ) {
        this.onlinePlayerLookup = Objects.requireNonNull(onlinePlayerLookup, "onlinePlayerLookup");
        this.canonicalContextSource = Objects.requireNonNull(canonicalContextSource, "canonicalContextSource");
    }

    @Override
    public Optional<CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext> resolve(
            String externalPlayerActorId
    ) {
        Optional<UUID> playerUuid = parseCanonicalUuid(externalPlayerActorId);
        if (playerUuid.isEmpty()) return Optional.empty();

        UUID uuid = playerUuid.get();
        if (!onlinePlayerLookup.isOnline(uuid)) return Optional.empty();

        Optional<CobblemonPlayerVsWildClaimCoordinator.PlayerEncounterContext> context =
                canonicalContextSource.resolve(uuid);
        return context == null ? Optional.empty() : context;
    }

    private static OnlinePlayerLookup serverLookup(MinecraftServer server) {
        MinecraftServer authenticatedServer = Objects.requireNonNull(server, "server");
        return playerUuid -> authenticatedServer.getPlayerManager().getPlayer(playerUuid) != null;
    }

    private static Optional<UUID> parseCanonicalUuid(String value) {
        if (value == null || value.isBlank()) return Optional.empty();
        String token = value.strip();
        try {
            UUID uuid = UUID.fromString(token);
            if (!uuid.toString().equalsIgnoreCase(token)) return Optional.empty();
            return Optional.of(uuid);
        } catch (IllegalArgumentException invalidUuid) {
            return Optional.empty();
        }
    }
}

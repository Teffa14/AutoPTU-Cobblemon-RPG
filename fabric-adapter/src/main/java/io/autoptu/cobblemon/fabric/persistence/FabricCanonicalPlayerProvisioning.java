package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.CanonicalPlayerState;
import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Creates the minimal canonical player aggregate after Minecraft has authenticated a real player.
 *
 * Minecraft contributes only the authenticated UUID. The initial PTU aggregate is intentionally
 * empty/fail-closed: no classes, Features, skills, Pokemon capabilities, AP, initiative modifiers,
 * team, inventory or progression are inferred from ServerPlayerEntity state. Later character
 * creation/progression services must mutate these fields through server-authoritative contracts.
 */
public final class FabricCanonicalPlayerProvisioning {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final String CANONICAL_PLAYER_PREFIX = "minecraft-player:";

    private FabricCanonicalPlayerProvisioning() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                provisionAuthenticatedPlayer(server, handler.getPlayer()));
    }

    static CanonicalPlayerState provisionAuthenticatedPlayer(
            MinecraftServer server,
            ServerPlayerEntity player
    ) {
        if (server == null) throw new IllegalArgumentException("server is required");
        if (player == null) throw new IllegalArgumentException("player is required");
        return provision(FabricCanonicalPlayerStoreRuntime.requireRepository(server), player.getUuid());
    }

    static CanonicalPlayerState provision(
            FileVersionedCanonicalStateRepository repository,
            UUID authenticatedUuid
    ) {
        if (repository == null) throw new IllegalArgumentException("repository is required");
        String playerId = canonicalPlayerId(authenticatedUuid);
        repository.createPlayerIfAbsent(initialState(playerId));
        return repository.findPlayer(playerId)
                .orElseThrow(() -> new IllegalStateException("canonical player provisioning did not persist state"));
    }

    static String canonicalPlayerId(UUID authenticatedUuid) {
        if (authenticatedUuid == null) throw new IllegalArgumentException("authenticatedUuid is required");
        return CANONICAL_PLAYER_PREFIX + authenticatedUuid;
    }

    static CanonicalPlayerState initialState(String canonicalPlayerId) {
        if (canonicalPlayerId == null || canonicalPlayerId.isBlank()) {
            throw new IllegalArgumentException("canonicalPlayerId is required");
        }
        return new CanonicalPlayerState(
                canonicalPlayerId.strip(),
                Set.of(),
                Map.of(),
                Set.of(),
                Set.of(),
                0,
                0,
                null,
                "",
                0L
        );
    }
}

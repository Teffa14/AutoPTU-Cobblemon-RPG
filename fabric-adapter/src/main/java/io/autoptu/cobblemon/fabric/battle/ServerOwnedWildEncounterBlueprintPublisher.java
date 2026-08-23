package io.autoptu.cobblemon.fabric.battle;

import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.minecraft.server.MinecraftServer;

import java.util.Objects;
import java.util.Optional;

/**
 * Copies an already-decided RPG/campaign WILD encounter blueprint into the active world's
 * create-only blueprint registry before any Cobblemon identity is attached.
 *
 * The trusted source is queried by canonical encounter ID only. Cobblemon actor/Pokemon UUIDs,
 * entities and battle objects are intentionally absent from this boundary, so presentation state
 * cannot influence species, level, stats, HP, moves, abilities, items or other PTU values.
 */
public final class ServerOwnedWildEncounterBlueprintPublisher {
    private final CanonicalWildEncounterBlueprintSource campaignSource;
    private final WorldScopedCanonicalWildEncounterBlueprintRegistry worldRegistry;

    public ServerOwnedWildEncounterBlueprintPublisher(
            CanonicalWildEncounterBlueprintSource campaignSource,
            WorldScopedCanonicalWildEncounterBlueprintRegistry worldRegistry
    ) {
        this.campaignSource = Objects.requireNonNull(campaignSource, "campaignSource");
        this.worldRegistry = Objects.requireNonNull(worldRegistry, "worldRegistry");
    }

    /** Production composition for the active Fabric world. */
    public static ServerOwnedWildEncounterBlueprintPublisher fromWorldRuntime(
            MinecraftServer server,
            CanonicalWildEncounterBlueprintSource campaignSource
    ) {
        Objects.requireNonNull(server, "server");
        return new ServerOwnedWildEncounterBlueprintPublisher(
                Objects.requireNonNull(campaignSource, "campaignSource"),
                FabricCanonicalPlayerStoreRuntime.requireWildEncounterBlueprintRegistry(server)
        );
    }

    /**
     * Publishes one canonical encounter exactly once. Missing or confused-deputy source responses
     * fail closed. Duplicate publication is rejected by the world registry rather than overwriting
     * the first authoritative decision.
     */
    public boolean publish(String canonicalEncounterId) {
        String encounterId = requireId(canonicalEncounterId);
        Optional<CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint> resolved =
                campaignSource.resolve(encounterId);
        if (resolved.isEmpty()) return false;

        CanonicalWildEncounterBlueprintSource.CanonicalWildEncounterBlueprint blueprint = resolved.get();
        if (!encounterId.equals(blueprint.canonicalEncounterId())) {
            throw new IllegalStateException("campaign source returned a different canonical encounter id");
        }

        worldRegistry.register(blueprint);
        return true;
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("canonicalEncounterId is required");
        }
        return value.strip();
    }
}

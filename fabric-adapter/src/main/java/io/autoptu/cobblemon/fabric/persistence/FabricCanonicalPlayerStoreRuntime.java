package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.FileCanonicalItemReservationRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPlayerEncounterProfileRepository;
import io.autoptu.cobblemon.authority.FileCanonicalPokemonRepository;
import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import io.autoptu.cobblemon.authority.FileWorldTaskCraftAttemptRepository;
import io.autoptu.cobblemon.fabric.battle.WorldScopedCanonicalWildEncounterBlueprintRegistry;
import io.autoptu.cobblemon.fabric.battle.WorldScopedWildEncounterCorrelationRegistry;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns world-scoped canonical state for each live Minecraft server instance.
 *
 * The filesystem location is derived from the server's world save root. Minecraft supplies
 * storage location and lifecycle only; it does not supply canonical Trainer, Pokemon, item,
 * arena or WILD encounter values. WILD blueprint and actor-correlation registries are intentionally
 * lifecycle-scoped in memory; durable encounter recovery remains a separate future contract.
 */
public final class FabricCanonicalPlayerStoreRuntime {
    private record Stores(
            FileVersionedCanonicalStateRepository players,
            FileCanonicalPlayerEncounterProfileRepository encounterProfiles,
            FileCanonicalPokemonRepository pokemon,
            FileCanonicalItemReservationRepository assets,
            FileWorldTaskCraftAttemptRepository craftAttempts,
            WorldScopedCanonicalWildEncounterBlueprintRegistry wildEncounterBlueprints,
            WorldScopedWildEncounterCorrelationRegistry wildEncounterCorrelations
    ) {}

    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<MinecraftServer, Stores> STORES = new IdentityHashMap<>();

    private FabricCanonicalPlayerStoreRuntime() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricCanonicalPlayerStoreRuntime::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(FabricCanonicalPlayerStoreRuntime::stop);
    }

    public static FileVersionedCanonicalStateRepository requireRepository(MinecraftServer server) {
        return requireStores(server).players();
    }

    public static FileCanonicalPlayerEncounterProfileRepository requireEncounterProfileRepository(
            MinecraftServer server
    ) {
        return requireStores(server).encounterProfiles();
    }

    public static FileCanonicalPokemonRepository requirePokemonRepository(MinecraftServer server) {
        return requireStores(server).pokemon();
    }

    public static FileCanonicalItemReservationRepository requireAssetRepository(MinecraftServer server) {
        return requireStores(server).assets();
    }

    public static FileWorldTaskCraftAttemptRepository requireCraftAttemptRepository(MinecraftServer server) {
        return requireStores(server).craftAttempts();
    }

    public static WorldScopedCanonicalWildEncounterBlueprintRegistry requireWildEncounterBlueprintRegistry(
            MinecraftServer server
    ) {
        return requireStores(server).wildEncounterBlueprints();
    }

    public static WorldScopedWildEncounterCorrelationRegistry requireWildEncounterCorrelationRegistry(
            MinecraftServer server
    ) {
        return requireStores(server).wildEncounterCorrelations();
    }

    static Path storageRoot(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return storageRoot(server.getSavePath(WorldSavePath.ROOT));
    }

    static Path storageRoot(Path worldSaveRoot) {
        Objects.requireNonNull(worldSaveRoot, "worldSaveRoot");
        return worldSaveRoot.resolve("autoptu").resolve("canonical-state").normalize();
    }

    private static Stores requireStores(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (STORES) {
            Stores stores = STORES.get(server);
            if (stores == null) {
                throw new IllegalStateException("canonical stores are unavailable for this server lifecycle");
            }
            return stores;
        }
    }

    private static void start(MinecraftServer server) {
        Path root = storageRoot(server);
        FileCanonicalPokemonRepository pokemon = new FileCanonicalPokemonRepository(root);
        Stores stores = new Stores(
                new FileVersionedCanonicalStateRepository(root),
                new FileCanonicalPlayerEncounterProfileRepository(root),
                pokemon,
                new FileCanonicalItemReservationRepository(root, pokemon::findPokemon),
                new FileWorldTaskCraftAttemptRepository(root),
                new WorldScopedCanonicalWildEncounterBlueprintRegistry(),
                new WorldScopedWildEncounterCorrelationRegistry()
        );
        synchronized (STORES) {
            if (STORES.putIfAbsent(server, stores) != null) {
                throw new IllegalStateException("canonical stores already initialized for server");
            }
        }
    }

    private static void stop(MinecraftServer server) {
        synchronized (STORES) {
            STORES.remove(server);
        }
    }
}
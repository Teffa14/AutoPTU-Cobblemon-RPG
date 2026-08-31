package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.CanonicalNurseryCustodyService;
import io.autoptu.cobblemon.authority.FileCanonicalNurseryRepository;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** World-scoped nursery repository and restart reconciliation. */
public final class FabricNurseryStoreRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<MinecraftServer, FileCanonicalNurseryRepository> STORES = new IdentityHashMap<>();

    private FabricNurseryStoreRuntime() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricNurseryStoreRuntime::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (STORES) { STORES.remove(server); }
        });
    }

    public static FileCanonicalNurseryRepository requireRepository(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (STORES) {
            FileCanonicalNurseryRepository repository = STORES.get(server);
            if (repository == null) throw new IllegalStateException("nursery store is unavailable for this server lifecycle");
            return repository;
        }
    }

    private static void start(MinecraftServer server) {
        Path root = server.getSavePath(WorldSavePath.ROOT).resolve("autoptu").resolve("canonical-state").normalize();
        FileCanonicalNurseryRepository repository = new FileCanonicalNurseryRepository(root);
        synchronized (STORES) {
            if (STORES.putIfAbsent(server, repository) != null) throw new IllegalStateException("nursery store already initialized");
        }
        new CanonicalNurseryCustodyService(
                repository,
                FabricCanonicalPlayerStoreRuntime.requirePokemonStorageRepository(server),
                FabricCanonicalPlayerStoreRuntime.requirePokemonRepository(server)
        ).recoverCustody();
    }
}
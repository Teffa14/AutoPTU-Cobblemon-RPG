package io.autoptu.cobblemon.fabric.persistence;

import io.autoptu.cobblemon.authority.FileVersionedCanonicalStateRepository;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.nio.file.Path;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Owns the durable canonical-player repository for each live Minecraft server instance.
 *
 * The filesystem location is derived from the server's world save root. Minecraft supplies
 * storage location and lifecycle only; it does not supply canonical Trainer values.
 */
public final class FabricCanonicalPlayerStoreRuntime {
    private static final AtomicBoolean REGISTERED = new AtomicBoolean();
    private static final Map<MinecraftServer, FileVersionedCanonicalStateRepository> REPOSITORIES =
            new IdentityHashMap<>();

    private FabricCanonicalPlayerStoreRuntime() {}

    public static void register() {
        if (!REGISTERED.compareAndSet(false, true)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricCanonicalPlayerStoreRuntime::start);
        ServerLifecycleEvents.SERVER_STOPPED.register(FabricCanonicalPlayerStoreRuntime::stop);
    }

    public static FileVersionedCanonicalStateRepository requireRepository(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        synchronized (REPOSITORIES) {
            FileVersionedCanonicalStateRepository repository = REPOSITORIES.get(server);
            if (repository == null) {
                throw new IllegalStateException("canonical player store is unavailable for this server lifecycle");
            }
            return repository;
        }
    }

    static Path storageRoot(MinecraftServer server) {
        Objects.requireNonNull(server, "server");
        return storageRoot(server.getSavePath(WorldSavePath.ROOT));
    }

    static Path storageRoot(Path worldSaveRoot) {
        Objects.requireNonNull(worldSaveRoot, "worldSaveRoot");
        return worldSaveRoot.resolve("autoptu").resolve("canonical-state").normalize();
    }

    private static void start(MinecraftServer server) {
        Path root = storageRoot(server);
        FileVersionedCanonicalStateRepository repository = new FileVersionedCanonicalStateRepository(root);
        synchronized (REPOSITORIES) {
            if (REPOSITORIES.putIfAbsent(server, repository) != null) {
                throw new IllegalStateException("canonical player store already initialized for server");
            }
        }
    }

    private static void stop(MinecraftServer server) {
        synchronized (REPOSITORIES) {
            REPOSITORIES.remove(server);
        }
    }
}

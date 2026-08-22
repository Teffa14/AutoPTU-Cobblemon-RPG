package io.autoptu.cobblemon.fabric.battle;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Headless production-runtime proof for the Minecraft authentication boundary.
 *
 * Dedicated-server CI has no authenticated client. This smoke therefore proves the fail-closed
 * side of the real MinecraftServer player lookup: an offline UUID must not reach canonical PTU
 * state resolution. The successful authenticated path remains covered by the resolver contract
 * test until CI owns a graphical/logged-in client fixture.
 */
public final class FabricAuthenticatedPlayerContextResolverSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveAuthenticatedPlayerContextSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live authenticated player context smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final UUID OFFLINE_FIXTURE_UUID = UUID.fromString("0dc68ed1-50de-4f20-8c17-f6196115cfde");

    private FabricAuthenticatedPlayerContextResolverSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricAuthenticatedPlayerContextResolverSmoke::run);
    }

    private static void run(MinecraftServer server) {
        AtomicInteger canonicalLookups = new AtomicInteger();
        FabricAuthenticatedPlayerContextResolver resolver = new FabricAuthenticatedPlayerContextResolver(
                server,
                uuid -> {
                    canonicalLookups.incrementAndGet();
                    return Optional.empty();
                }
        );

        if (server.getPlayerManager().getPlayer(OFFLINE_FIXTURE_UUID) != null) {
            throw new IllegalStateException("authenticated-player smoke fixture unexpectedly matches an online player");
        }
        if (resolver.resolve(OFFLINE_FIXTURE_UUID.toString()).isPresent()) {
            throw new IllegalStateException("offline player UUID resolved an authenticated encounter context");
        }
        if (canonicalLookups.get() != 0) {
            throw new IllegalStateException("offline player UUID reached canonical PTU context resolution");
        }
        if (resolver.resolve("not-a-player-uuid").isPresent()) {
            throw new IllegalStateException("malformed player identity resolved an authenticated encounter context");
        }

        LOGGER.info(SUCCESS_LOG);
    }
}

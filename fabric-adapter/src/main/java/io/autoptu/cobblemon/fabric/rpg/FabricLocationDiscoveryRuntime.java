package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalLocationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalLocationDiscoveryService;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerProvisioning;
import io.autoptu.cobblemon.fabric.persistence.FabricCanonicalPlayerStoreRuntime;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Observes authenticated players entering server-authored location footprints and persists discovery.
 * Player coordinates, location identity and progression truth never come from a client payload.
 */
public final class FabricLocationDiscoveryRuntime {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final Map<MinecraftServer, Integer> TICKS = new IdentityHashMap<>();

    private FabricLocationDiscoveryRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!shouldCheck(server)) return;
            var service = new CanonicalLocationDiscoveryService(
                    CanonicalLocationCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireRepository(server),
                    FabricCanonicalPlayerStoreRuntime.requireLocationDiscoveryRepository(server)
            );
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                observePlayer(player, service);
            }
        });
    }

    private static boolean shouldCheck(MinecraftServer server) {
        synchronized (TICKS) {
            int next = TICKS.getOrDefault(server, 0) + 1;
            if (next < CHECK_INTERVAL_TICKS) {
                TICKS.put(server, next);
                return false;
            }
            TICKS.put(server, 0);
            return true;
        }
    }

    private static void observePlayer(ServerPlayerEntity player, CanonicalLocationDiscoveryService service) {
        ServerWorld world = player.getServerWorld();
        String dimensionId = world.getRegistryKey().getValue().toString();
        for (var location : CanonicalLocationCatalogue.DEFAULT.locations()) {
            if (!location.dimensionId().equals(dimensionId)) continue;
            BlockPos anchor = world.getSpawnPos();
            if (!withinRadius(player, anchor, location.triggerRadius())) continue;
            String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
            var decision = service.observe(playerId, location.id());
            if (decision.allowed() && decision.newlyDiscovered()) {
                player.sendMessage(Text.literal("Discovered: " + decision.location().displayName()), false);
            }
        }
    }

    static boolean withinRadius(ServerPlayerEntity player, BlockPos anchor, double radius) {
        double centerX = anchor.getX() + 0.5D;
        double centerY = anchor.getY() + 0.5D;
        double centerZ = anchor.getZ() + 0.5D;
        return player.squaredDistanceTo(centerX, centerY, centerZ) <= radius * radius;
    }
}

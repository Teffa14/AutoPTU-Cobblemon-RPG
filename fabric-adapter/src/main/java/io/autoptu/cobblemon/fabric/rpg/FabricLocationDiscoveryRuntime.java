package io.autoptu.cobblemon.fabric.rpg;

import io.autoptu.cobblemon.authority.CanonicalLocationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalLocationDiscoveryService;
import io.autoptu.cobblemon.authority.CanonicalQuestObjectiveCatalogue;
import io.autoptu.cobblemon.authority.CanonicalQuestObjectiveService;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
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
 * Marea Interior uses fixed CanonicalWorldMapCatalogue anchors; legacy locations may still use spawn.
 */
public final class FabricLocationDiscoveryRuntime {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final Map<MinecraftServer, Integer> TICKS = new IdentityHashMap<>();

    private FabricLocationDiscoveryRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!shouldCheck(server)) return;
            var discovery = new CanonicalLocationDiscoveryService(
                    CanonicalLocationCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireRepository(server),
                    FabricCanonicalPlayerStoreRuntime.requireLocationDiscoveryRepository(server)
            );
            var objectives = new CanonicalQuestObjectiveService(
                    CanonicalQuestObjectiveCatalogue.DEFAULT,
                    FabricCanonicalPlayerStoreRuntime.requireQuestJournalRepository(server),
                    FabricCanonicalPlayerStoreRuntime.requireQuestObjectiveRepository(server)
            );
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                observePlayer(player, discovery, objectives);
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

    private static void observePlayer(
            ServerPlayerEntity player,
            CanonicalLocationDiscoveryService discovery,
            CanonicalQuestObjectiveService objectives
    ) {
        ServerWorld world = player.getServerWorld();
        String dimensionId = world.getRegistryKey().getValue().toString();
        String playerId = FabricCanonicalPlayerProvisioning.canonicalPlayerId(player.getUuid());
        for (var location : CanonicalLocationCatalogue.DEFAULT.locations()) {
            if (!location.dimensionId().equals(dimensionId)) continue;
            BlockPos anchor = anchor(world, location.id());
            if (!withinRadius(player, anchor, location.triggerRadius())) continue;

            var decision = discovery.observe(playerId, location.id());
            if (decision.allowed() && decision.newlyDiscovered()) {
                player.sendMessage(Text.literal("Discovered: " + decision.location().displayName()), false);
            }
            observeQuestEvent(player, playerId, location.id(), objectives);
        }
    }

    private static BlockPos anchor(ServerWorld world, String locationId) {
        return CanonicalWorldMapCatalogue.DEFAULT.site(locationId)
                .map(site -> new BlockPos(site.x(), site.y(), site.z()))
                .orElseGet(world::getSpawnPos);
    }

    private static void observeQuestEvent(
            ServerPlayerEntity player,
            String playerId,
            String locationId,
            CanonicalQuestObjectiveService service
    ) {
        var result = service.observe(playerId, "location:" + locationId);
        for (var update : result.updates()) {
            if (!update.newlyCompleted()) continue;
            var progress = update.questProgress();
            player.sendMessage(Text.literal("Quest updated: " + update.objective().description()), false);
            if (progress.complete()) {
                player.sendMessage(Text.literal("Quest objectives complete: return to the quest giver."), false);
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

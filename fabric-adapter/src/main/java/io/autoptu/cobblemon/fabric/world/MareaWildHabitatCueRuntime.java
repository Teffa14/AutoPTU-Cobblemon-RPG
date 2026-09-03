package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Minecraft-visible feedback for entering server-authored Marea wild-population footprints. */
public final class MareaWildHabitatCueRuntime {
    private static final int POLL_INTERVAL_TICKS = MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks();
    private static final Map<MinecraftServer, Map<UUID, String>> LAST_POPULATION_BY_PLAYER = new IdentityHashMap<>();

    private MareaWildHabitatCueRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % POLL_INTERVAL_TICKS != 0) return;
            reconcile(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (LAST_POPULATION_BY_PLAYER) {
                LAST_POPULATION_BY_PLAYER.remove(server);
            }
        });
    }

    static void reconcile(MinecraftServer server) {
        var world = server.getOverworld();
        Map<UUID, String> current = new HashMap<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            String populationId = populationAt(player);
            if (populationId == null) continue;
            current.put(player.getUuid(), populationId);
            String previous = previousPopulation(server, player.getUuid());
            if (!populationId.equals(previous)) sendEntryCue(player, populationId);
        }
        synchronized (LAST_POPULATION_BY_PLAYER) {
            LAST_POPULATION_BY_PLAYER.put(server, current);
        }
    }

    private static String populationAt(ServerPlayerEntity player) {
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var site = CanonicalWorldMapCatalogue.DEFAULT.site(population.siteId())
                    .orElseThrow(() -> new IllegalStateException("missing canonical wild population site: " + population.siteId()));
            double dx = player.getX() - (site.x() + 0.5D);
            double dy = player.getY() - site.y();
            double dz = player.getZ() - (site.z() + 0.5D);
            if (population.presenceFootprint().containsOffset(dx, dy, dz)) return population.populationId();
        }
        return null;
    }

    private static String previousPopulation(MinecraftServer server, UUID playerId) {
        synchronized (LAST_POPULATION_BY_PLAYER) {
            Map<UUID, String> values = LAST_POPULATION_BY_PLAYER.get(server);
            return values == null ? null : values.get(playerId);
        }
    }

    private static void sendEntryCue(ServerPlayerEntity player, String populationId) {
        var population = CanonicalWildPopulationCatalogue.DEFAULT.population(populationId)
                .orElseThrow(() -> new IllegalStateException("missing canonical wild population: " + populationId));
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(population.siteId())
                .orElseThrow(() -> new IllegalStateException("missing canonical wild population site: " + population.siteId()));
        int visibleMembers = CanonicalWildPopulationCatalogue.DEFAULT.members(population).size();
        player.sendMessage(Text.literal("Wild habitat — " + site.displayName() + " · " + visibleMembers + " roaming Pokemon"), true);
    }
}

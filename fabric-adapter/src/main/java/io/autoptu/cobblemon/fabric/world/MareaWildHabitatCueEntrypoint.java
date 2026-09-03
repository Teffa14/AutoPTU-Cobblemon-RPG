package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Minecraft-visible habitat entry feedback backed only by the canonical Ouros wild-population catalogue.
 *
 * <p>This runtime observes server-owned player position and authored habitat footprints. It never
 * selects an encounter, rolls RNG, reads Cobblemon Pokemon gameplay payloads, or supplies PTU facts.</p>
 */
public final class MareaWildHabitatCueEntrypoint implements ModInitializer {
    private static final Map<MinecraftServer, Map<UUID, Set<String>>> INSIDE_POPULATIONS = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % MareaVisibleWildPokemonRuntime.presenceReconcileIntervalTicks() != 0) return;
            reconcile(server.getOverworld());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (INSIDE_POPULATIONS) {
                INSIDE_POPULATIONS.remove(server);
            }
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            online.add(player.getUuid());
            if (player.isSpectator()) {
                remember(world.getServer(), player.getUuid(), Set.of());
                continue;
            }

            Set<String> previous = remembered(world.getServer(), player.getUuid());
            Set<String> current = new HashSet<>();
            for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
                if (!population.siteId().startsWith("ouros.marea.")) continue;
                var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
                if (projectedSiteId.isEmpty()) continue;
                var site = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId.get())
                        .orElseThrow(() -> new IllegalStateException(
                                "missing projected canonical wild population site: " + projectedSiteId.get()));
                if (!containsPlayer(player, population, site)) continue;

                current.add(population.populationId());
                if (!previous.contains(population.populationId())) {
                    announce(player, population, site);
                }
            }
            remember(world.getServer(), player.getUuid(), current);
        }
        forgetOffline(world.getServer(), online);
    }

    private static boolean containsPlayer(
            ServerPlayerEntity player,
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            CanonicalWorldMapCatalogue.Site site
    ) {
        double dx = player.getX() - (site.x() + 0.5D);
        double dy = player.getY() - site.y();
        double dz = player.getZ() - (site.z() + 0.5D);
        return population.presenceFootprint().containsOffset(dx, dy, dz);
    }

    private static void announce(
            ServerPlayerEntity player,
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            CanonicalWorldMapCatalogue.Site site
    ) {
        int visibleMembers = CanonicalWildPopulationCatalogue.DEFAULT.members(population).size();
        player.sendMessage(Text.literal(
                "Wild habitat — " + site.displayName() + " · " + visibleMembers + " roaming Pokemon"
        ), true);
    }

    private static Set<String> remembered(MinecraftServer server, UUID playerId) {
        synchronized (INSIDE_POPULATIONS) {
            Map<UUID, Set<String>> players = INSIDE_POPULATIONS.get(server);
            if (players == null) return Set.of();
            Set<String> populations = players.get(playerId);
            return populations == null ? Set.of() : Set.copyOf(populations);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, Set<String> populations) {
        synchronized (INSIDE_POPULATIONS) {
            Map<UUID, Set<String>> players = INSIDE_POPULATIONS.computeIfAbsent(server, ignored -> new HashMap<>());
            if (populations.isEmpty()) players.remove(playerId);
            else players.put(playerId, Set.copyOf(populations));
            if (players.isEmpty()) INSIDE_POPULATIONS.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        synchronized (INSIDE_POPULATIONS) {
            Map<UUID, Set<String>> players = INSIDE_POPULATIONS.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) INSIDE_POPULATIONS.remove(server);
        }
    }
}

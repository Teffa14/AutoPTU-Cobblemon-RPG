package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
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
 * Player-facing discovery cue for canonical Marea wild-population footprints.
 *
 * <p>The server observes player position against the authored population catalogue. The cue only
 * announces that a visible habitat has become active; it does not choose a Pokemon, create an
 * encounter, roll RNG or provide any PTU battle fact.</p>
 */
public final class MareaWildHabitatCueRuntime {
    private static final int CHECK_INTERVAL_TICKS = 20;
    private static final Map<MinecraftServer, Map<UUID, Set<String>>> INSIDE_POPULATIONS = new IdentityHashMap<>();

    private MareaWildHabitatCueRuntime() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % CHECK_INTERVAL_TICKS != 0) return;
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
                if (!containsPlayer(world, player, population)) continue;
                current.add(population.populationId());
                if (!previous.contains(population.populationId())) announce(player, population);
            }
            remember(world.getServer(), player.getUuid(), current);
        }
        forgetOffline(world.getServer(), online);
    }

    private static boolean containsPlayer(
            ServerWorld world,
            ServerPlayerEntity player,
            CanonicalWildPopulationCatalogue.PopulationDefinition population
    ) {
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(population.siteId())
                .orElseThrow(() -> new IllegalStateException("missing canonical wild population site: " + population.siteId()));
        double dx = player.getX() - (site.x() + 0.5D);
        double dy = player.getY() - site.y();
        double dz = player.getZ() - (site.z() + 0.5D);
        return population.presenceFootprint().containsOffset(dx, dy, dz);
    }

    private static void announce(
            ServerPlayerEntity player,
            CanonicalWildPopulationCatalogue.PopulationDefinition population
    ) {
        String habitat = population.siteId().substring(population.siteId().lastIndexOf('.') + 1)
                .replace('_', ' ');
        player.sendMessage(Text.literal(
                "Wild habitat: " + habitat + " · visible Pokemon nearby: " + population.encounterIds().size()
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

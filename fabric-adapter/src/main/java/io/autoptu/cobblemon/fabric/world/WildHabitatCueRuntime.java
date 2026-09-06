package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Global Minecraft-visible habitat-entry feedback for every registered wild ecology population.
 *
 * Region/species content publishes projected actors through {@link WildEcologyProjectionRegistry}.
 * This runtime derives only presentation habitat presence from those server-authored projections.
 * It never selects encounters, rolls RNG, reads Cobblemon Pokemon gameplay payloads, or supplies
 * PTU species/stats/moves/legality/results.
 */
public final class WildHabitatCueRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 100;
    private static final Map<MinecraftServer, Map<UUID, Set<String>>> INSIDE_POPULATIONS = new IdentityHashMap<>();

    record HabitatCircle(double centerX, double centerZ, int radiusBlocks) {
        HabitatCircle {
            if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) throw new IllegalArgumentException("habitat center must be finite");
            if (radiusBlocks <= 0) throw new IllegalArgumentException("radiusBlocks must be positive");
        }
    }

    record HabitatCue(String populationKey, String displayName, List<HabitatCircle> circles, int visibleActors, int visibleAlphas) {
        HabitatCue {
            if (populationKey == null || populationKey.isBlank()) throw new IllegalArgumentException("populationKey is required");
            if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("displayName is required");
            circles = List.copyOf(circles);
            if (circles.isEmpty()) throw new IllegalArgumentException("at least one habitat circle is required");
            if (visibleActors <= 0) throw new IllegalArgumentException("visibleActors must be positive");
            if (visibleAlphas < 0 || visibleAlphas > visibleActors) throw new IllegalArgumentException("visibleAlphas must be within visible actor count");
        }

        HabitatCue(String populationKey, String displayName, List<HabitatCircle> circles, int visibleActors) {
            this(populationKey, displayName, circles, visibleActors, 0);
        }
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
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

        Map<String, HabitatCue> habitats = habitatCues(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            online.add(player.getUuid());
            if (player.isSpectator()) {
                remember(world.getServer(), player.getUuid(), Set.of());
                continue;
            }

            Set<String> previous = remembered(world.getServer(), player.getUuid());
            Set<String> current = new HashSet<>();
            for (HabitatCue habitat : habitats.values()) {
                if (!containsHorizontal(player.getX(), player.getZ(), habitat)) continue;
                current.add(habitat.populationKey());
                if (!previous.contains(habitat.populationKey())) announce(player, habitat);
            }
            remember(world.getServer(), player.getUuid(), current);
        }
        forgetOffline(world.getServer(), online);
    }

    static Map<String, HabitatCue> habitatCues(ServerWorld world) {
        Map<String, List<HabitatCircle>> circlesByPopulation = new LinkedHashMap<>();
        Map<String, Integer> countsByPopulation = new LinkedHashMap<>();
        Map<String, Integer> alphasByPopulation = new LinkedHashMap<>();
        Map<String, String> labelsByPopulation = new LinkedHashMap<>();
        for (var projection : WildEcologyProjectionRegistry.collect(world)) {
            circlesByPopulation.computeIfAbsent(projection.populationKey(), ignored -> new ArrayList<>())
                    .add(new HabitatCircle(
                            projection.habitatCenterX(),
                            projection.habitatCenterZ(),
                            projection.habitatLeashRadiusBlocks()));
            countsByPopulation.merge(projection.populationKey(), 1, Integer::sum);
            if (projection.socialRole() == WildSocialRole.ALPHA) {
                alphasByPopulation.merge(projection.populationKey(), 1, Integer::sum);
            }
            String previousLabel = labelsByPopulation.putIfAbsent(projection.populationKey(), projection.habitatDisplayName());
            if (previousLabel != null && !previousLabel.equals(projection.habitatDisplayName())) {
                throw new IllegalStateException("inconsistent habitat display name for population: " + projection.populationKey());
            }
        }

        Map<String, HabitatCue> habitats = new LinkedHashMap<>();
        for (var entry : circlesByPopulation.entrySet()) {
            habitats.put(entry.getKey(), new HabitatCue(
                    entry.getKey(),
                    labelsByPopulation.getOrDefault(entry.getKey(), entry.getKey()),
                    entry.getValue(),
                    countsByPopulation.getOrDefault(entry.getKey(), 0),
                    alphasByPopulation.getOrDefault(entry.getKey(), 0)));
        }
        return Map.copyOf(habitats);
    }

    static boolean containsHorizontal(double playerX, double playerZ, HabitatCue habitat) {
        for (HabitatCircle circle : habitat.circles()) {
            double dx = playerX - circle.centerX();
            double dz = playerZ - circle.centerZ();
            double radius = circle.radiusBlocks();
            if (dx * dx + dz * dz <= radius * radius) return true;
        }
        return false;
    }

    static String announcementText(HabitatCue habitat) {
        String herd = habitat.visibleActors() == 1
                ? "1 roaming Pokemon"
                : habitat.visibleActors() + " roaming Pokemon";
        String alpha = habitat.visibleAlphas() == 1
                ? " · Alpha present"
                : habitat.visibleAlphas() > 1 ? " · " + habitat.visibleAlphas() + " authored Alphas present" : "";
        return "Wild habitat — " + habitat.displayName() + " · " + herd + alpha;
    }

    private static void announce(ServerPlayerEntity player, HabitatCue habitat) {
        player.sendMessage(Text.literal(announcementText(habitat)), true);
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

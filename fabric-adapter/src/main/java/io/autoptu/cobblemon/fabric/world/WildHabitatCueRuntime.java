package io.autoptu.cobblemon.fabric.world;

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
import java.util.LinkedHashMap;
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

    record HabitatCue(String populationKey, double centerX, double centerZ, int radiusBlocks, int visibleActors) {
        HabitatCue {
            if (populationKey == null || populationKey.isBlank()) throw new IllegalArgumentException("populationKey is required");
            if (!Double.isFinite(centerX) || !Double.isFinite(centerZ)) throw new IllegalArgumentException("habitat center must be finite");
            if (radiusBlocks <= 0) throw new IllegalArgumentException("radiusBlocks must be positive");
            if (visibleActors <= 0) throw new IllegalArgumentException("visibleActors must be positive");
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
        Map<String, HabitatCue> habitats = new LinkedHashMap<>();
        for (var projection : WildEcologyProjectionRegistry.collect(world)) {
            HabitatCue existing = habitats.get(projection.populationKey());
            if (existing == null) {
                habitats.put(projection.populationKey(), new HabitatCue(
                        projection.populationKey(),
                        projection.habitatCenterX(),
                        projection.habitatCenterZ(),
                        projection.habitatLeashRadiusBlocks(),
                        1));
                continue;
            }
            if (Double.compare(existing.centerX(), projection.habitatCenterX()) != 0
                    || Double.compare(existing.centerZ(), projection.habitatCenterZ()) != 0
                    || existing.radiusBlocks() != projection.habitatLeashRadiusBlocks()) {
                throw new IllegalStateException("inconsistent habitat projection for population: " + projection.populationKey());
            }
            habitats.put(projection.populationKey(), new HabitatCue(
                    existing.populationKey(), existing.centerX(), existing.centerZ(), existing.radiusBlocks(), existing.visibleActors() + 1));
        }
        return Map.copyOf(habitats);
    }

    static boolean containsHorizontal(double playerX, double playerZ, HabitatCue habitat) {
        double dx = playerX - habitat.centerX();
        double dz = playerZ - habitat.centerZ();
        double radius = habitat.radiusBlocks();
        return dx * dx + dz * dz <= radius * radius;
    }

    private static void announce(ServerPlayerEntity player, HabitatCue habitat) {
        player.sendMessage(Text.literal(
                "Wild habitat · " + habitat.populationKey() + " · " + habitat.visibleActors() + " roaming Pokemon"
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

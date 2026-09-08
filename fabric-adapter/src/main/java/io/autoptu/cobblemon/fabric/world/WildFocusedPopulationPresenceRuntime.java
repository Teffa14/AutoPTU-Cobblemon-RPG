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
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Surfaces the visible population around the player's currently focused canonical WILD.
 *
 * <p>The count comes only from server-owned ecology projections that share the focused actor's
 * canonical population key. Cobblemon entities provide presentation identity only. This runtime
 * never derives PTU legality, stats, HP, moves, RNG, statuses, capture, encounter outcomes or
 * battle results.</p>
 */
public final class WildFocusedPopulationPresenceRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final Map<MinecraftServer, Map<UUID, PopulationPresence>> REMEMBERED = new IdentityHashMap<>();

    record PopulationPresence(String populationKey, String speciesDisplayName, int visibleActors) {
        PopulationPresence {
            if (populationKey == null || populationKey.isBlank()) {
                throw new IllegalArgumentException("populationKey is required");
            }
            if (speciesDisplayName == null || speciesDisplayName.isBlank()) {
                throw new IllegalArgumentException("speciesDisplayName is required");
            }
            if (visibleActors <= 0) throw new IllegalArgumentException("visibleActors must be positive");
            populationKey = populationKey.strip();
            speciesDisplayName = speciesDisplayName.strip();
        }
    }

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) reconcile(server.getOverworld());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (REMEMBERED) {
                REMEMBERED.remove(server);
            }
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        MinecraftServer server = world.getServer();
        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            if (player.isSpectator()) {
                remember(server, playerId, null);
                continue;
            }

            WildHabitatCueRuntime.NearbyInteractionSnapshot focused =
                    WildHabitatCueRuntime.nearestInteractionActor(player, projections);
            PopulationPresence current = presenceFor(focused, projections);
            PopulationPresence previous = remembered(server, playerId);
            remember(server, playerId, current);
            if (shouldAnnounce(previous, current)) {
                player.sendMessage(Text.literal(presenceText(previous, current)), true);
            }
        }
        forgetOffline(server, online);
    }

    static PopulationPresence presenceFor(
            WildHabitatCueRuntime.NearbyInteractionSnapshot focused,
            List<WildEcologyProjectionRegistry.ProjectedActor> projections
    ) {
        if (focused == null || projections == null || projections.isEmpty()) return null;

        WildEcologyProjectionRegistry.ProjectedActor focusedProjection = projections.stream()
                .filter(candidate -> candidate != null && !candidate.actor().isRemoved())
                .filter(candidate -> candidate.actor().getUuid().equals(focused.actorId()))
                .findFirst()
                .orElse(null);
        if (focusedProjection == null) return null;

        String populationKey = focusedProjection.populationKey();
        int visibleActors = 0;
        for (WildEcologyProjectionRegistry.ProjectedActor projection : projections) {
            if (projection == null || projection.actor().isRemoved()) continue;
            if (populationKey.equals(projection.populationKey())) visibleActors++;
        }
        if (visibleActors <= 0) return null;

        return new PopulationPresence(
                populationKey,
                WildHabitatCueRuntime.displaySpeciesName(focused.speciesId()),
                visibleActors);
    }

    static boolean shouldAnnounce(PopulationPresence previous, PopulationPresence current) {
        if (current == null) return false;
        if (previous == null) return true;
        if (!current.populationKey().equals(previous.populationKey())) return true;
        return current.visibleActors() != previous.visibleActors();
    }

    static String presenceText(PopulationPresence previous, PopulationPresence current) {
        if (current == null) throw new IllegalArgumentException("current presence is required");
        String count = Integer.toString(current.visibleActors());
        if (previous != null
                && current.populationKey().equals(previous.populationKey())
                && previous.visibleActors() != current.visibleActors()) {
            count = previous.visibleActors() + " → " + current.visibleActors();
        }
        return "Wild population — "
                + current.speciesDisplayName()
                + " · "
                + count
                + " WILD visible";
    }

    private static PopulationPresence remembered(MinecraftServer server, UUID playerId) {
        synchronized (REMEMBERED) {
            Map<UUID, PopulationPresence> players = REMEMBERED.get(server);
            return players == null ? null : players.get(playerId);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, PopulationPresence presence) {
        synchronized (REMEMBERED) {
            Map<UUID, PopulationPresence> players = REMEMBERED.computeIfAbsent(server, ignored -> new HashMap<>());
            if (presence == null) players.remove(playerId);
            else players.put(playerId, presence);
            if (players.isEmpty()) REMEMBERED.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        synchronized (REMEMBERED) {
            Map<UUID, PopulationPresence> players = REMEMBERED.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) REMEMBERED.remove(server);
        }
    }
}

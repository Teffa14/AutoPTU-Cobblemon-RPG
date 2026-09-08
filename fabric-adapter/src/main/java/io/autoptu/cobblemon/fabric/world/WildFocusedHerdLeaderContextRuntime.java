package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Surfaces the authored herd leader relationship for a player's focused canonical WILD member.
 *
 * <p>The relationship is resolved only from same-population ecology projections whose social role
 * is explicitly {@link WildSocialRole#ALPHA}. Minecraft positions are used only to describe visible
 * proximity and horizontal separation in blocks. Cobblemon brain/herd state and Pokemon gameplay
 * payloads are never authority inputs, and this runtime supplies no PTU leadership, targeting,
 * movement, initiative or battle effects.</p>
 */
public final class WildFocusedHerdLeaderContextRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final Map<MinecraftServer, Map<UUID, LeaderContext>> REMEMBERED = new IdentityHashMap<>();

    record LeaderContext(
            String populationKey,
            UUID leaderActorId,
            String speciesDisplayName,
            boolean withinCohesion,
            int horizontalDistanceBlocks
    ) {
        LeaderContext {
            if (populationKey == null || populationKey.isBlank()) throw new IllegalArgumentException("populationKey is required");
            if (leaderActorId == null) throw new IllegalArgumentException("leaderActorId is required");
            if (speciesDisplayName == null || speciesDisplayName.isBlank()) throw new IllegalArgumentException("speciesDisplayName is required");
            if (horizontalDistanceBlocks < 0) throw new IllegalArgumentException("horizontalDistanceBlocks must be non-negative");
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
            LeaderContext current = contextFor(player, projections);
            LeaderContext previous = remembered(server, playerId);
            remember(server, playerId, current);
            if (shouldAnnounce(previous, current)) {
                player.sendMessage(Text.literal(contextText(current)), true);
            }
        }
        forgetOffline(server, online);
    }

    static LeaderContext contextFor(
            ServerPlayerEntity player,
            List<WildEcologyProjectionRegistry.ProjectedActor> projections
    ) {
        if (player == null || projections == null || projections.isEmpty()) return null;
        var focused = WildHabitatCueRuntime.nearestInteractionActor(player, projections);
        if (focused == null || focused.socialRole() == WildSocialRole.ALPHA) return null;

        var member = projections.stream()
                .filter(candidate -> candidate != null && !candidate.actor().isRemoved())
                .filter(candidate -> candidate.actor().getUuid().equals(focused.actorId()))
                .findFirst()
                .orElse(null);
        if (member == null) return null;

        var alpha = projections.stream()
                .filter(candidate -> candidate != null)
                .filter(candidate -> candidate.socialRole() == WildSocialRole.ALPHA)
                .filter(candidate -> member.populationKey().equals(candidate.populationKey()))
                .filter(candidate -> !candidate.actor().isRemoved() && !candidate.actor().isInvisible())
                .filter(candidate -> VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid()))
                .min(Comparator
                        .comparingDouble((WildEcologyProjectionRegistry.ProjectedActor candidate) ->
                                member.actor().squaredDistanceTo(candidate.actor()))
                        .thenComparing(candidate -> candidate.actor().getUuid()))
                .orElse(null);
        if (alpha == null) return null;

        double dx = alpha.actor().getX() - member.actor().getX();
        double dz = alpha.actor().getZ() - member.actor().getZ();
        double horizontalDistanceSquared = dx * dx + dz * dz;
        double cohesion = member.behaviorProfile().cohesionDistance();
        boolean withinCohesion = horizontalDistanceSquared <= cohesion * cohesion;
        int distanceBlocks = roundedHorizontalDistanceBlocks(horizontalDistanceSquared);
        return new LeaderContext(
                member.populationKey(),
                alpha.actor().getUuid(),
                WildHabitatCueRuntime.displaySpeciesName(focused.speciesId()),
                withinCohesion,
                distanceBlocks);
    }

    static int roundedHorizontalDistanceBlocks(double horizontalDistanceSquared) {
        if (!Double.isFinite(horizontalDistanceSquared) || horizontalDistanceSquared < 0.0D) {
            throw new IllegalArgumentException("horizontalDistanceSquared must be finite and non-negative");
        }
        double distance = Math.sqrt(horizontalDistanceSquared);
        if (distance >= Integer.MAX_VALUE) return Integer.MAX_VALUE;
        return (int) Math.round(distance);
    }

    static boolean shouldAnnounce(LeaderContext previous, LeaderContext current) {
        if (current == null) return false;
        return previous == null
                || !current.populationKey().equals(previous.populationKey())
                || !current.leaderActorId().equals(previous.leaderActorId())
                || current.withinCohesion() != previous.withinCohesion()
                || current.horizontalDistanceBlocks() != previous.horizontalDistanceBlocks();
    }

    static String contextText(LeaderContext context) {
        if (context == null) throw new IllegalArgumentException("context is required");
        return "Herd leader — Alpha " + context.speciesDisplayName()
                + (context.withinCohesion() ? " · nearby" : " · regrouping distance")
                + " · " + context.horizontalDistanceBlocks() + " blocks";
    }

    private static LeaderContext remembered(MinecraftServer server, UUID playerId) {
        synchronized (REMEMBERED) {
            Map<UUID, LeaderContext> players = REMEMBERED.get(server);
            return players == null ? null : players.get(playerId);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, LeaderContext context) {
        synchronized (REMEMBERED) {
            Map<UUID, LeaderContext> players = REMEMBERED.computeIfAbsent(server, ignored -> new HashMap<>());
            if (context == null) players.remove(playerId);
            else players.put(playerId, context);
            if (players.isEmpty()) REMEMBERED.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        synchronized (REMEMBERED) {
            Map<UUID, LeaderContext> players = REMEMBERED.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) REMEMBERED.remove(server);
        }
    }
}

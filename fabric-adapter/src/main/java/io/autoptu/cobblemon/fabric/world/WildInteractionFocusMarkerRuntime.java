package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.particle.SimpleParticleType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Briefly marks the exact visible WILD actor that the server currently resolves as the player's
 * interaction focus. The marker is presentation only: target selection reuses the same canonical
 * projection/binding/range boundary as the encounter cue and never reads Cobblemon Pokemon state.
 */
public final class WildInteractionFocusMarkerRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final int PARTICLE_COUNT = 5;
    private static final double MARKER_HEIGHT_OFFSET = 0.35D;
    private static final Map<MinecraftServer, Map<UUID, UUID>> FOCUSED_ACTORS = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS == 0) reconcile(server.getOverworld());
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (FOCUSED_ACTORS) {
                FOCUSED_ACTORS.remove(server);
            }
        });
    }

    static void reconcile(ServerWorld world) {
        if (world == null || world.getServer() == null || world != world.getServer().getOverworld()) return;

        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            if (player.isSpectator()) {
                remember(world.getServer(), playerId, null);
                continue;
            }

            WildHabitatCueRuntime.NearbyInteractionSnapshot focus =
                    WildHabitatCueRuntime.nearestInteractionActor(player, projections);
            UUID currentActorId = focus == null ? null : focus.actorId();
            UUID previousActorId = remembered(world.getServer(), playerId);
            remember(world.getServer(), playerId, currentActorId);
            if (!shouldMark(previousActorId, currentActorId)) continue;

            WildEcologyProjectionRegistry.ProjectedActor projection = projectionFor(currentActorId, projections);
            if (projection == null || projection.actor().isRemoved()) continue;
            mark(world, projection);
        }
        forgetOffline(world.getServer(), online);
    }

    static boolean shouldMark(UUID previousActorId, UUID currentActorId) {
        return currentActorId != null && !currentActorId.equals(previousActorId);
    }

    static WildEcologyProjectionRegistry.ProjectedActor projectionFor(
            UUID actorId,
            List<WildEcologyProjectionRegistry.ProjectedActor> projections) {
        if (actorId == null || projections == null || projections.isEmpty()) return null;
        for (WildEcologyProjectionRegistry.ProjectedActor projection : projections) {
            if (projection != null && actorId.equals(projection.actor().getUuid())) return projection;
        }
        return null;
    }

    static SimpleParticleType markerParticle(WildSocialRole socialRole) {
        if (socialRole == null) throw new IllegalArgumentException("socialRole is required");
        return socialRole == WildSocialRole.ALPHA ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD;
    }

    private static void mark(ServerWorld world, WildEcologyProjectionRegistry.ProjectedActor projection) {
        var actor = projection.actor();
        world.spawnParticles(
                markerParticle(projection.socialRole()),
                actor.getX(),
                actor.getBoundingBox().maxY + MARKER_HEIGHT_OFFSET,
                actor.getZ(),
                PARTICLE_COUNT,
                0.22D,
                0.12D,
                0.22D,
                0.01D);
    }

    private static UUID remembered(MinecraftServer server, UUID playerId) {
        synchronized (FOCUSED_ACTORS) {
            Map<UUID, UUID> players = FOCUSED_ACTORS.get(server);
            return players == null ? null : players.get(playerId);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, UUID actorId) {
        synchronized (FOCUSED_ACTORS) {
            Map<UUID, UUID> players = FOCUSED_ACTORS.computeIfAbsent(server, ignored -> new HashMap<>());
            if (actorId == null) players.remove(playerId);
            else players.put(playerId, actorId);
            if (players.isEmpty()) FOCUSED_ACTORS.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        synchronized (FOCUSED_ACTORS) {
            Map<UUID, UUID> players = FOCUSED_ACTORS.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) FOCUSED_ACTORS.remove(server);
        }
    }
}

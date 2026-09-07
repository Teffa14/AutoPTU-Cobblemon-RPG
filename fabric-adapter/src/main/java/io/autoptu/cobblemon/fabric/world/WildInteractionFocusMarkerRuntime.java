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
 * Marks the exact visible WILD actor that the server currently resolves as the player's interaction
 * focus. The marker is presentation only: target selection reuses the same canonical
 * projection/binding/range boundary as the encounter cue and never reads Cobblemon Pokemon state.
 * A quiet repeat pulse keeps the selected actor identifiable in dense groups without changing focus.
 */
public final class WildInteractionFocusMarkerRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    static final int REPEAT_MARKER_INTERVAL_TICKS = 40;
    private static final int PARTICLE_COUNT = 5;
    private static final double MARKER_HEIGHT_OFFSET = 0.35D;
    private static final Map<MinecraftServer, Map<UUID, FocusMarkerState>> FOCUSED_ACTORS = new IdentityHashMap<>();

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

        MinecraftServer server = world.getServer();
        long currentTick = server.getTicks();
        List<WildEcologyProjectionRegistry.ProjectedActor> projections = WildEcologyProjectionRegistry.collect(world);
        Set<UUID> online = new HashSet<>();
        for (ServerPlayerEntity player : world.getPlayers()) {
            UUID playerId = player.getUuid();
            online.add(playerId);
            if (player.isSpectator()) {
                remember(server, playerId, null);
                continue;
            }

            WildHabitatCueRuntime.NearbyInteractionSnapshot focus =
                    WildHabitatCueRuntime.nearestInteractionActor(player, projections);
            UUID currentActorId = focus == null ? null : focus.actorId();
            FocusMarkerState previous = remembered(server, playerId);
            boolean markNow = shouldMark(previous, currentActorId, currentTick);
            remember(server, playerId, nextState(previous, currentActorId, currentTick, markNow));
            if (!markNow) continue;

            WildEcologyProjectionRegistry.ProjectedActor projection = projectionFor(currentActorId, projections);
            if (projection == null || projection.actor().isRemoved()) continue;
            mark(world, projection);
        }
        forgetOffline(server, online);
    }

    static boolean shouldMark(FocusMarkerState previous, UUID currentActorId, long currentTick) {
        if (currentActorId == null) return false;
        if (previous == null || !currentActorId.equals(previous.actorId())) return true;
        return currentTick - previous.lastMarkerTick() >= REPEAT_MARKER_INTERVAL_TICKS;
    }

    static FocusMarkerState nextState(
            FocusMarkerState previous,
            UUID currentActorId,
            long currentTick,
            boolean markedNow) {
        if (currentActorId == null) return null;
        if (markedNow || previous == null || !currentActorId.equals(previous.actorId())) {
            return new FocusMarkerState(currentActorId, currentTick);
        }
        return previous;
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

    static MarkerStyle markerStyle(WildSocialRole socialRole) {
        if (socialRole == null) throw new IllegalArgumentException("socialRole is required");
        return socialRole == WildSocialRole.ALPHA ? MarkerStyle.ALPHA : MarkerStyle.MEMBER;
    }

    private static SimpleParticleType markerParticle(MarkerStyle markerStyle) {
        return markerStyle == MarkerStyle.ALPHA ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.END_ROD;
    }

    private static void mark(ServerWorld world, WildEcologyProjectionRegistry.ProjectedActor projection) {
        var actor = projection.actor();
        world.spawnParticles(
                markerParticle(markerStyle(projection.socialRole())),
                actor.getX(),
                actor.getBoundingBox().maxY + MARKER_HEIGHT_OFFSET,
                actor.getZ(),
                PARTICLE_COUNT,
                0.22D,
                0.12D,
                0.22D,
                0.01D);
    }

    private static FocusMarkerState remembered(MinecraftServer server, UUID playerId) {
        synchronized (FOCUSED_ACTORS) {
            Map<UUID, FocusMarkerState> players = FOCUSED_ACTORS.get(server);
            return players == null ? null : players.get(playerId);
        }
    }

    private static void remember(MinecraftServer server, UUID playerId, FocusMarkerState state) {
        synchronized (FOCUSED_ACTORS) {
            Map<UUID, FocusMarkerState> players = FOCUSED_ACTORS.computeIfAbsent(server, ignored -> new HashMap<>());
            if (state == null) players.remove(playerId);
            else players.put(playerId, state);
            if (players.isEmpty()) FOCUSED_ACTORS.remove(server);
        }
    }

    private static void forgetOffline(MinecraftServer server, Set<UUID> online) {
        synchronized (FOCUSED_ACTORS) {
            Map<UUID, FocusMarkerState> players = FOCUSED_ACTORS.get(server);
            if (players == null) return;
            players.keySet().removeIf(playerId -> !online.contains(playerId));
            if (players.isEmpty()) FOCUSED_ACTORS.remove(server);
        }
    }

    record FocusMarkerState(UUID actorId, long lastMarkerTick) {
        FocusMarkerState {
            if (actorId == null) throw new IllegalArgumentException("actorId is required");
            if (lastMarkerTick < 0L) throw new IllegalArgumentException("lastMarkerTick must not be negative");
        }
    }

    enum MarkerStyle {
        MEMBER,
        ALPHA
    }
}

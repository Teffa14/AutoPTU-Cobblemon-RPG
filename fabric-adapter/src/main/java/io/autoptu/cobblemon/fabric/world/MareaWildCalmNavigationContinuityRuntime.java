package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Keeps an already-started Minecraft-native CALM route visually continuous across the 10-tick
 * ambient update cadence.
 *
 * The ambient controller intentionally revokes native navigation before it re-evaluates authored
 * behavior precedence. This runtime observes only routes that were already active during a CALM
 * wander segment and may rehydrate the same deterministic authored destination after that update.
 * A nearby non-spectator player, the CALM rest window, a segment change, actor deactivation, leash
 * escape or path failure clears continuity immediately.
 *
 * This is Minecraft world presentation only. It never reads Cobblemon Pokemon gameplay state and
 * never decides PTU movement legality, targets, RNG, damage, statuses, initiative or outcomes.
 */
public final class MareaWildCalmNavigationContinuityRuntime implements ModInitializer {
    private static final int AMBIENT_UPDATE_INTERVAL_TICKS = 10;
    private static final long CALM_WANDER_SEGMENT_TICKS = 80L;
    private static final double PLAYER_INVALIDATION_DISTANCE = 16.0D;
    private static final double TARGET_STOP_DISTANCE = 1.0D;
    private static final double NATIVE_NAVIGATION_SPEED = 0.08D;
    private static final Map<MinecraftServer, Map<UUID, Long>> ACTIVE_SEGMENTS = new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(this::update);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (ACTIVE_SEGMENTS) {
                ACTIVE_SEGMENTS.remove(server);
            }
        });
    }

    private void update(MinecraftServer server) {
        if (server == null) return;
        ServerWorld world = server.getOverworld();
        long worldTime = world.getTime();
        long currentSegment = calmSegment(worldTime);
        Map<UUID, Long> active = activeFor(server);
        HashSet<UUID> liveActors = new HashSet<>();

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, worldTime);
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

                liveActors.add(actor.getUuid());
                boolean nearbyPlayer = hasNearbyPlayer(world, actor);
                boolean calmActive = MareaWildAmbientBehaviorRuntime.calmWanderActive(worldTime);
                if (!calmActive || nearbyPlayer) {
                    active.remove(actor.getUuid());
                    continue;
                }

                var navigation = actor.getNavigation();
                if (!navigation.isIdle()) {
                    active.put(actor.getUuid(), currentSegment);
                    continue;
                }

                Long recordedSegment = active.get(actor.getUuid());
                if (!shouldRehydrate(recordedSegment, currentSegment, calmActive, nearbyPlayer)) {
                    active.remove(actor.getUuid());
                    continue;
                }
                if (server.getTicks() % AMBIENT_UPDATE_INTERVAL_TICKS != 0) continue;

                BlockPos anchor = MareaVisibleWildPokemonRuntime.projectedPresentationAnchor(
                        encounter,
                        projectedSiteId.get());
                double centerX = anchor.getX() + 0.5D;
                double centerZ = anchor.getZ() + 0.5D;
                if (!MareaWildCalmCollisionSteeringRuntime.navigationTargetInsideLeash(
                        centerX, centerZ, population.habitatLeashRadiusBlocks(), actor.getX(), actor.getZ())) {
                    active.remove(actor.getUuid());
                    continue;
                }

                double[] target = MareaWildAmbientBehaviorRuntime.calmRoamingTarget(
                        actor.getUuid(), worldTime, centerX, centerZ, population.habitatLeashRadiusBlocks());
                double dx = target[0] - actor.getX();
                double dz = target[1] - actor.getZ();
                if (dx * dx + dz * dz <= TARGET_STOP_DISTANCE * TARGET_STOP_DISTANCE) {
                    active.remove(actor.getUuid());
                    continue;
                }

                Path path = MareaWildCalmCollisionSteeringRuntime.findLeashSafeNativePath(
                        actor,
                        centerX,
                        centerZ,
                        population.habitatLeashRadiusBlocks(),
                        target);
                if (path == null || !navigation.startMovingAlong(path, NATIVE_NAVIGATION_SPEED)) {
                    active.remove(actor.getUuid());
                }
            }
        }

        active.keySet().removeIf(uuid -> !liveActors.contains(uuid));
    }

    static long calmSegment(long worldTime) {
        return Math.floorDiv(worldTime, CALM_WANDER_SEGMENT_TICKS);
    }

    static boolean shouldRehydrate(
            Long recordedSegment,
            long currentSegment,
            boolean calmActive,
            boolean nearbyPlayer
    ) {
        return recordedSegment != null
                && recordedSegment == currentSegment
                && calmActive
                && !nearbyPlayer;
    }

    private static boolean hasNearbyPlayer(ServerWorld world, PokemonEntity actor) {
        double limitSquared = PLAYER_INVALIDATION_DISTANCE * PLAYER_INVALIDATION_DISTANCE;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            if (actor.squaredDistanceTo(player) <= limitSquared) return true;
        }
        return false;
    }

    private static Map<UUID, Long> activeFor(MinecraftServer server) {
        synchronized (ACTIVE_SEGMENTS) {
            return ACTIVE_SEGMENTS.computeIfAbsent(server, ignored -> new HashMap<>());
        }
    }
}

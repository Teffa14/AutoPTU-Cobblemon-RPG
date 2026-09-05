package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Global Minecraft-presentation reciprocal yield for visible wild ecology actors.
 *
 * Every actor comes from {@link WildEcologyProjectionRegistry}; region/species code only publishes
 * actor, habitat and behavior-profile data. This runtime owns short-lived movement intents and
 * deterministic yield leases for all registered populations. Canonical encounter identity is used
 * only as a stable presentation tie-breaker. No PTU movement, initiative, targeting, legality, RNG,
 * damage or battle result is inferred here, and no Cobblemon Pokemon gameplay payload is trusted.
 */
public final class WildCalmReciprocalYieldRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final int MAX_YIELD_HOLD_TICKS = 40;
    private static final int INTENT_TTL_TICKS = 12;
    private static final int PATH_INTENT_NODE_LOOKAHEAD = 5;
    private static final double PATH_INTENT_MAX_DISTANCE = 5.0D;
    private static final double MIN_HORIZONTAL_SPEED = 0.000001D;
    private static final double RECIPROCAL_PROBE_DISTANCE = 0.9D;
    private static final double INTENT_CORRIDOR_DISTANCE = 2.4D;

    private static final Map<UUID, YieldLease> YIELD_LEASES = new HashMap<>();
    private static final Map<UUID, CorridorIntent> CORRIDOR_INTENTS = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long tick = server.getTicks();
            List<WildEcologyProjectionRegistry.ProjectedActor> projectedActors = world == null
                    ? List.of()
                    : WildEcologyProjectionRegistry.collect(world);
            publishCorridorIntents(projectedActors, tick);
            applyActiveLeases(world, tick);
            if (tick % UPDATE_INTERVAL_TICKS == 0) {
                acquireReciprocalYields(world, projectedActors, tick);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            YIELD_LEASES.clear();
            CORRIDOR_INTENTS.clear();
        });
    }

    private static void publishCorridorIntents(
            List<WildEcologyProjectionRegistry.ProjectedActor> projectedActors,
            long tick
    ) {
        CORRIDOR_INTENTS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() <= tick);
        Map<UUID, Boolean> observed = new HashMap<>();
        for (var projected : projectedActors) {
            PokemonEntity actor = projected.actor();
            observed.put(actor.getUuid(), Boolean.TRUE);
            if (!isActive(actor)) {
                CORRIDOR_INTENTS.remove(actor.getUuid());
                continue;
            }
            var binding = VisibleWildPokemonEncounterRuntime.binding(actor.getUuid());
            if (binding.isEmpty()) {
                CORRIDOR_INTENTS.remove(actor.getUuid());
                continue;
            }

            Vec3d velocity = actor.getVelocity();
            double speed = horizontalSpeed(velocity.x, velocity.z);
            double maxCalmSpeed = projected.behaviorProfile().calmRoamSpeed() + MIN_HORIZONTAL_SPEED;
            if (speed > maxCalmSpeed) {
                CORRIDOR_INTENTS.remove(actor.getUuid());
                continue;
            }

            List<DirectedCorridor> corridors = nativePathCorridors(actor);
            if (corridors.isEmpty()) {
                if (speed <= MIN_HORIZONTAL_SPEED) {
                    CORRIDOR_INTENTS.remove(actor.getUuid());
                    continue;
                }
                corridors = velocityIntentCorridors(
                        actor.getBoundingBox(), velocity.x / speed, velocity.z / speed);
            }
            CORRIDOR_INTENTS.put(
                    actor.getUuid(),
                    new CorridorIntent(
                            binding.get().canonicalEncounterId(),
                            corridors,
                            tick + INTENT_TTL_TICKS));
        }
        CORRIDOR_INTENTS.keySet().removeIf(uuid -> !observed.containsKey(uuid));
    }

    private static void acquireReciprocalYields(
            ServerWorld world,
            List<WildEcologyProjectionRegistry.ProjectedActor> projectedActors,
            long tick
    ) {
        if (world == null) return;
        Map<UUID, WildEcologyProjectionRegistry.ProjectedActor> projectedByUuid = new HashMap<>();
        for (var projected : projectedActors) projectedByUuid.put(projected.actor().getUuid(), projected);

        for (var projected : projectedActors) {
            PokemonEntity actor = projected.actor();
            if (YIELD_LEASES.containsKey(actor.getUuid()) || !isActive(actor)) continue;
            Vec3d velocity = actor.getVelocity();
            double speed = horizontalSpeed(velocity.x, velocity.z);
            double maxCalmSpeed = projected.behaviorProfile().calmRoamSpeed() + MIN_HORIZONTAL_SPEED;
            if (speed > maxCalmSpeed) continue;

            CorridorIntent actorIntent = activeIntent(actor.getUuid(), tick);
            PokemonEntity peer = conflictingIntentPeer(world, actor, projectedByUuid, tick);
            List<DirectedCorridor> leaseCorridors;
            if (peer != null && actorIntent != null) {
                leaseCorridors = actorIntent.corridors();
            } else {
                if (speed <= MIN_HORIZONTAL_SPEED) continue;
                peer = reciprocalPeerAhead(
                        actor,
                        projectedActors,
                        velocity.x,
                        velocity.z,
                        maxCalmSpeed);
                if (peer == null) continue;
                leaseCorridors = velocityIntentCorridors(
                        actor.getBoundingBox(), velocity.x / speed, velocity.z / speed);
            }

            var actorBinding = VisibleWildPokemonEncounterRuntime.binding(actor.getUuid());
            var peerBinding = VisibleWildPokemonEncounterRuntime.binding(peer.getUuid());
            if (actorBinding.isEmpty() || peerBinding.isEmpty()) continue;
            if (!shouldYield(
                    actorBinding.get().canonicalEncounterId(),
                    peerBinding.get().canonicalEncounterId())) continue;

            YIELD_LEASES.put(
                    actor.getUuid(),
                    new YieldLease(peer.getUuid(), leaseCorridors, tick + MAX_YIELD_HOLD_TICKS));
            stopPresentationMotion(actor);
        }
    }

    private static void applyActiveLeases(ServerWorld world, long tick) {
        if (world == null) {
            YIELD_LEASES.clear();
            return;
        }
        var iterator = YIELD_LEASES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var actorEntity = world.getEntity(entry.getKey());
            var peerEntity = world.getEntity(entry.getValue().peerUuid());
            if (!(actorEntity instanceof PokemonEntity actor)
                    || !(peerEntity instanceof PokemonEntity peer)
                    || !isActive(actor)
                    || !isActive(peer)) {
                iterator.remove();
                continue;
            }
            var actorBinding = VisibleWildPokemonEncounterRuntime.binding(actor.getUuid());
            var peerBinding = VisibleWildPokemonEncounterRuntime.binding(peer.getUuid());
            boolean priority = actorBinding.isPresent()
                    && peerBinding.isPresent()
                    && shouldYield(
                            actorBinding.get().canonicalEncounterId(),
                            peerBinding.get().canonicalEncounterId());
            CorridorIntent peerIntent = activeIntent(peer.getUuid(), tick);
            boolean occupied = priority && corridorsOccupiedOrClaimed(
                    entry.getValue().actorCorridors(),
                    peer.getBoundingBox(),
                    peerIntent == null ? List.of() : peerIntent.corridors());
            if (!shouldRetainLease(
                    tick,
                    entry.getValue().expiresAtTick(),
                    true,
                    priority,
                    occupied)) {
                iterator.remove();
                continue;
            }
            stopPresentationMotion(actor);
        }
    }

    private static CorridorIntent activeIntent(UUID actorUuid, long tick) {
        CorridorIntent intent = CORRIDOR_INTENTS.get(actorUuid);
        return intent != null && intent.expiresAtTick() > tick ? intent : null;
    }

    private static PokemonEntity conflictingIntentPeer(
            ServerWorld world,
            PokemonEntity actor,
            Map<UUID, WildEcologyProjectionRegistry.ProjectedActor> projectedByUuid,
            long tick
    ) {
        CorridorIntent actorIntent = activeIntent(actor.getUuid(), tick);
        if (actorIntent == null) return null;
        PokemonEntity selected = null;
        String selectedId = null;
        for (var entry : CORRIDOR_INTENTS.entrySet()) {
            if (entry.getKey().equals(actor.getUuid()) || !projectedByUuid.containsKey(entry.getKey())) continue;
            CorridorIntent peerIntent = activeIntent(entry.getKey(), tick);
            if (peerIntent == null || !corridorsConflict(actorIntent.corridors(), peerIntent.corridors())) continue;
            var entity = world.getEntity(entry.getKey());
            if (!(entity instanceof PokemonEntity peer) || !isActive(peer)) continue;
            var binding = VisibleWildPokemonEncounterRuntime.binding(peer.getUuid());
            if (binding.isEmpty()) continue;
            String canonicalId = requireCanonicalId(binding.get().canonicalEncounterId(), "peerCanonicalEncounterId");
            if (!canonicalId.equals(peerIntent.canonicalEncounterId())) continue;
            if (selectedId == null || canonicalId.compareTo(selectedId) < 0) {
                selectedId = canonicalId;
                selected = peer;
            }
        }
        return selected;
    }

    private static PokemonEntity reciprocalPeerAhead(
            PokemonEntity actor,
            List<WildEcologyProjectionRegistry.ProjectedActor> projectedActors,
            double velocityX,
            double velocityZ,
            double maxCalmSpeed
    ) {
        double speed = horizontalSpeed(velocityX, velocityZ);
        if (speed <= MIN_HORIZONTAL_SPEED) return null;
        PokemonEntity selected = null;
        String selectedId = null;
        for (var projected : projectedActors) {
            PokemonEntity peer = projected.actor();
            if (peer.getUuid().equals(actor.getUuid()) || !isActive(peer)) continue;
            Vec3d peerVelocity = peer.getVelocity();
            double peerSpeed = horizontalSpeed(peerVelocity.x, peerVelocity.z);
            double peerMaxCalmSpeed = projected.behaviorProfile().calmRoamSpeed() + MIN_HORIZONTAL_SPEED;
            if (peerSpeed <= MIN_HORIZONTAL_SPEED || peerSpeed > Math.max(maxCalmSpeed, peerMaxCalmSpeed)) continue;
            if (!reciprocalApproach(
                    actor.getX(), actor.getZ(), velocityX, velocityZ,
                    peer.getX(), peer.getZ(), peerVelocity.x, peerVelocity.z)) continue;
            if (!sweptReciprocalCorridorsConflict(
                    actor.getBoundingBox(), velocityX, velocityZ,
                    peer.getBoundingBox(), peerVelocity.x, peerVelocity.z,
                    RECIPROCAL_PROBE_DISTANCE)) continue;
            var binding = VisibleWildPokemonEncounterRuntime.binding(peer.getUuid());
            if (binding.isEmpty()) continue;
            String canonicalId = requireCanonicalId(binding.get().canonicalEncounterId(), "peerCanonicalEncounterId");
            if (selectedId == null || canonicalId.compareTo(selectedId) < 0) {
                selectedId = canonicalId;
                selected = peer;
            }
        }
        return selected;
    }

    private static boolean isActive(PokemonEntity actor) {
        return actor != null
                && !actor.isRemoved()
                && !actor.isInvisible()
                && VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
    }

    private static List<DirectedCorridor> nativePathCorridors(PokemonEntity actor) {
        Path path = actor.getNavigation().getCurrentPath();
        if (path == null || path.isFinished()) return List.of();
        int start = Math.max(0, path.getCurrentNodeIndex());
        int end = Math.min(path.getLength(), start + PATH_INTENT_NODE_LOOKAHEAD);
        List<Vec3d> nodes = new ArrayList<>(Math.max(0, end - start));
        for (int index = start; index < end; index++) nodes.add(path.getNodePosition(actor, index));
        return pathIntentCorridors(actor.getBoundingBox(), actor.getPos(), nodes, PATH_INTENT_MAX_DISTANCE);
    }

    private static List<DirectedCorridor> velocityIntentCorridors(Box bounds, double directionX, double directionZ) {
        return List.of(new DirectedCorridor(
                directionX,
                directionZ,
                bounds.stretch(directionX * INTENT_CORRIDOR_DISTANCE, 0.0D, directionZ * INTENT_CORRIDOR_DISTANCE)));
    }

    static List<DirectedCorridor> pathIntentCorridors(
            Box actorBounds,
            Vec3d actorPosition,
            List<Vec3d> remainingNodes,
            double maxDistance
    ) {
        if (actorBounds == null || actorPosition == null || remainingNodes == null
                || !Double.isFinite(maxDistance) || maxDistance <= 0.0D) {
            throw new IllegalArgumentException("path intent requires bounds, position, nodes and positive max distance");
        }
        List<DirectedCorridor> corridors = new ArrayList<>();
        Vec3d segmentStart = actorPosition;
        double remainingDistance = maxDistance;
        for (Vec3d requestedEnd : remainingNodes) {
            if (requestedEnd == null || !allFinite(requestedEnd.x, requestedEnd.y, requestedEnd.z)) {
                throw new IllegalArgumentException("path intent nodes must be finite");
            }
            double dx = requestedEnd.x - segmentStart.x;
            double dy = requestedEnd.y - segmentStart.y;
            double dz = requestedEnd.z - segmentStart.z;
            double distance = horizontalSpeed(dx, dz);
            if (distance <= MIN_HORIZONTAL_SPEED) {
                segmentStart = requestedEnd;
                continue;
            }
            double consumed = Math.min(distance, remainingDistance);
            double fraction = consumed / distance;
            Vec3d end = new Vec3d(
                    segmentStart.x + dx * fraction,
                    segmentStart.y + dy * fraction,
                    segmentStart.z + dz * fraction);
            Box startBounds = actorBounds.offset(
                    segmentStart.x - actorPosition.x,
                    segmentStart.y - actorPosition.y,
                    segmentStart.z - actorPosition.z);
            corridors.add(new DirectedCorridor(
                    dx / distance,
                    dz / distance,
                    startBounds.stretch(end.x - segmentStart.x, end.y - segmentStart.y, end.z - segmentStart.z)));
            remainingDistance -= consumed;
            if (remainingDistance <= MIN_HORIZONTAL_SPEED || consumed < distance) break;
            segmentStart = requestedEnd;
        }
        return List.copyOf(corridors);
    }

    static boolean sweptReciprocalCorridorsConflict(
            Box actorBounds,
            double actorVelocityX,
            double actorVelocityZ,
            Box peerBounds,
            double peerVelocityX,
            double peerVelocityZ,
            double probeDistance
    ) {
        if (actorBounds == null || peerBounds == null
                || !allFinite(actorVelocityX, actorVelocityZ, peerVelocityX, peerVelocityZ, probeDistance)
                || probeDistance <= 0.0D) {
            throw new IllegalArgumentException("reciprocal swept corridors require bounds, finite velocities and positive distance");
        }
        double actorSpeed = horizontalSpeed(actorVelocityX, actorVelocityZ);
        double peerSpeed = horizontalSpeed(peerVelocityX, peerVelocityZ);
        if (actorSpeed <= MIN_HORIZONTAL_SPEED || peerSpeed <= MIN_HORIZONTAL_SPEED) return false;
        Box actorCorridor = actorBounds.stretch(
                actorVelocityX * probeDistance / actorSpeed,
                0.0D,
                actorVelocityZ * probeDistance / actorSpeed);
        Box peerCorridor = peerBounds.stretch(
                peerVelocityX * probeDistance / peerSpeed,
                0.0D,
                peerVelocityZ * probeDistance / peerSpeed);
        return actorCorridor.intersects(peerCorridor);
    }

    static boolean corridorsOccupiedOrClaimed(
            List<DirectedCorridor> yieldCorridors,
            Box peerBounds,
            List<DirectedCorridor> peerIntentCorridors
    ) {
        if (yieldCorridors == null || peerBounds == null || peerIntentCorridors == null) {
            throw new IllegalArgumentException("yield corridors, peer bounds and peer intent corridors are required");
        }
        for (DirectedCorridor yield : yieldCorridors) {
            if (yield.corridor().intersects(peerBounds)) return true;
            for (DirectedCorridor peer : peerIntentCorridors) {
                if (yield.corridor().intersects(peer.corridor())) return true;
            }
        }
        return false;
    }

    static boolean corridorsConflict(List<DirectedCorridor> actorCorridors, List<DirectedCorridor> peerCorridors) {
        if (actorCorridors == null || peerCorridors == null) {
            throw new IllegalArgumentException("corridor conflict requires corridor lists");
        }
        for (DirectedCorridor actor : actorCorridors) {
            for (DirectedCorridor peer : peerCorridors) {
                if (actor.corridor().intersects(peer.corridor())) {
                    double dot = actor.directionX() * peer.directionX() + actor.directionZ() * peer.directionZ();
                    if (dot < 0.75D) return true;
                }
            }
        }
        return false;
    }

    static boolean shouldYield(String actorCanonicalEncounterId, String peerCanonicalEncounterId) {
        String actor = requireCanonicalId(actorCanonicalEncounterId, "actorCanonicalEncounterId");
        String peer = requireCanonicalId(peerCanonicalEncounterId, "peerCanonicalEncounterId");
        return !actor.equals(peer) && actor.compareTo(peer) > 0;
    }

    static boolean shouldRetainLease(
            long currentTick,
            long expiresAtTick,
            boolean actorActive,
            boolean peerActive,
            boolean peerInCorridor
    ) {
        if (currentTick < 0L || expiresAtTick < 0L) {
            throw new IllegalArgumentException("yield lease ticks must be non-negative");
        }
        return currentTick < expiresAtTick && actorActive && peerActive && peerInCorridor;
    }

    static boolean reciprocalApproach(
            double actorX,
            double actorZ,
            double actorVelocityX,
            double actorVelocityZ,
            double peerX,
            double peerZ,
            double peerVelocityX,
            double peerVelocityZ
    ) {
        if (!allFinite(actorX, actorZ, actorVelocityX, actorVelocityZ, peerX, peerZ, peerVelocityX, peerVelocityZ)) {
            throw new IllegalArgumentException("reciprocal approach requires finite coordinates and velocities");
        }
        double separationX = peerX - actorX;
        double separationZ = peerZ - actorZ;
        if (Math.abs(separationX) <= MIN_HORIZONTAL_SPEED && Math.abs(separationZ) <= MIN_HORIZONTAL_SPEED) return true;
        return actorVelocityX * separationX + actorVelocityZ * separationZ > 0.0D
                && peerVelocityX * -separationX + peerVelocityZ * -separationZ > 0.0D;
    }

    static String preferredConflictingCanonicalPeer(String actorCanonicalEncounterId, List<String> peerIds) {
        String actor = requireCanonicalId(actorCanonicalEncounterId, "actorCanonicalEncounterId");
        if (peerIds == null) throw new IllegalArgumentException("conflictingPeerCanonicalIds is required");
        String selected = null;
        for (String value : peerIds) {
            String candidate = requireCanonicalId(value, "conflictingPeerCanonicalId");
            if (candidate.equals(actor)) continue;
            if (selected == null || candidate.compareTo(selected) < 0) selected = candidate;
        }
        return selected;
    }

    private static void stopPresentationMotion(PokemonEntity actor) {
        Vec3d velocity = actor.getVelocity();
        actor.getNavigation().stop();
        actor.setVelocity(0.0D, velocity.y, 0.0D);
        actor.velocityModified = true;
    }

    private static double horizontalSpeed(double x, double z) {
        return Math.sqrt(x * x + z * z);
    }

    private static String requireCanonicalId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    private static boolean allFinite(double... values) {
        for (double value : values) if (!Double.isFinite(value)) return false;
        return true;
    }

    static record DirectedCorridor(double directionX, double directionZ, Box corridor) {
        DirectedCorridor {
            if (!allFinite(directionX, directionZ) || corridor == null) {
                throw new IllegalArgumentException("directed corridor requires finite direction and bounds");
            }
        }
    }

    private record YieldLease(UUID peerUuid, List<DirectedCorridor> actorCorridors, long expiresAtTick) {
        YieldLease {
            if (peerUuid == null || actorCorridors == null || actorCorridors.isEmpty()) {
                throw new IllegalArgumentException("yield lease requires peer and actor corridors");
            }
            actorCorridors = List.copyOf(actorCorridors);
        }
    }

    private record CorridorIntent(String canonicalEncounterId, List<DirectedCorridor> corridors, long expiresAtTick) {
        CorridorIntent {
            canonicalEncounterId = requireCanonicalId(canonicalEncounterId, "canonicalEncounterId");
            corridors = List.copyOf(corridors);
            if (corridors.isEmpty()) throw new IllegalArgumentException("corridor intent requires at least one corridor");
        }
    }
}

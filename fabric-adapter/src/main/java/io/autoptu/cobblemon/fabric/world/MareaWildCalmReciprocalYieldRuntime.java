package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
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
 * Deterministic Minecraft-presentation yield for reciprocal CALM wild encounters.
 *
 * Interaction-active canonical wild actors publish a short-lived server-owned movement intent.
 * When Minecraft has an active native navigation path, the intent follows a bounded lookahead of
 * the remaining path nodes so curves and crossings can be reserved before the actors physically
 * meet. Path-derived corridors retain the native node elevation, so routes that cross only in X/Z
 * at separate heights do not create false yield conflicts. Without an active path, the runtime keeps
 * the existing low-speed velocity corridor as a fallback. The reciprocal physical fallback sweeps
 * both actors' short forward volumes and detects overlap before either current bounding box enters
 * the other's corridor. When multiple published intents or reciprocal fallback peers overlap, the
 * peer is selected by stable canonical encounter identity rather than collection iteration order.
 * The lower lexical encounter identity keeps presentation priority while the higher identity stops.
 * A short server-owned lease keeps that yield stable against the same segmented 3D intent geometry
 * that caused the yield, while the same peer still occupies or claims that reserved route.
 *
 * This never decides PTU movement, initiative, targeting, legality, RNG, damage or results and never
 * reads Cobblemon Pokemon gameplay state.
 */
public final class MareaWildCalmReciprocalYieldRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final int MAX_YIELD_HOLD_TICKS = 40;
    private static final int INTENT_TTL_TICKS = 12;
    private static final int PATH_INTENT_NODE_LOOKAHEAD = 5;
    private static final double PATH_INTENT_MAX_DISTANCE = 5.0D;
    private static final double MAX_CALM_SPEED = 0.025001D;
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
            publishCorridorIntents(world, tick);
            applyActiveLeases(world, tick);
            if (tick % UPDATE_INTERVAL_TICKS == 0) {
                acquireReciprocalYields(world, tick);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            YIELD_LEASES.clear();
            CORRIDOR_INTENTS.clear();
        });
    }

    private static void publishCorridorIntents(ServerWorld world, long tick) {
        CORRIDOR_INTENTS.entrySet().removeIf(entry -> entry.getValue().expiresAtTick() <= tick);
        if (world == null) {
            CORRIDOR_INTENTS.clear();
            return;
        }

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) {
                    CORRIDOR_INTENTS.remove(boundUuid.get());
                    continue;
                }
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) {
                    CORRIDOR_INTENTS.remove(actor.getUuid());
                    continue;
                }

                var velocity = actor.getVelocity();
                double speed = horizontalSpeed(velocity.x, velocity.z);
                if (speed > MAX_CALM_SPEED) {
                    CORRIDOR_INTENTS.remove(actor.getUuid());
                    continue;
                }

                List<DirectedCorridor> corridors = nativePathCorridors(actor);
                if (corridors.isEmpty()) {
                    if (speed <= MIN_HORIZONTAL_SPEED) {
                        CORRIDOR_INTENTS.remove(actor.getUuid());
                        continue;
                    }
                    double directionX = velocity.x / speed;
                    double directionZ = velocity.z / speed;
                    corridors = velocityIntentCorridors(actor.getBoundingBox(), directionX, directionZ);
                }

                DirectedCorridor lead = corridors.getFirst();
                CORRIDOR_INTENTS.put(
                        actor.getUuid(),
                        new CorridorIntent(
                                encounter.canonicalEncounterId(),
                                lead.directionX(),
                                lead.directionZ(),
                                corridors,
                                tick + INTENT_TTL_TICKS));
            }
        }
    }

    private static List<DirectedCorridor> nativePathCorridors(PokemonEntity actor) {
        Path path = actor.getNavigation().getCurrentPath();
        if (path == null || path.isFinished()) return List.of();

        int startIndex = Math.max(0, path.getCurrentNodeIndex());
        int endIndex = Math.min(path.getLength(), startIndex + PATH_INTENT_NODE_LOOKAHEAD);
        if (startIndex >= endIndex) return List.of();

        List<Vec3d> remainingNodes = new ArrayList<>(endIndex - startIndex);
        for (int index = startIndex; index < endIndex; index++) {
            remainingNodes.add(path.getNodePosition(actor, index));
        }
        return pathIntentCorridors(
                actor.getBoundingBox(),
                actor.getPos(),
                remainingNodes,
                PATH_INTENT_MAX_DISTANCE);
    }

    private static List<DirectedCorridor> velocityIntentCorridors(
            Box actorBounds,
            double directionX,
            double directionZ
    ) {
        return List.of(new DirectedCorridor(
                directionX,
                directionZ,
                actorBounds.stretch(
                        directionX * INTENT_CORRIDOR_DISTANCE,
                        0.0D,
                        directionZ * INTENT_CORRIDOR_DISTANCE)));
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
            double deltaX = requestedEnd.x - segmentStart.x;
            double deltaY = requestedEnd.y - segmentStart.y;
            double deltaZ = requestedEnd.z - segmentStart.z;
            double segmentDistance = horizontalSpeed(deltaX, deltaZ);
            if (segmentDistance <= MIN_HORIZONTAL_SPEED) {
                segmentStart = requestedEnd;
                continue;
            }

            double consumedDistance = Math.min(segmentDistance, remainingDistance);
            double fraction = consumedDistance / segmentDistance;
            double endX = segmentStart.x + deltaX * fraction;
            double endY = segmentStart.y + deltaY * fraction;
            double endZ = segmentStart.z + deltaZ * fraction;
            double directionX = deltaX / segmentDistance;
            double directionZ = deltaZ / segmentDistance;

            Box segmentStartBounds = actorBounds.offset(
                    segmentStart.x - actorPosition.x,
                    segmentStart.y - actorPosition.y,
                    segmentStart.z - actorPosition.z);
            Box corridor = segmentStartBounds.stretch(
                    endX - segmentStart.x,
                    endY - segmentStart.y,
                    endZ - segmentStart.z);
            corridors.add(new DirectedCorridor(directionX, directionZ, corridor));

            remainingDistance -= consumedDistance;
            if (remainingDistance <= MIN_HORIZONTAL_SPEED || consumedDistance < segmentDistance) break;
            segmentStart = requestedEnd;
        }
        return List.copyOf(corridors);
    }

    private static void applyActiveLeases(ServerWorld world, long tick) {
        if (world == null) {
            YIELD_LEASES.clear();
            return;
        }

        var iterator = YIELD_LEASES.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            var lease = entry.getValue();
            var actorEntity = world.getEntity(entry.getKey());
            var peerEntity = world.getEntity(lease.peerUuid());
            boolean actorActive = actorEntity instanceof PokemonEntity actor
                    && !actor.isRemoved()
                    && !actor.isInvisible()
                    && VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            boolean peerActive = peerEntity instanceof PokemonEntity peer
                    && !peer.isRemoved()
                    && !peer.isInvisible()
                    && VisibleWildPokemonEncounterRuntime.isInteractionActive(peer.getUuid());

            if (!actorActive || !peerActive) {
                iterator.remove();
                continue;
            }

            PokemonEntity actor = (PokemonEntity) actorEntity;
            PokemonEntity peer = (PokemonEntity) peerEntity;
            var actorBinding = VisibleWildPokemonEncounterRuntime.binding(actor.getUuid());
            var peerBinding = VisibleWildPokemonEncounterRuntime.binding(peer.getUuid());
            boolean canonicalPriorityStillApplies = actorBinding.isPresent()
                    && peerBinding.isPresent()
                    && shouldYield(
                            actorBinding.get().canonicalEncounterId(),
                            peerBinding.get().canonicalEncounterId());
            CorridorIntent peerIntent = CORRIDOR_INTENTS.get(peer.getUuid());
            List<DirectedCorridor> peerIntentCorridors = peerIntent != null && peerIntent.expiresAtTick() > tick
                    ? peerIntent.corridors()
                    : List.of();
            boolean peerInCorridor = canonicalPriorityStillApplies
                    && corridorsOccupiedOrClaimed(
                            lease.actorCorridors(),
                            peer.getBoundingBox(),
                            peerIntentCorridors);

            if (!shouldRetainLease(
                    tick,
                    lease.expiresAtTick(),
                    actorActive,
                    peerActive && canonicalPriorityStillApplies,
                    peerInCorridor)) {
                iterator.remove();
                continue;
            }

            stopPresentationMotion(actor);
        }
    }

    private static void acquireReciprocalYields(ServerWorld world, long tick) {
        if (world == null) return;

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty() || YIELD_LEASES.containsKey(boundUuid.get())) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

                var velocity = actor.getVelocity();
                double speed = horizontalSpeed(velocity.x, velocity.z);
                if (speed > MAX_CALM_SPEED) continue;

                CorridorIntent actorIntent = CORRIDOR_INTENTS.get(actor.getUuid());
                PokemonEntity peer = conflictingIntentPeer(world, actor, tick);
                List<DirectedCorridor> leaseCorridors;
                if (peer != null && actorIntent != null && actorIntent.expiresAtTick() > tick) {
                    leaseCorridors = actorIntent.corridors();
                } else {
                    if (speed <= MIN_HORIZONTAL_SPEED) continue;
                    peer = reciprocalPeerAhead(world, actor, velocity.x, velocity.z);
                    if (peer == null) continue;
                    leaseCorridors = velocityIntentCorridors(
                            actor.getBoundingBox(),
                            velocity.x / speed,
                            velocity.z / speed);
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
    }

    private static PokemonEntity conflictingIntentPeer(ServerWorld world, PokemonEntity actor, long tick) {
        CorridorIntent actorIntent = CORRIDOR_INTENTS.get(actor.getUuid());
        if (actorIntent == null || actorIntent.expiresAtTick() <= tick) return null;

        PokemonEntity selectedPeer = null;
        String selectedCanonicalId = null;
        for (var entry : CORRIDOR_INTENTS.entrySet()) {
            if (entry.getKey().equals(actor.getUuid())) continue;
            CorridorIntent peerIntent = entry.getValue();
            if (peerIntent.expiresAtTick() <= tick) continue;
            if (!corridorsConflict(actorIntent.corridors(), peerIntent.corridors())) continue;

            var peerEntity = world.getEntity(entry.getKey());
            if (!(peerEntity instanceof PokemonEntity peer) || peer.isRemoved() || peer.isInvisible()) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(peer.getUuid())) continue;
            var peerBinding = VisibleWildPokemonEncounterRuntime.binding(peer.getUuid());
            if (peerBinding.isEmpty()) continue;
            String peerCanonicalId = requireCanonicalId(
                    peerBinding.get().canonicalEncounterId(),
                    "peerCanonicalEncounterId");
            if (!peerCanonicalId.equals(peerIntent.canonicalEncounterId())) continue;
            if (selectedCanonicalId == null || peerCanonicalId.compareTo(selectedCanonicalId) < 0) {
                selectedCanonicalId = peerCanonicalId;
                selectedPeer = peer;
            }
        }
        return selectedPeer;
    }

    static String preferredConflictingCanonicalPeer(
            String actorCanonicalEncounterId,
            List<String> conflictingPeerCanonicalIds
    ) {
        String actorId = requireCanonicalId(actorCanonicalEncounterId, "actorCanonicalEncounterId");
        if (conflictingPeerCanonicalIds == null) {
            throw new IllegalArgumentException("conflictingPeerCanonicalIds is required");
        }
        String selected = null;
        for (String candidate : conflictingPeerCanonicalIds) {
            String candidateId = requireCanonicalId(candidate, "conflictingPeerCanonicalId");
            if (actorId.equals(candidateId)) continue;
            if (selected == null || candidateId.compareTo(selected) < 0) selected = candidateId;
        }
        return selected;
    }

    private static PokemonEntity reciprocalPeerAhead(
            ServerWorld world,
            PokemonEntity actor,
            double velocityX,
            double velocityZ
    ) {
        double speed = horizontalSpeed(velocityX, velocityZ);
        if (speed <= MIN_HORIZONTAL_SPEED) return null;
        double scale = RECIPROCAL_PROBE_DISTANCE / speed;
        Box actorCorridor = actor.getBoundingBox().stretch(
                velocityX * scale,
                0.0D,
                velocityZ * scale);
        Box peerSearch = actorCorridor.expand(RECIPROCAL_PROBE_DISTANCE, 0.0D, RECIPROCAL_PROBE_DISTANCE);

        PokemonEntity selectedPeer = null;
        String selectedCanonicalId = null;
        for (var candidate : world.getOtherEntities(
                actor,
                peerSearch,
                entity -> entity instanceof PokemonEntity
                        && VisibleWildPokemonEncounterRuntime.isInteractionActive(entity.getUuid()))) {
            PokemonEntity peer = (PokemonEntity) candidate;
            var peerVelocity = peer.getVelocity();
            double peerSpeed = horizontalSpeed(peerVelocity.x, peerVelocity.z);
            if (peerSpeed <= MIN_HORIZONTAL_SPEED || peerSpeed > MAX_CALM_SPEED) continue;
            if (!reciprocalApproach(
                    actor.getX(), actor.getZ(), velocityX, velocityZ,
                    peer.getX(), peer.getZ(), peerVelocity.x, peerVelocity.z)) continue;
            if (!sweptReciprocalCorridorsConflict(
                    actor.getBoundingBox(), velocityX, velocityZ,
                    peer.getBoundingBox(), peerVelocity.x, peerVelocity.z,
                    RECIPROCAL_PROBE_DISTANCE)) continue;

            var peerBinding = VisibleWildPokemonEncounterRuntime.binding(peer.getUuid());
            if (peerBinding.isEmpty()) continue;
            String peerCanonicalId = requireCanonicalId(
                    peerBinding.get().canonicalEncounterId(),
                    "peerCanonicalEncounterId");
            if (selectedCanonicalId == null || peerCanonicalId.compareTo(selectedCanonicalId) < 0) {
                selectedCanonicalId = peerCanonicalId;
                selectedPeer = peer;
            }
        }
        return selectedPeer;
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

        double actorScale = probeDistance / actorSpeed;
        double peerScale = probeDistance / peerSpeed;
        Box actorCorridor = actorBounds.stretch(
                actorVelocityX * actorScale,
                0.0D,
                actorVelocityZ * actorScale);
        Box peerCorridor = peerBounds.stretch(
                peerVelocityX * peerScale,
                0.0D,
                peerVelocityZ * peerScale);
        return actorCorridor.intersects(peerCorridor);
    }

    private static void stopPresentationMotion(PokemonEntity actor) {
        var velocity = actor.getVelocity();
        actor.getNavigation().stop();
        actor.setVelocity(0.0D, velocity.y, 0.0D);
        actor.velocityModified = true;
    }

    static boolean corridorOccupiedOrClaimed(
            Box yieldCorridor,
            Box peerBounds,
            List<DirectedCorridor> peerIntentCorridors
    ) {
        if (yieldCorridor == null || peerBounds == null || peerIntentCorridors == null) {
            throw new IllegalArgumentException("yield corridor, peer bounds and peer intent corridors are required");
        }
        return corridorsOccupiedOrClaimed(
                List.of(new DirectedCorridor(0.0D, 0.0D, yieldCorridor)),
                peerBounds,
                peerIntentCorridors);
    }

    static boolean corridorsOccupiedOrClaimed(
            List<DirectedCorridor> yieldCorridors,
            Box peerBounds,
            List<DirectedCorridor> peerIntentCorridors
    ) {
        if (yieldCorridors == null || peerBounds == null || peerIntentCorridors == null) {
            throw new IllegalArgumentException("yield corridors, peer bounds and peer intent corridors are required");
        }
        for (DirectedCorridor yieldCorridor : yieldCorridors) {
            if (yieldCorridor == null || yieldCorridor.corridor() == null) {
                throw new IllegalArgumentException("yield corridors must be complete");
            }
            if (yieldCorridor.corridor().intersects(peerBounds)) return true;
            for (DirectedCorridor peerIntentCorridor : peerIntentCorridors) {
                if (peerIntentCorridor == null || peerIntentCorridor.corridor() == null) {
                    throw new IllegalArgumentException("peer intent corridors must be complete");
                }
                if (yieldCorridor.corridor().intersects(peerIntentCorridor.corridor())) return true;
            }
        }
        return false;
    }

    static boolean corridorsConflict(List<DirectedCorridor> actorCorridors, List<DirectedCorridor> peerCorridors) {
        if (actorCorridors == null || peerCorridors == null) {
            throw new IllegalArgumentException("corridor conflict requires corridor lists");
        }
        for (DirectedCorridor actorCorridor : actorCorridors) {
            for (DirectedCorridor peerCorridor : peerCorridors) {
                if (corridorsConflict(
                        actorCorridor.directionX(), actorCorridor.directionZ(), actorCorridor.corridor(),
                        peerCorridor.directionX(), peerCorridor.directionZ(), peerCorridor.corridor())) {
                    return true;
                }
            }
        }
        return false;
    }

    static boolean corridorsConflict(
            double actorDirectionX,
            double actorDirectionZ,
            Box actorCorridor,
            double peerDirectionX,
            double peerDirectionZ,
            Box peerCorridor
    ) {
        if (!allFinite(actorDirectionX, actorDirectionZ, peerDirectionX, peerDirectionZ)
                || actorCorridor == null || peerCorridor == null) {
            throw new IllegalArgumentException("corridor conflict requires finite directions and corridors");
        }
        if (!actorCorridor.intersects(peerCorridor)) return false;
        double dot = actorDirectionX * peerDirectionX + actorDirectionZ * peerDirectionZ;
        return dot < 0.75D;
    }

    static boolean shouldYield(String actorCanonicalEncounterId, String peerCanonicalEncounterId) {
        String actorId = requireCanonicalId(actorCanonicalEncounterId, "actorCanonicalEncounterId");
        String peerId = requireCanonicalId(peerCanonicalEncounterId, "peerCanonicalEncounterId");
        if (actorId.equals(peerId)) return false;
        return actorId.compareTo(peerId) > 0;
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
        if (!allFinite(
                actorX, actorZ, actorVelocityX, actorVelocityZ,
                peerX, peerZ, peerVelocityX, peerVelocityZ)) {
            throw new IllegalArgumentException("reciprocal approach requires finite coordinates and velocities");
        }
        double separationX = peerX - actorX;
        double separationZ = peerZ - actorZ;
        if (Math.abs(separationX) <= MIN_HORIZONTAL_SPEED && Math.abs(separationZ) <= MIN_HORIZONTAL_SPEED) {
            return true;
        }
        double actorTowardPeer = actorVelocityX * separationX + actorVelocityZ * separationZ;
        double peerTowardActor = peerVelocityX * -separationX + peerVelocityZ * -separationZ;
        return actorTowardPeer > 0.0D && peerTowardActor > 0.0D;
    }

    private static double horizontalSpeed(double velocityX, double velocityZ) {
        return Math.sqrt(velocityX * velocityX + velocityZ * velocityZ);
    }

    private static String requireCanonicalId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }

    private static boolean allFinite(double... values) {
        for (double value : values) {
            if (!Double.isFinite(value)) return false;
        }
        return true;
    }

    static record DirectedCorridor(double directionX, double directionZ, Box corridor) {
        DirectedCorridor {
            if (!allFinite(directionX, directionZ) || corridor == null) {
                throw new IllegalArgumentException("directed corridor requires finite direction and bounds");
            }
        }
    }

    private record YieldLease(
            UUID peerUuid,
            List<DirectedCorridor> actorCorridors,
            long expiresAtTick
    ) {
        YieldLease {
            if (peerUuid == null || actorCorridors == null || actorCorridors.isEmpty()) {
                throw new IllegalArgumentException("yield lease requires peer and actor corridors");
            }
            actorCorridors = List.copyOf(actorCorridors);
        }
    }

    private record CorridorIntent(
            String canonicalEncounterId,
            double directionX,
            double directionZ,
            List<DirectedCorridor> corridors,
            long expiresAtTick
    ) {
        CorridorIntent {
            canonicalEncounterId = requireCanonicalId(canonicalEncounterId, "canonicalEncounterId");
            corridors = List.copyOf(corridors);
            if (corridors.isEmpty()) throw new IllegalArgumentException("corridor intent requires at least one corridor");
        }
    }
}

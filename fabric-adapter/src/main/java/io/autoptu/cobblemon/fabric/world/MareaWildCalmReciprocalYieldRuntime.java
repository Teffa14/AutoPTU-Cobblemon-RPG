package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
 * Interaction-active canonical wild actors publish a short-lived server-owned movement corridor
 * from the remaining nodes of their active native Minecraft path. When no usable path segment is
 * available, observed low-speed CALM velocity remains the presentation fallback. When two
 * published corridors overlap while both actors are approaching the shared space, exactly one
 * actor yields according to canonical encounter identity before their physical bounding boxes have
 * to collide. The lower lexical encounter identity keeps presentation priority while the higher
 * identity stops. A short server-owned lease keeps that yield stable while the same peer still
 * occupies or claims the actor's forward corridor, avoiding cadence-by-cadence stop/start
 * oscillation.
 *
 * This never decides PTU movement, initiative, targeting, legality, RNG, damage or results and never
 * reads Cobblemon Pokemon gameplay state.
 */
public final class MareaWildCalmReciprocalYieldRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final int MAX_YIELD_HOLD_TICKS = 40;
    private static final int INTENT_TTL_TICKS = 12;
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
                if (speed <= MIN_HORIZONTAL_SPEED || speed > MAX_CALM_SPEED) {
                    CORRIDOR_INTENTS.remove(actor.getUuid());
                    continue;
                }

                double directionX = velocity.x / speed;
                double directionZ = velocity.z / speed;
                List<CorridorSegment> segments = nativePathCorridorSegments(actor);
                if (segments.isEmpty()) {
                    Box corridor = actor.getBoundingBox().stretch(
                            directionX * INTENT_CORRIDOR_DISTANCE,
                            0.0D,
                            directionZ * INTENT_CORRIDOR_DISTANCE);
                    segments = List.of(new CorridorSegment(directionX, directionZ, corridor));
                }
                CORRIDOR_INTENTS.put(
                        actor.getUuid(),
                        new CorridorIntent(
                                encounter.canonicalEncounterId(),
                                segments,
                                tick + INTENT_TTL_TICKS));
            }
        }
    }

    private static List<CorridorSegment> nativePathCorridorSegments(PokemonEntity actor) {
        var path = actor.getNavigation().getCurrentPath();
        if (path == null || path.isFinished()) return List.of();

        int currentNodeIndex = Math.max(0, path.getCurrentNodeIndex());
        if (currentNodeIndex >= path.getLength()) return List.of();

        List<Vec3d> remainingNodes = new ArrayList<>();
        for (int index = currentNodeIndex; index < path.getLength(); index++) {
            remainingNodes.add(path.getNodePosition(actor, index));
        }
        return buildPathCorridorSegments(
                actor.getBoundingBox(),
                actor.getPos(),
                remainingNodes,
                INTENT_CORRIDOR_DISTANCE);
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
            Box yieldCorridor = actor.getBoundingBox().stretch(
                    lease.directionX() * INTENT_CORRIDOR_DISTANCE,
                    0.0D,
                    lease.directionZ() * INTENT_CORRIDOR_DISTANCE);
            CorridorIntent peerIntent = CORRIDOR_INTENTS.get(peer.getUuid());
            List<CorridorSegment> peerIntentSegments = peerIntent != null && peerIntent.expiresAtTick() > tick
                    ? peerIntent.segments()
                    : List.of();
            boolean peerInCorridor = canonicalPriorityStillApplies
                    && corridorOccupiedOrClaimedBySegments(
                            yieldCorridor,
                            peer.getBoundingBox(),
                            peerIntentSegments);

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
                if (speed <= MIN_HORIZONTAL_SPEED || speed > MAX_CALM_SPEED) continue;

                PokemonEntity peer = conflictingIntentPeer(world, actor, tick);
                if (peer == null) {
                    peer = reciprocalPeerAhead(world, actor, velocity.x, velocity.z);
                }
                if (peer == null) continue;

                var actorBinding = VisibleWildPokemonEncounterRuntime.binding(actor.getUuid());
                var peerBinding = VisibleWildPokemonEncounterRuntime.binding(peer.getUuid());
                if (actorBinding.isEmpty() || peerBinding.isEmpty()) continue;
                if (!shouldYield(
                        actorBinding.get().canonicalEncounterId(),
                        peerBinding.get().canonicalEncounterId())) continue;

                double directionX = velocity.x / speed;
                double directionZ = velocity.z / speed;
                YIELD_LEASES.put(
                        actor.getUuid(),
                        new YieldLease(peer.getUuid(), directionX, directionZ, tick + MAX_YIELD_HOLD_TICKS));
                stopPresentationMotion(actor);
            }
        }
    }

    private static PokemonEntity conflictingIntentPeer(ServerWorld world, PokemonEntity actor, long tick) {
        CorridorIntent actorIntent = CORRIDOR_INTENTS.get(actor.getUuid());
        if (actorIntent == null || actorIntent.expiresAtTick() <= tick) return null;

        for (var entry : CORRIDOR_INTENTS.entrySet()) {
            if (entry.getKey().equals(actor.getUuid())) continue;
            CorridorIntent peerIntent = entry.getValue();
            if (peerIntent.expiresAtTick() <= tick) continue;
            if (!corridorSegmentsConflict(actorIntent.segments(), peerIntent.segments())) continue;

            var peerEntity = world.getEntity(entry.getKey());
            if (!(peerEntity instanceof PokemonEntity peer) || peer.isRemoved() || peer.isInvisible()) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(peer.getUuid())) continue;
            return peer;
        }
        return null;
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
        var projectedBox = actor.getBoundingBox().offset(velocityX * scale, 0.0D, velocityZ * scale);

        for (var candidate : world.getOtherEntities(
                actor,
                projectedBox,
                entity -> entity instanceof PokemonEntity
                        && VisibleWildPokemonEncounterRuntime.isInteractionActive(entity.getUuid()))) {
            PokemonEntity peer = (PokemonEntity) candidate;
            var peerVelocity = peer.getVelocity();
            double peerSpeed = horizontalSpeed(peerVelocity.x, peerVelocity.z);
            if (peerSpeed <= MIN_HORIZONTAL_SPEED || peerSpeed > MAX_CALM_SPEED) continue;
            if (reciprocalApproach(
                    actor.getX(), actor.getZ(), velocityX, velocityZ,
                    peer.getX(), peer.getZ(), peerVelocity.x, peerVelocity.z)) {
                return peer;
            }
        }
        return null;
    }

    private static void stopPresentationMotion(PokemonEntity actor) {
        var velocity = actor.getVelocity();
        actor.getNavigation().stop();
        actor.setVelocity(0.0D, velocity.y, 0.0D);
        actor.velocityModified = true;
    }

    private static boolean corridorOccupiedOrClaimedBySegments(
            Box yieldCorridor,
            Box peerBounds,
            List<CorridorSegment> peerIntentSegments
    ) {
        if (yieldCorridor == null || peerBounds == null || peerIntentSegments == null) {
            throw new IllegalArgumentException("yield corridor, peer bounds and peer intent segments are required");
        }
        if (yieldCorridor.intersects(peerBounds)) return true;
        for (CorridorSegment segment : peerIntentSegments) {
            if (segment != null && yieldCorridor.intersects(segment.corridor())) return true;
        }
        return false;
    }

    static boolean corridorOccupiedOrClaimed(Box yieldCorridor, Box peerBounds, Box peerIntentCorridor) {
        if (yieldCorridor == null || peerBounds == null) {
            throw new IllegalArgumentException("yield corridor and peer bounds are required");
        }
        return yieldCorridor.intersects(peerBounds)
                || (peerIntentCorridor != null && yieldCorridor.intersects(peerIntentCorridor));
    }

    static boolean corridorSegmentsConflict(
            List<CorridorSegment> actorSegments,
            List<CorridorSegment> peerSegments
    ) {
        if (actorSegments == null || peerSegments == null) {
            throw new IllegalArgumentException("corridor segment lists are required");
        }
        for (CorridorSegment actorSegment : actorSegments) {
            if (actorSegment == null) continue;
            for (CorridorSegment peerSegment : peerSegments) {
                if (peerSegment == null) continue;
                if (corridorsConflict(
                        actorSegment.directionX(),
                        actorSegment.directionZ(),
                        actorSegment.corridor(),
                        peerSegment.directionX(),
                        peerSegment.directionZ(),
                        peerSegment.corridor())) {
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

    static List<CorridorSegment> buildPathCorridorSegments(
            Box actorBounds,
            Vec3d actorPosition,
            List<Vec3d> waypoints,
            double maxHorizontalDistance
    ) {
        if (actorBounds == null || actorPosition == null || waypoints == null
                || !allFinite(actorPosition.x, actorPosition.y, actorPosition.z, maxHorizontalDistance)
                || maxHorizontalDistance <= 0.0D) {
            throw new IllegalArgumentException("path corridor requires finite actor geometry and positive distance");
        }

        List<CorridorSegment> segments = new ArrayList<>();
        Vec3d previous = actorPosition;
        double remaining = maxHorizontalDistance;
        for (Vec3d waypoint : waypoints) {
            if (waypoint == null || !allFinite(waypoint.x, waypoint.y, waypoint.z)) {
                throw new IllegalArgumentException("path corridor waypoints must be finite");
            }

            double deltaX = waypoint.x - previous.x;
            double deltaY = waypoint.y - previous.y;
            double deltaZ = waypoint.z - previous.z;
            double horizontalDistance = horizontalSpeed(deltaX, deltaZ);
            if (horizontalDistance <= MIN_HORIZONTAL_SPEED) {
                previous = waypoint;
                continue;
            }

            double usedDistance = Math.min(horizontalDistance, remaining);
            double ratio = usedDistance / horizontalDistance;
            double usedX = deltaX * ratio;
            double usedY = deltaY * ratio;
            double usedZ = deltaZ * ratio;
            Box segmentBounds = actorBounds
                    .offset(
                            previous.x - actorPosition.x,
                            previous.y - actorPosition.y,
                            previous.z - actorPosition.z)
                    .stretch(usedX, usedY, usedZ);
            segments.add(new CorridorSegment(
                    deltaX / horizontalDistance,
                    deltaZ / horizontalDistance,
                    segmentBounds));

            remaining -= usedDistance;
            if (remaining <= MIN_HORIZONTAL_SPEED) break;
            previous = waypoint;
        }
        return List.copyOf(segments);
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

    private record YieldLease(
            UUID peerUuid,
            double directionX,
            double directionZ,
            long expiresAtTick
    ) {}

    record CorridorSegment(
            double directionX,
            double directionZ,
            Box corridor
    ) {}

    private record CorridorIntent(
            String canonicalEncounterId,
            List<CorridorSegment> segments,
            long expiresAtTick
    ) {}
}

package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Deterministic Minecraft-presentation yield for reciprocal CALM wild encounters.
 *
 * When two interaction-active canonical wild actors are already moving toward one another, exactly
 * one actor yields according to canonical encounter identity. The lower lexical encounter identity
 * keeps presentation priority while the higher identity stops. A short server-owned lease keeps
 * that yield stable while the same peer still physically occupies the actor's forward corridor,
 * avoiding cadence-by-cadence stop/start oscillation. The lease is released as soon as the peer
 * clears the corridor or either canonical presentation binding becomes inactive, and it has a hard
 * lifetime cap so stale world presentation can never retain a yield indefinitely.
 *
 * This never decides PTU movement, initiative, targeting, legality, RNG, damage or results and never
 * reads Cobblemon Pokemon gameplay state.
 */
public final class MareaWildCalmReciprocalYieldRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final int MAX_YIELD_HOLD_TICKS = 40;
    private static final double MAX_CALM_SPEED = 0.025001D;
    private static final double MIN_HORIZONTAL_SPEED = 0.000001D;
    private static final double RECIPROCAL_PROBE_DISTANCE = 0.9D;
    private static final Map<UUID, YieldLease> YIELD_LEASES = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ServerWorld world = server.getOverworld();
            long tick = server.getTicks();
            applyActiveLeases(world, tick);
            if (tick % UPDATE_INTERVAL_TICKS == 0) {
                acquireReciprocalYields(world, tick);
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> YIELD_LEASES.clear());
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
            boolean peerInCorridor = canonicalPriorityStillApplies
                    && peerOccupiesYieldCorridor(actor, peer, lease.directionX(), lease.directionZ());

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

                PokemonEntity peer = reciprocalPeerAhead(world, actor, velocity.x, velocity.z);
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

    private static boolean peerOccupiesYieldCorridor(
            PokemonEntity actor,
            PokemonEntity peer,
            double directionX,
            double directionZ
    ) {
        var corridor = actor.getBoundingBox().stretch(
                directionX * RECIPROCAL_PROBE_DISTANCE,
                0.0D,
                directionZ * RECIPROCAL_PROBE_DISTANCE);
        return corridor.intersects(peer.getBoundingBox());
    }

    private static void stopPresentationMotion(PokemonEntity actor) {
        var velocity = actor.getVelocity();
        actor.getNavigation().stop();
        actor.setVelocity(0.0D, velocity.y, 0.0D);
        actor.velocityModified = true;
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
}

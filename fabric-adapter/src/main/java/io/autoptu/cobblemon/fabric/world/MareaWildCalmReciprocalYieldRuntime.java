package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.world.ServerWorld;

/**
 * Deterministic Minecraft-presentation yield for reciprocal CALM wild encounters.
 *
 * When two interaction-active canonical wild actors are already moving toward one another, exactly
 * one actor yields according to canonical encounter identity. The lower lexical encounter identity
 * keeps presentation priority while the higher identity stops for this cadence, allowing the
 * existing collision/path runtime to route the priority actor around a stationary peer instead of
 * making both actors repeatedly choose new detours. This never decides PTU movement, initiative,
 * targeting, legality, RNG, damage or results and never reads Cobblemon Pokemon gameplay state.
 */
public final class MareaWildCalmReciprocalYieldRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double MAX_CALM_SPEED = 0.025001D;
    private static final double MIN_HORIZONTAL_SPEED = 0.000001D;
    private static final double RECIPROCAL_PROBE_DISTANCE = 0.9D;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            apply(server.getOverworld());
        });
    }

    static void apply(ServerWorld world) {
        if (world == null) return;

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
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

                actor.getNavigation().stop();
                actor.setVelocity(0.0D, velocity.y, 0.0D);
                actor.velocityModified = true;
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

    static boolean shouldYield(String actorCanonicalEncounterId, String peerCanonicalEncounterId) {
        String actorId = requireCanonicalId(actorCanonicalEncounterId, "actorCanonicalEncounterId");
        String peerId = requireCanonicalId(peerCanonicalEncounterId, "peerCanonicalEncounterId");
        if (actorId.equals(peerId)) return false;
        return actorId.compareTo(peerId) > 0;
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
}

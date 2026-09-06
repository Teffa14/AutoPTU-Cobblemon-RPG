package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Generic server-owned herd attention for visible wild actors during CALM rest windows.
 *
 * <p>The presentation anchor is selected only from interaction-active actors already published by
 * the canonical ecology projection for the same population. Canonically authored Alpha actors are
 * preferred inside the same cohesion radius; otherwise the nearest eligible member remains the
 * anchor. Selection uses only server-owned ecology role, server-observed world position and stable
 * Minecraft actor identity. Species, level, stats, moves, abilities and Cobblemon gameplay state are
 * never read. The anchor is presentation-only and has no PTU initiative, leadership or battle
 * semantics.</p>
 */
public final class WildCalmHerdAttentionRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double MIN_ANCHOR_DISTANCE_SQUARED = 0.000000000001D;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            update(server.getOverworld());
        });
    }

    static void update(ServerWorld world) {
        if (world == null) return;
        List<WildEcologyProjectionRegistry.ProjectedActor> projected = WildEcologyProjectionRegistry.collect(world);
        for (WildEcologyProjectionRegistry.ProjectedActor projection : projected) {
            apply(world, projection, projected);
        }
    }

    static boolean apply(
            ServerWorld world,
            WildEcologyProjectionRegistry.ProjectedActor projection,
            List<WildEcologyProjectionRegistry.ProjectedActor> allActors
    ) {
        if (world == null || projection == null || allActors == null) return false;
        PokemonEntity actor = projection.actor();
        WildBehaviorProfile profile = projection.behaviorProfile();
        if (profile.calmMovementActive(world.getTime())) return false;
        if (!eligibleRestingActor(world, actor, profile)) return false;

        Optional<WildEcologyProjectionRegistry.ProjectedActor> anchor = herdAnchor(
                projection,
                allActors,
                profile.cohesionDistance());
        if (anchor.isEmpty()) return false;

        PokemonEntity anchorActor = anchor.get().actor();
        double dx = anchorActor.getX() - actor.getX();
        double dz = anchorActor.getZ() - actor.getZ();
        float yaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float pitch = WildCalmIdleLookRuntime.idleFacingPitch(
                actor.getY() + actor.getStandingEyeHeight(),
                anchorActor.getY() + anchorActor.getStandingEyeHeight(),
                actor.getX(),
                actor.getZ(),
                anchorActor.getX(),
                anchorActor.getZ());
        actor.setYaw(yaw);
        actor.setHeadYaw(yaw);
        actor.setPitch(pitch);
        return true;
    }

    static Optional<WildEcologyProjectionRegistry.ProjectedActor> herdAnchor(
            WildEcologyProjectionRegistry.ProjectedActor projection,
            List<WildEcologyProjectionRegistry.ProjectedActor> allActors,
            double cohesionDistance
    ) {
        if (projection == null || allActors == null || !Double.isFinite(cohesionDistance) || cohesionDistance <= 0.0D) {
            return Optional.empty();
        }

        PokemonEntity actor = projection.actor();
        UUID actorId = actor.getUuid();
        double maxDistanceSquared = cohesionDistance * cohesionDistance;

        return allActors.stream()
                .filter(candidate -> candidate != null)
                .filter(candidate -> projection.populationKey().equals(candidate.populationKey()))
                .filter(candidate -> !candidate.actor().getUuid().equals(actorId))
                .filter(candidate -> !candidate.actor().isRemoved() && !candidate.actor().isInvisible())
                .filter(candidate -> VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid()))
                .filter(candidate -> {
                    double distanceSquared = horizontalDistanceSquared(actor, candidate.actor());
                    return Double.isFinite(distanceSquared)
                            && distanceSquared > MIN_ANCHOR_DISTANCE_SQUARED
                            && distanceSquared <= maxDistanceSquared;
                })
                .min(Comparator
                        .comparingInt((WildEcologyProjectionRegistry.ProjectedActor candidate) ->
                                socialRolePriority(candidate.socialRole()))
                        .thenComparingDouble(candidate -> horizontalDistanceSquared(actor, candidate.actor()))
                        .thenComparing(candidate -> candidate.actor().getUuid()));
    }

    static Optional<UUID> deterministicPreferredAnchorIdentity(
            UUID actorId,
            double actorX,
            double actorZ,
            double cohesionDistance,
            List<AnchorCandidate> candidates
    ) {
        if (actorId == null
                || !Double.isFinite(actorX)
                || !Double.isFinite(actorZ)
                || !Double.isFinite(cohesionDistance)
                || cohesionDistance <= 0.0D
                || candidates == null) {
            return Optional.empty();
        }

        double maxDistanceSquared = cohesionDistance * cohesionDistance;
        return candidates.stream()
                .filter(candidate -> candidate != null && candidate.actorId() != null && candidate.socialRole() != null)
                .filter(candidate -> !candidate.actorId().equals(actorId))
                .filter(candidate -> Double.isFinite(candidate.x()) && Double.isFinite(candidate.z()))
                .filter(candidate -> {
                    double distanceSquared = horizontalDistanceSquared(actorX, actorZ, candidate.x(), candidate.z());
                    return distanceSquared > MIN_ANCHOR_DISTANCE_SQUARED && distanceSquared <= maxDistanceSquared;
                })
                .min(Comparator
                        .comparingInt((AnchorCandidate candidate) -> socialRolePriority(candidate.socialRole()))
                        .thenComparingDouble(candidate ->
                                horizontalDistanceSquared(actorX, actorZ, candidate.x(), candidate.z()))
                        .thenComparing(AnchorCandidate::actorId))
                .map(AnchorCandidate::actorId);
    }

    static Optional<UUID> deterministicNearestAnchorIdentity(
            UUID actorId,
            double actorX,
            double actorZ,
            double cohesionDistance,
            List<AnchorCandidate> candidates
    ) {
        return deterministicPreferredAnchorIdentity(actorId, actorX, actorZ, cohesionDistance, candidates);
    }

    private static int socialRolePriority(WildSocialRole role) {
        return role == WildSocialRole.ALPHA ? 0 : 1;
    }

    private static double horizontalDistanceSquared(PokemonEntity first, PokemonEntity second) {
        return horizontalDistanceSquared(first.getX(), first.getZ(), second.getX(), second.getZ());
    }

    private static double horizontalDistanceSquared(double firstX, double firstZ, double secondX, double secondZ) {
        double dx = secondX - firstX;
        double dz = secondZ - firstZ;
        return dx * dx + dz * dz;
    }

    private static boolean eligibleRestingActor(ServerWorld world, PokemonEntity actor, WildBehaviorProfile profile) {
        if (actor.isRemoved() || actor.isInvisible()) return false;
        if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) return false;
        if (!actor.getNavigation().isIdle()) return false;
        var velocity = actor.getVelocity();
        double horizontalSpeed = Math.hypot(velocity.x, velocity.z);
        if (horizontalSpeed > profile.maxIdleHorizontalSpeed()) return false;
        return !hasNearbyPlayer(world, actor, profile.playerGuardRadius());
    }

    private static boolean hasNearbyPlayer(ServerWorld world, PokemonEntity actor, double radius) {
        double radiusSquared = radius * radius;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            if (actor.squaredDistanceTo(player) <= radiusSquared) return true;
        }
        return false;
    }

    record AnchorCandidate(UUID actorId, double x, double z, WildSocialRole socialRole) {
        AnchorCandidate(UUID actorId, double x, double z) {
            this(actorId, x, z, WildSocialRole.MEMBER);
        }
    }
}

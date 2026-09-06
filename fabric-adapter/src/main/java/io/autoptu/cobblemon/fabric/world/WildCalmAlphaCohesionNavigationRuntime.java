package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.entity.ai.pathing.Path;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Minecraft-native CALM cohesion toward a canonically authored Alpha actor.
 *
 * <p>This runtime only runs for visible interaction-active herd members during authored CALM
 * movement windows with no nearby player guard. If the member has drifted beyond its authored
 * cohesion distance, it may start a leash-safe Minecraft navigation path toward the same-population
 * Alpha that was already published by the server ecology projection. The Alpha role has no PTU
 * initiative, movement, targeting, stat, damage, reward or encounter authority.</p>
 */
public final class WildCalmAlphaCohesionNavigationRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double NATIVE_NAVIGATION_SPEED = 0.08D;
    private static final double MIN_DISTANCE_SQUARED = 0.000000000001D;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server == null || server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
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
        if (projection.socialRole() == WildSocialRole.ALPHA) return false;

        PokemonEntity actor = projection.actor();
        WildBehaviorProfile profile = projection.behaviorProfile();
        if (actor.isRemoved() || actor.isInvisible()) return false;
        if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) return false;
        if (!profile.calmMovementActive(world.getTime())) return false;
        if (hasNearbyPlayer(world, actor, profile.playerGuardRadius())) return false;
        if (!actor.getNavigation().isIdle()) return false;
        if (!WildCalmCollisionNavigationRuntime.navigationTargetInsideLeash(
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                projection.habitatLeashRadiusBlocks(),
                actor.getX(),
                actor.getZ())) return false;

        Optional<WildEcologyProjectionRegistry.ProjectedActor> alpha = alphaAnchor(projection, allActors);
        if (alpha.isEmpty()) return false;
        PokemonEntity anchor = alpha.get().actor();

        double dx = anchor.getX() - actor.getX();
        double dz = anchor.getZ() - actor.getZ();
        double distanceSquared = dx * dx + dz * dz;
        double cohesionDistance = profile.cohesionDistance();
        if (!Double.isFinite(distanceSquared)
                || distanceSquared <= cohesionDistance * cohesionDistance
                || distanceSquared <= MIN_DISTANCE_SQUARED) return false;

        double[] target = new double[] {anchor.getX(), anchor.getZ()};
        if (!WildCalmCollisionNavigationRuntime.navigationTargetInsideLeash(
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                projection.habitatLeashRadiusBlocks(),
                target[0],
                target[1])) return false;

        Path path = WildCalmCollisionNavigationRuntime.findLeashSafeNativePath(
                actor,
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                projection.habitatLeashRadiusBlocks(),
                target);
        return path != null && actor.getNavigation().startMovingAlong(path, NATIVE_NAVIGATION_SPEED);
    }

    static Optional<WildEcologyProjectionRegistry.ProjectedActor> alphaAnchor(
            WildEcologyProjectionRegistry.ProjectedActor projection,
            List<WildEcologyProjectionRegistry.ProjectedActor> allActors
    ) {
        if (projection == null || allActors == null) return Optional.empty();
        PokemonEntity actor = projection.actor();
        UUID selected = deterministicAlphaAnchorIdentity(
                actor.getUuid(),
                actor.getX(),
                actor.getZ(),
                projection.populationKey(),
                candidates(allActors)).orElse(null);
        if (selected == null) return Optional.empty();
        return allActors.stream()
                .filter(candidate -> candidate != null && candidate.actor().getUuid().equals(selected))
                .findFirst();
    }

    static Optional<UUID> deterministicAlphaAnchorIdentity(
            UUID actorId,
            double actorX,
            double actorZ,
            String populationKey,
            List<AlphaCandidate> candidates
    ) {
        if (actorId == null
                || !Double.isFinite(actorX)
                || !Double.isFinite(actorZ)
                || populationKey == null
                || populationKey.isBlank()
                || candidates == null) return Optional.empty();

        return candidates.stream()
                .filter(candidate -> candidate != null)
                .filter(candidate -> candidate.actorId() != null && !candidate.actorId().equals(actorId))
                .filter(candidate -> candidate.socialRole() == WildSocialRole.ALPHA)
                .filter(AlphaCandidate::interactionActive)
                .filter(candidate -> populationKey.equals(candidate.populationKey()))
                .filter(candidate -> Double.isFinite(candidate.x()) && Double.isFinite(candidate.z()))
                .filter(candidate -> horizontalDistanceSquared(actorX, actorZ, candidate.x(), candidate.z())
                        > MIN_DISTANCE_SQUARED)
                .min(Comparator
                        .comparingDouble((AlphaCandidate candidate) ->
                                horizontalDistanceSquared(actorX, actorZ, candidate.x(), candidate.z()))
                        .thenComparing(AlphaCandidate::actorId))
                .map(AlphaCandidate::actorId);
    }

    private static List<AlphaCandidate> candidates(
            List<WildEcologyProjectionRegistry.ProjectedActor> allActors
    ) {
        List<AlphaCandidate> candidates = new ArrayList<>();
        for (WildEcologyProjectionRegistry.ProjectedActor candidate : allActors) {
            if (candidate == null) continue;
            PokemonEntity actor = candidate.actor();
            if (actor.isRemoved() || actor.isInvisible()) continue;
            candidates.add(new AlphaCandidate(
                    actor.getUuid(),
                    candidate.populationKey(),
                    actor.getX(),
                    actor.getZ(),
                    candidate.socialRole(),
                    VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())));
        }
        return List.copyOf(candidates);
    }

    private static double horizontalDistanceSquared(
            double firstX,
            double firstZ,
            double secondX,
            double secondZ
    ) {
        double dx = secondX - firstX;
        double dz = secondZ - firstZ;
        return dx * dx + dz * dz;
    }

    private static boolean hasNearbyPlayer(ServerWorld world, PokemonEntity actor, double guardRadius) {
        double limitSquared = guardRadius * guardRadius;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            if (actor.squaredDistanceTo(player) <= limitSquared) return true;
        }
        return false;
    }

    record AlphaCandidate(
            UUID actorId,
            String populationKey,
            double x,
            double z,
            WildSocialRole socialRole,
            boolean interactionActive
    ) {}
}

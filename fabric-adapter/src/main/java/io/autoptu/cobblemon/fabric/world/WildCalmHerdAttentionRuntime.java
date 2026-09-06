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
 * the canonical ecology projection for the same population. Selection uses stable Minecraft actor
 * identity; species, level, stats, moves, abilities and Cobblemon gameplay state are never read.
 * The anchor is presentation-only and has no PTU initiative, leadership or battle semantics.</p>
 */
public final class WildCalmHerdAttentionRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;

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

        Optional<WildEcologyProjectionRegistry.ProjectedActor> anchor = herdAnchor(projection, allActors);
        if (anchor.isEmpty() || anchor.get().actor().getUuid().equals(actor.getUuid())) return false;

        PokemonEntity anchorActor = anchor.get().actor();
        double dx = anchorActor.getX() - actor.getX();
        double dz = anchorActor.getZ() - actor.getZ();
        double distance = Math.hypot(dx, dz);
        if (distance <= 0.000001D || distance > profile.cohesionDistance()) return false;

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
            List<WildEcologyProjectionRegistry.ProjectedActor> allActors
    ) {
        if (projection == null || allActors == null) return Optional.empty();
        return allActors.stream()
                .filter(candidate -> candidate != null)
                .filter(candidate -> projection.populationKey().equals(candidate.populationKey()))
                .filter(candidate -> !candidate.actor().isRemoved() && !candidate.actor().isInvisible())
                .filter(candidate -> VisibleWildPokemonEncounterRuntime.isInteractionActive(candidate.actor().getUuid()))
                .min(Comparator.comparing(candidate -> candidate.actor().getUuid()));
    }

    static Optional<UUID> deterministicAnchorIdentity(List<UUID> actorIds) {
        if (actorIds == null) return Optional.empty();
        return actorIds.stream().filter(id -> id != null).min(UUID::compareTo);
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
}

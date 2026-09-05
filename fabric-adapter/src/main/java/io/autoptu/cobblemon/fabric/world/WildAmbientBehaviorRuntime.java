package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * World-wide Minecraft ambient behavior for server-authored visible wild actors.
 *
 * Region/species adapters publish actor, population, habitat and behavior profile through
 * {@link WildEcologyProjectionRegistry}. This runtime owns generic CALM roaming, same-population
 * separation/cohesion, habitat recovery and player watch/alarm presentation for every published
 * population. It consumes Minecraft presentation state only and never supplies PTU movement,
 * initiative, targeting, RNG, damage, moves, abilities, legality or outcomes.
 */
public final class WildAmbientBehaviorRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final Map<MinecraftServer, Map<UUID, AmbientPokemonBehaviorController>> CONTROLLERS =
            new IdentityHashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (server.getTicks() % UPDATE_INTERVAL_TICKS != 0) return;
            update(server);
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            synchronized (CONTROLLERS) {
                CONTROLLERS.remove(server);
            }
        });
    }

    static void update(MinecraftServer server) {
        if (server == null) return;
        ServerWorld world = server.getOverworld();
        List<WildEcologyProjectionRegistry.ProjectedActor> projected = WildEcologyProjectionRegistry.collect(world);
        Map<UUID, AmbientPokemonBehaviorController> controllers = controllersFor(server);
        var liveActors = new java.util.HashSet<UUID>();

        for (var projection : projected) {
            PokemonEntity actor = projection.actor();
            if (actor.isRemoved() || actor.isInvisible()) continue;
            liveActors.add(actor.getUuid());

            ServerPlayerEntity nearest = nearestPlayer(world, actor);
            WildBehaviorProfile profile = projection.behaviorProfile();
            AmbientPokemonBehaviorController controller = controllers.computeIfAbsent(
                    actor.getUuid(), ignored -> new AmbientPokemonBehaviorController(profile.proximityProfile()));
            var state = controller.update(
                    nearest == null ? Double.POSITIVE_INFINITY : Math.sqrt(actor.squaredDistanceTo(nearest)),
                    nearest != null);
            applyPresentation(projection, nearest, state, projected, world.getTime());
        }

        controllers.keySet().removeIf(uuid -> !liveActors.contains(uuid));
    }

    private static ServerPlayerEntity nearestPlayer(ServerWorld world, PokemonEntity actor) {
        ServerPlayerEntity nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator()) continue;
            double distance = actor.squaredDistanceTo(player);
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = player;
            }
        }
        return nearest;
    }

    private static void applyPresentation(
            WildEcologyProjectionRegistry.ProjectedActor projection,
            ServerPlayerEntity nearest,
            AmbientPokemonBehaviorController.State state,
            List<WildEcologyProjectionRegistry.ProjectedActor> allActors,
            long worldTime
    ) {
        PokemonEntity actor = projection.actor();
        WildBehaviorProfile profile = projection.behaviorProfile();
        double centerX = projection.habitatCenterX();
        double centerZ = projection.habitatCenterZ();
        int leash = projection.habitatLeashRadiusBlocks();

        // Native pathing remains a bounded presentation bridge between ambient evaluations.
        // Region-agnostic collision/navigation runtimes may start a fresh safe path afterwards.
        actor.getNavigation().stop();

        if (!insideHorizontalLeash(actor.getX(), actor.getZ(), centerX, centerZ, leash)) {
            applyRecovery(actor, centerX, centerZ, profile);
            return;
        }
        if (state == AmbientPokemonBehaviorController.State.RECOVERING) {
            applyRecovery(actor, centerX, centerZ, profile);
            return;
        }
        if (state == AmbientPokemonBehaviorController.State.CALM) {
            applyCalmRoaming(projection, nearestPopulationSibling(projection, allActors), worldTime);
            return;
        }
        if (nearest == null) return;

        double dx = nearest.getX() - actor.getX();
        double dz = nearest.getZ() - actor.getZ();
        float towardPlayerYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        if (state == AmbientPokemonBehaviorController.State.WATCHING) {
            actor.setYaw(towardPlayerYaw);
            return;
        }
        if (state != AmbientPokemonBehaviorController.State.ALARMED) return;

        actor.setYaw(towardPlayerYaw + 180.0F);
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length <= 0.001D) return;
        double fleeX = (-dx / length) * profile.fleeSpeed();
        double fleeZ = (-dz / length) * profile.fleeSpeed();
        if (!insideLeashAfterImpulse(actor, centerX, centerZ, leash, fleeX, fleeZ)) {
            double[] recovery = recoveryImpulse(actor.getX(), actor.getZ(), centerX, centerZ, profile.fleeSpeed());
            fleeX = recovery[0];
            fleeZ = recovery[1];
            if (fleeX == 0.0D && fleeZ == 0.0D) return;
        }
        setAmbientHorizontalVelocity(actor, fleeX, fleeZ, profile.fleeSpeed());
    }

    static WildEcologyProjectionRegistry.ProjectedActor nearestPopulationSibling(
            WildEcologyProjectionRegistry.ProjectedActor actor,
            List<WildEcologyProjectionRegistry.ProjectedActor> allActors
    ) {
        WildEcologyProjectionRegistry.ProjectedActor nearest = null;
        double nearestDistance = Double.POSITIVE_INFINITY;
        for (var candidate : allActors) {
            if (candidate.actor().getUuid().equals(actor.actor().getUuid())) continue;
            if (!candidate.populationKey().equals(actor.populationKey())) continue;
            if (candidate.actor().isRemoved() || candidate.actor().isInvisible()) continue;
            double distance = actor.actor().squaredDistanceTo(candidate.actor());
            if (distance < nearestDistance) {
                nearestDistance = distance;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private static void applyCalmRoaming(
            WildEcologyProjectionRegistry.ProjectedActor projection,
            WildEcologyProjectionRegistry.ProjectedActor nearestSibling,
            long worldTime
    ) {
        PokemonEntity actor = projection.actor();
        WildBehaviorProfile profile = projection.behaviorProfile();
        double[] target = calmRoamingTarget(
                actor.getUuid(),
                worldTime,
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                projection.habitatLeashRadiusBlocks(),
                profile.calmSegmentTicks());
        double dx = target[0] - actor.getX();
        double dz = target[1] - actor.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double requestedX = 0.0D;
        double requestedZ = 0.0D;
        if (profile.calmMovementActive(worldTime) && distance > profile.calmStopDistance()) {
            requestedX = (dx / distance) * profile.calmRoamSpeed();
            requestedZ = (dz / distance) * profile.calmRoamSpeed();
        }

        if (nearestSibling != null) {
            PokemonEntity sibling = nearestSibling.actor();
            double[] separation = pairImpulse(
                    actor.getUuid(), actor.getX(), actor.getZ(),
                    sibling.getUuid(), sibling.getX(), sibling.getZ(),
                    profile.separationDistance(), profile.separationSpeed(), false);
            requestedX += separation[0];
            requestedZ += separation[1];

            double[] cohesion = pairImpulse(
                    actor.getUuid(), actor.getX(), actor.getZ(),
                    sibling.getUuid(), sibling.getX(), sibling.getZ(),
                    profile.cohesionDistance(), profile.cohesionSpeed(), true);
            requestedX += cohesion[0];
            requestedZ += cohesion[1];
        }

        if (Math.abs(requestedX) <= 0.000001D && Math.abs(requestedZ) <= 0.000001D) return;
        double[] bounded = boundedHorizontalVelocity(requestedX, requestedZ, profile.calmRoamSpeed());
        if (!insideLeashAfterImpulse(
                actor,
                projection.habitatCenterX(),
                projection.habitatCenterZ(),
                projection.habitatLeashRadiusBlocks(),
                bounded[0],
                bounded[1])) return;
        actor.setYaw((float) Math.toDegrees(Math.atan2(-bounded[0], bounded[1])));
        setAmbientHorizontalVelocity(actor, bounded[0], bounded[1], profile.calmRoamSpeed());
    }

    static double[] calmRoamingTarget(
            UUID actorId,
            long worldTime,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            long segmentTicks
    ) {
        if (actorId == null
                || !Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || leashRadiusBlocks <= 0 || segmentTicks <= 0L) {
            throw new IllegalArgumentException("calm roaming target requires actor, finite center, positive leash and cadence");
        }
        long segment = Math.floorDiv(worldTime, segmentTicks);
        long mixed = actorId.getMostSignificantBits()
                ^ Long.rotateLeft(actorId.getLeastSignificantBits(), 17)
                ^ (segment * 0x9E3779B97F4A7C15L);
        double angleUnit = ((mixed >>> 11) & 0x1FFFFFL) / (double) 0x1FFFFF;
        double radiusUnit = ((Long.rotateLeft(mixed, 29) >>> 11) & 0x1FFFFFL) / (double) 0x1FFFFF;
        double angle = angleUnit * Math.PI * 2.0D;
        double radius = Math.max(1.0D, leashRadiusBlocks * (0.25D + radiusUnit * 0.55D));
        return new double[] {centerX + Math.cos(angle) * radius, centerZ + Math.sin(angle) * radius};
    }

    static double[] pairImpulse(
            UUID actorId,
            double actorX,
            double actorZ,
            UUID siblingId,
            double siblingX,
            double siblingZ,
            double distanceBound,
            double speedBound,
            boolean cohesion
    ) {
        if (actorId == null || siblingId == null
                || !Double.isFinite(actorX) || !Double.isFinite(actorZ)
                || !Double.isFinite(siblingX) || !Double.isFinite(siblingZ)
                || !Double.isFinite(distanceBound) || distanceBound <= 0.0D
                || !Double.isFinite(speedBound) || speedBound <= 0.0D) {
            throw new IllegalArgumentException("pair impulse requires identities, finite coordinates and positive bounds");
        }
        if (actorId.equals(siblingId)) return new double[] {0.0D, 0.0D};

        double dx = siblingX - actorX;
        double dz = siblingZ - actorZ;
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (cohesion) {
            if (distance <= distanceBound || distance <= 0.001D) return new double[] {0.0D, 0.0D};
            double strength = Math.min(1.0D, (distance - distanceBound) / distanceBound);
            return new double[] {(dx / distance) * speedBound * strength, (dz / distance) * speedBound * strength};
        }

        if (distance >= distanceBound) return new double[] {0.0D, 0.0D};
        if (distance <= 0.001D) {
            long mixed = actorId.getMostSignificantBits()
                    ^ actorId.getLeastSignificantBits()
                    ^ Long.rotateLeft(siblingId.getMostSignificantBits(), 13)
                    ^ Long.rotateLeft(siblingId.getLeastSignificantBits(), 31);
            double angleUnit = ((mixed >>> 11) & 0x1FFFFFL) / (double) 0x1FFFFF;
            double angle = angleUnit * Math.PI * 2.0D;
            return new double[] {Math.cos(angle) * speedBound, Math.sin(angle) * speedBound};
        }
        double strength = (distanceBound - distance) / distanceBound;
        return new double[] {(-dx / distance) * speedBound * strength, (-dz / distance) * speedBound * strength};
    }

    static double[] boundedHorizontalVelocity(double requestedX, double requestedZ, double maxSpeed) {
        if (!Double.isFinite(requestedX) || !Double.isFinite(requestedZ)
                || !Double.isFinite(maxSpeed) || maxSpeed <= 0.0D) {
            throw new IllegalArgumentException("ambient velocity requires finite components and positive max speed");
        }
        double speed = Math.sqrt(requestedX * requestedX + requestedZ * requestedZ);
        if (speed <= maxSpeed || speed <= 0.001D) return new double[] {requestedX, requestedZ};
        double scale = maxSpeed / speed;
        return new double[] {requestedX * scale, requestedZ * scale};
    }

    static double[] recoveryImpulse(double actorX, double actorZ, double centerX, double centerZ, double speed) {
        if (!Double.isFinite(actorX) || !Double.isFinite(actorZ)
                || !Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || !Double.isFinite(speed) || speed <= 0.0D) {
            throw new IllegalArgumentException("recovery impulse requires finite coordinates and positive speed");
        }
        double dx = centerX - actorX;
        double dz = centerZ - actorZ;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length <= 0.001D) return new double[] {0.0D, 0.0D};
        return new double[] {(dx / length) * speed, (dz / length) * speed};
    }

    static boolean insideHorizontalLeash(
            double actorX,
            double actorZ,
            double centerX,
            double centerZ,
            int leashRadiusBlocks
    ) {
        if (!Double.isFinite(actorX) || !Double.isFinite(actorZ)
                || !Double.isFinite(centerX) || !Double.isFinite(centerZ)
                || leashRadiusBlocks <= 0) {
            throw new IllegalArgumentException("habitat leash requires finite coordinates and positive radius");
        }
        double dx = actorX - centerX;
        double dz = actorZ - centerZ;
        return dx * dx + dz * dz <= (double) leashRadiusBlocks * leashRadiusBlocks;
    }

    private static boolean insideLeashAfterImpulse(
            PokemonEntity actor,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double velocityX,
            double velocityZ
    ) {
        return insideHorizontalLeash(
                actor.getX() + velocityX,
                actor.getZ() + velocityZ,
                centerX,
                centerZ,
                leashRadiusBlocks);
    }

    private static void applyRecovery(PokemonEntity actor, double centerX, double centerZ, WildBehaviorProfile profile) {
        double dx = centerX - actor.getX();
        double dz = centerZ - actor.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= profile.recoveryStopDistance()) return;
        double[] recovery = recoveryImpulse(actor.getX(), actor.getZ(), centerX, centerZ, profile.recoverySpeed());
        if (recovery[0] == 0.0D && recovery[1] == 0.0D) return;
        actor.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        setAmbientHorizontalVelocity(actor, recovery[0], recovery[1], profile.recoverySpeed());
    }

    private static void setAmbientHorizontalVelocity(PokemonEntity actor, double requestedX, double requestedZ, double maxSpeed) {
        double[] bounded = boundedHorizontalVelocity(requestedX, requestedZ, maxSpeed);
        actor.setVelocity(bounded[0], actor.getVelocity().y, bounded[1]);
        actor.velocityModified = true;
    }

    private static Map<UUID, AmbientPokemonBehaviorController> controllersFor(MinecraftServer server) {
        synchronized (CONTROLLERS) {
            return CONTROLLERS.computeIfAbsent(server, ignored -> new HashMap<>());
        }
    }
}

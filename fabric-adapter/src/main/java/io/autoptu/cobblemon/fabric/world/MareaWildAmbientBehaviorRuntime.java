package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Minecraft-only ambient presentation for canonical Marea roaming actors.
 *
 * This runtime consumes only server-observed player proximity plus authored presentation thresholds.
 * It never reads Cobblemon Pokemon gameplay payload, PTU stats, HP, moves, statuses, abilities,
 * battle state, encounter legality, RNG or results. Combat authority remains entirely upstream.
 */
public final class MareaWildAmbientBehaviorRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 10;
    private static final double FLEE_SPEED = 0.08D;
    private static final double RECOVERY_SPEED = 0.04D;
    private static final double RECOVERY_STOP_DISTANCE = 1.5D;
    private static final AmbientPokemonBehaviorController.Profile MAREA_ROAMING_PROFILE =
            new AmbientPokemonBehaviorController.Profile(14.0D, 7.0D, 3, 5);
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
        Map<UUID, AmbientPokemonBehaviorController> controllers = controllersFor(server);
        var liveActors = new java.util.HashSet<UUID>();

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var entity = world.getEntity(boundUuid.get());
                if (!(entity instanceof PokemonEntity actor) || actor.isRemoved()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid()) || actor.isInvisible()) continue;

                liveActors.add(actor.getUuid());
                var nearest = nearestPlayer(world, actor);
                AmbientPokemonBehaviorController controller = controllers.computeIfAbsent(
                        actor.getUuid(), ignored -> new AmbientPokemonBehaviorController(MAREA_ROAMING_PROFILE));
                var state = controller.update(
                        nearest == null ? Double.POSITIVE_INFINITY : Math.sqrt(actor.squaredDistanceTo(nearest)),
                        nearest != null
                );
                applyPresentation(actor, nearest, state, encounter, population, projectedSiteId.get());
            }
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
            PokemonEntity actor,
            ServerPlayerEntity nearest,
            AmbientPokemonBehaviorController.State state,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter,
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            String projectedSiteId
    ) {
        BlockPos anchor = MareaVisibleWildPokemonRuntime.projectedPresentationAnchor(encounter, projectedSiteId);
        double centerX = anchor.getX() + 0.5D;
        double centerZ = anchor.getZ() + 0.5D;

        if (state == AmbientPokemonBehaviorController.State.RECOVERING) {
            applyRecovery(actor, centerX, centerZ);
            return;
        }
        if (nearest == null) return;

        double dx = nearest.getX() - actor.getX();
        double dz = nearest.getZ() - actor.getZ();
        float towardPlayerYaw = (float) (Math.toDegrees(Math.atan2(-dx, dz)));

        if (state == AmbientPokemonBehaviorController.State.WATCHING) {
            actor.setYaw(towardPlayerYaw);
            return;
        }

        if (state == AmbientPokemonBehaviorController.State.ALARMED) {
            actor.setYaw(towardPlayerYaw + 180.0F);
            double length = Math.sqrt(dx * dx + dz * dz);
            if (length <= 0.001D) return;

            double fleeX = (-dx / length) * FLEE_SPEED;
            double fleeZ = (-dz / length) * FLEE_SPEED;
            if (!insideLeashAfterImpulse(actor, centerX, centerZ, population.habitatLeashRadiusBlocks(), fleeX, fleeZ)) {
                double[] recovery = recoveryImpulse(actor.getX(), actor.getZ(), centerX, centerZ, FLEE_SPEED);
                fleeX = recovery[0];
                fleeZ = recovery[1];
                if (fleeX == 0.0D && fleeZ == 0.0D) return;
            }

            actor.addVelocity(fleeX, 0.0D, fleeZ);
            actor.velocityModified = true;
        }
    }

    private static void applyRecovery(PokemonEntity actor, double centerX, double centerZ) {
        double dx = centerX - actor.getX();
        double dz = centerZ - actor.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        if (distance <= RECOVERY_STOP_DISTANCE) return;

        double[] recovery = recoveryImpulse(actor.getX(), actor.getZ(), centerX, centerZ, RECOVERY_SPEED);
        if (recovery[0] == 0.0D && recovery[1] == 0.0D) return;
        actor.setYaw((float) Math.toDegrees(Math.atan2(-dx, dz)));
        actor.addVelocity(recovery[0], 0.0D, recovery[1]);
        actor.velocityModified = true;
    }

    static double[] recoveryImpulse(
            double actorX,
            double actorZ,
            double centerX,
            double centerZ,
            double speed
    ) {
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

    static boolean insideLeashAfterImpulse(
            PokemonEntity actor,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double velocityX,
            double velocityZ
    ) {
        if (actor == null) return false;
        double nextDx = actor.getX() + velocityX - centerX;
        double nextDz = actor.getZ() + velocityZ - centerZ;
        return nextDx * nextDx + nextDz * nextDz <= (double) leashRadiusBlocks * leashRadiusBlocks;
    }

    static int controllerCount(MinecraftServer server) {
        synchronized (CONTROLLERS) {
            var controllers = CONTROLLERS.get(server);
            return controllers == null ? 0 : controllers.size();
        }
    }

    private static Map<UUID, AmbientPokemonBehaviorController> controllersFor(MinecraftServer server) {
        synchronized (CONTROLLERS) {
            return CONTROLLERS.computeIfAbsent(server, ignored -> new HashMap<>());
        }
    }
}

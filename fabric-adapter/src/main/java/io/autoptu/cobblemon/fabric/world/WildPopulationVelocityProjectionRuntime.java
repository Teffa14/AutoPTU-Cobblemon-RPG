package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Suspends residual Minecraft locomotion and native combat presentation state while a canonical WILD actor is dormant.
 *
 * <p>Velocity, native navigation, vanilla/Cobblemon targets, attacker memory and vanilla fire are presentation/world
 * projection state only. AutoPTU-Java remains authoritative for tactical movement, targeting, forced movement,
 * reactions, damage, statuses and battle outcomes. Hidden or interaction-inactive actors must not keep following a
 * stale path, drift away from their server-authored ecology projection, retain native combat memory, or keep a vanilla
 * burn presentation alive across hibernation.</p>
 */
public final class WildPopulationVelocityProjectionRuntime implements ModInitializer {
    private static final double EPSILON_SQUARED = 1.0e-8;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationVelocityProjectionRuntime::synchronizeAllWorlds);
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null) return;
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int suspendedActors = 0;
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            boolean dormant = shouldSuspendPresentation(interactionActive, actor.isInvisible());
            if (!dormant) continue;

            boolean changed = false;
            var navigation = actor.getNavigation();
            if (!navigation.isIdle()) {
                navigation.stop();
                changed = true;
            }

            if (actor.getTarget() != null) {
                actor.setTarget(null);
                changed = true;
            }

            if (actor.getAttacker() != null) {
                actor.setAttacker(null);
                changed = true;
            }

            if (actor.isOnFire()) {
                actor.extinguish();
                changed = true;
            }

            Vec3d velocity = actor.getVelocity();
            if (hasResidualMotion(velocity.lengthSquared())) {
                actor.setVelocity(Vec3d.ZERO);
                actor.velocityModified = true;
                changed = true;
            }

            if (changed) suspendedActors++;
        }
        return suspendedActors;
    }

    static boolean shouldSuspendPresentation(boolean interactionActive, boolean invisible) {
        return !interactionActive || invisible;
    }

    static boolean hasResidualMotion(double velocitySquared) {
        return velocitySquared > EPSILON_SQUARED;
    }

    static boolean shouldStopResidualMotion(boolean interactionActive, boolean invisible, double velocitySquared) {
        return shouldSuspendPresentation(interactionActive, invisible) && hasResidualMotion(velocitySquared);
    }

    static boolean shouldClearFire(boolean interactionActive, boolean invisible, boolean onFire) {
        return shouldSuspendPresentation(interactionActive, invisible) && onFire;
    }
}

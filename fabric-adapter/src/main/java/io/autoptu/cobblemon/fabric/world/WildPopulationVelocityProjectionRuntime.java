package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Suspends residual Minecraft locomotion while a canonical WILD presentation actor is dormant.
 *
 * <p>Velocity and native navigation are presentation/world projection state only. AutoPTU-Java remains
 * authoritative for tactical movement, forced movement, reactions and battle outcomes. Hidden or
 * interaction-inactive actors must not keep following a stale vanilla/Cobblemon path or drift away from their
 * server-authored ecology projection.</p>
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
}

package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.Vec3d;

/**
 * Stops residual Minecraft motion while a canonical WILD presentation actor is dormant.
 *
 * <p>Entity velocity here is presentation/world projection state only. AutoPTU-Java remains authoritative for
 * tactical movement, forced movement, reactions and battle outcomes. Hidden or interaction-inactive actors must
 * not drift away from their server-authored ecology projection because of stale vanilla/Cobblemon velocity.</p>
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
        int stoppedActors = 0;
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            Vec3d velocity = actor.getVelocity();
            if (shouldStopResidualMotion(interactionActive, actor.isInvisible(), velocity.lengthSquared())) {
                actor.setVelocity(Vec3d.ZERO);
                actor.velocityModified = true;
                stoppedActors++;
            }
        }
        return stoppedActors;
    }

    static boolean shouldStopResidualMotion(boolean interactionActive, boolean invisible, double velocitySquared) {
        return (!interactionActive || invisible) && velocitySquared > EPSILON_SQUARED;
    }
}

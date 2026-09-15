package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Clears vanilla Minecraft fire from dormant canonical WILD presentation actors.
 *
 * <p>AutoPTU-Java remains authoritative for battle damage, statuses and outcomes. Fire on a hidden or otherwise
 * interaction-inactive Cobblemon body is presentation/world state only and must not survive suspension to produce
 * phantom burning particles, sounds or vanilla damage when the canonical WILD actor resumes.</p>
 */
public final class WildPopulationFireProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationFireProjectionRuntime::synchronizeAllWorlds);
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null) return;
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int clearedActors = 0;
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            if (shouldClearFire(interactionActive, actor.isInvisible(), actor.isOnFire())) {
                actor.extinguish();
                clearedActors++;
            }
        }
        return clearedActors;
    }

    static boolean shouldClearFire(boolean interactionActive, boolean invisible, boolean onFire) {
        return onFire && (!interactionActive || invisible);
    }
}

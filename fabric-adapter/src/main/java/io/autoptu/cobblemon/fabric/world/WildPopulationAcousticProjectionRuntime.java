package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Keeps ambient Cobblemon audio aligned with the server-owned WILD presence projection.
 *
 * <p>Canonical encounter identity and interaction eligibility remain authoritative elsewhere. This
 * runtime only projects that state into Minecraft sound: hibernating or otherwise inactive actors
 * are muted, while visible interaction-active actors regain normal Cobblemon audio.</p>
 */
public final class WildPopulationAcousticProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationAcousticProjectionRuntime::synchronizeAllWorlds);
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null
                || server.getTicks() % WildPopulationRuntime.presenceReconcileIntervalTicks() != 0) {
            return;
        }
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int synchronizedActors = 0;
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            actor.setSilent(shouldMute(interactionActive, actor.isInvisible()));
            synchronizedActors++;
        }
        return synchronizedActors;
    }

    static boolean shouldMute(boolean interactionActive, boolean invisible) {
        return !interactionActive || invisible;
    }
}

package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Keeps Minecraft collision aligned with the server-owned WILD presence projection.
 *
 * <p>Canonical encounter identity and interaction eligibility remain authoritative elsewhere. This runtime only
 * projects dormant presence into Minecraft physics: hibernating or otherwise inactive visible-WILD presentation
 * actors stop participating in collision, while active visible actors regain normal collision. It does not derive
 * PTU movement legality, forced movement, initiative, damage, HP, statuses, capture or battle outcomes.</p>
 */
public final class WildPopulationCollisionProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationCollisionProjectionRuntime::synchronizeAllWorlds);
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
            actor.noClip = shouldDisableCollision(interactionActive, actor.isInvisible());
            synchronizedActors++;
        }
        return synchronizedActors;
    }

    static boolean shouldDisableCollision(boolean interactionActive, boolean invisible) {
        return !interactionActive || invisible;
    }
}

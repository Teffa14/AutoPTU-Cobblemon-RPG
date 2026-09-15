package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Clears stale vanilla sneak posture from dormant canonical WILD actors.
 *
 * <p>Sneaking is Minecraft presentation state only. AutoPTU-Java remains authoritative for tactical movement,
 * movement legality, reactions, statuses, damage and battle outcomes.</p>
 */
public final class WildPopulationPostureProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationPostureProjectionRuntime::synchronizeAllWorlds);
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null) return;
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int resetActors = 0;
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            if (shouldClearSneak(interactionActive, actor.isInvisible(), actor.isSneaking())) {
                actor.setSneaking(false);
                resetActors++;
            }
        }
        return resetActors;
    }

    static boolean shouldClearSneak(boolean interactionActive, boolean invisible, boolean sneaking) {
        return sneaking && (!interactionActive || invisible);
    }
}

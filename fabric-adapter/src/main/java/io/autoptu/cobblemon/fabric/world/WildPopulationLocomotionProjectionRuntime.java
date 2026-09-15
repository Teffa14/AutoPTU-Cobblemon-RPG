package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Clears vanilla sprint presentation from dormant canonical WILD actors.
 *
 * <p>Sprinting on a Cobblemon entity is Minecraft presentation state. AutoPTU-Java remains authoritative for
 * tactical movement, movement legality, reactions and battle outcomes. Hidden or interaction-inactive actors must
 * not keep a stale sprint pose/particle state while canonical ecology presence is suspended.</p>
 */
public final class WildPopulationLocomotionProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationLocomotionProjectionRuntime::synchronizeAllWorlds);
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
            if (shouldClearSprint(interactionActive, actor.isInvisible(), actor.isSprinting())) {
                actor.setSprinting(false);
                resetActors++;
            }
        }
        return resetActors;
    }

    static boolean shouldClearSprint(boolean interactionActive, boolean invisible, boolean sprinting) {
        return sprinting && (!interactionActive || invisible);
    }
}

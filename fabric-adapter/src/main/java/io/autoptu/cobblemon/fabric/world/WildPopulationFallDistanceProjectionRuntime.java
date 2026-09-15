package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Prevents dormant canonical WILD presentation actors from carrying vanilla fall-distance debt.
 *
 * <p>AutoPTU-Java remains authoritative for battle HP, damage and outcomes. Fall distance on a hidden or otherwise
 * interaction-inactive Cobblemon body is Minecraft presentation/world state only. A suspended actor must not resume
 * and receive delayed vanilla fall damage because presentation physics accumulated distance while it was dormant.</p>
 */
public final class WildPopulationFallDistanceProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationFallDistanceProjectionRuntime::synchronizeAllWorlds);
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
            if (shouldResetFallDistance(interactionActive, actor.isInvisible(), actor.fallDistance)) {
                actor.fallDistance = 0.0F;
                resetActors++;
            }
        }
        return resetActors;
    }

    static boolean shouldResetFallDistance(boolean interactionActive, boolean invisible, float fallDistance) {
        return (!interactionActive || invisible) && fallDistance > 0.0F;
    }
}

package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Keeps dormant canonical WILD presentation actors from consuming vanilla Minecraft air.
 *
 * <p>AutoPTU-Java remains authoritative for battle HP, statuses, damage and outcomes. Air on a hidden or otherwise
 * interaction-inactive Cobblemon body is Minecraft presentation/world state only. A suspended actor must not drown
 * off-screen or resume with depleted vanilla air because its canonical WILD identity was hibernating underwater.</p>
 */
public final class WildPopulationAirProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationAirProjectionRuntime::synchronizeAllWorlds);
    }

    private static void synchronizeAllWorlds(MinecraftServer server) {
        if (server == null) return;
        for (ServerWorld world : server.getWorlds()) synchronize(world);
    }

    static int synchronize(ServerWorld world) {
        if (world == null) throw new IllegalArgumentException("world is required");
        int restoredActors = 0;
        for (var projected : WildEcologyProjectionSource.collect(world)) {
            var actor = projected.actor();
            boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
            if (shouldRestoreAir(interactionActive, actor.isInvisible(), actor.getAir(), actor.getMaxAir())) {
                actor.setAir(actor.getMaxAir());
                restoredActors++;
            }
        }
        return restoredActors;
    }

    static boolean shouldRestoreAir(boolean interactionActive, boolean invisible, int air, int maxAir) {
        return (!interactionActive || invisible) && air < maxAir;
    }
}

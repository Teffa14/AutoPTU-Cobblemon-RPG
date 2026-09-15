package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Prevents dormant canonical WILD presentation actors from carrying vanilla freezing state.
 *
 * <p>AutoPTU-Java remains authoritative for battle HP, statuses, damage and outcomes. Powder-snow frozen ticks on a
 * hidden or otherwise interaction-inactive Cobblemon body are Minecraft presentation/world state only. A suspended
 * actor must not resume with accumulated vanilla freeze visuals or delayed environmental consequences.</p>
 */
public final class WildPopulationFreezeProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationFreezeProjectionRuntime::synchronizeAllWorlds);
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
            if (shouldResetFrozenTicks(interactionActive, actor.isInvisible(), actor.getFrozenTicks())) {
                actor.setFrozenTicks(0);
                resetActors++;
            }
        }
        return resetActors;
    }

    static boolean shouldResetFrozenTicks(boolean interactionActive, boolean invisible, int frozenTicks) {
        return (!interactionActive || invisible) && frozenTicks > 0;
    }
}

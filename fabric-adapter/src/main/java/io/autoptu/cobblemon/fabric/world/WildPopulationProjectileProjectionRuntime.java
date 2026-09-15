package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Clears vanilla projectile-embedding presentation from dormant canonical WILD actors.
 *
 * <p>Embedded-arrow state belongs to the Minecraft presentation body. AutoPTU-Java remains authoritative for battle
 * damage, HP, statuses and outcomes. A hidden or otherwise interaction-inactive Cobblemon actor must not preserve
 * vanilla arrow visuals that can leak stale Minecraft combat presentation while the canonical WILD is suspended.</p>
 */
public final class WildPopulationProjectileProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationProjectileProjectionRuntime::synchronizeAllWorlds);
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
            if (shouldClearEmbeddedArrows(interactionActive, actor.isInvisible(), actor.getStuckArrowCount())) {
                actor.setStuckArrowCount(0);
                resetActors++;
            }
        }
        return resetActors;
    }

    static boolean shouldClearEmbeddedArrows(boolean interactionActive, boolean invisible, int stuckArrowCount) {
        return (!interactionActive || invisible) && stuckArrowCount > 0;
    }
}

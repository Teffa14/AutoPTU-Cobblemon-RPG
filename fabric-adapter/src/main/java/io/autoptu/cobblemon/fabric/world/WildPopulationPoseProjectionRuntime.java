package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Clears vanilla hurt presentation from dormant canonical WILD actors.
 *
 * <p>Hurt-time and hurt-tilt are Minecraft presentation state. AutoPTU-Java remains authoritative for battle
 * damage, HP, statuses and outcomes. A hidden or otherwise interaction-inactive Cobblemon actor must not preserve
 * stale vanilla hurt flashes or tilt while canonical ecology presence is suspended.</p>
 */
public final class WildPopulationPoseProjectionRuntime implements ModInitializer {
    @Override
    public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(WildPopulationPoseProjectionRuntime::synchronizeAllWorlds);
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
            if (shouldClearHurtPresentation(interactionActive, actor.isInvisible(), actor.hurtTime)) {
                actor.hurtTime = 0;
                actor.hurtDuration = 0;
                actor.lastDamageTaken = 0.0F;
                resetActors++;
            }
        }
        return resetActors;
    }

    static boolean shouldClearHurtPresentation(boolean interactionActive, boolean invisible, int hurtTime) {
        return (!interactionActive || invisible) && hurtTime > 0;
    }
}

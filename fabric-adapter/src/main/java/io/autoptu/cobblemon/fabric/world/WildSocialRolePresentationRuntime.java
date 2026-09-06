package io.autoptu.cobblemon.fabric.world;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;

/**
 * Projects explicitly server-authored wild social roles into Minecraft-only ambient visuals.
 *
 * <p>Alpha designation comes only from the registered ecology descriptor. This runtime never reads
 * species, level, stats, moves, abilities, HP, Cobblemon BattleState, encounter priority or PTU
 * mechanics. The particle marker is presentation-only.</p>
 */
public final class WildSocialRolePresentationRuntime implements ModInitializer {
    private static final int UPDATE_INTERVAL_TICKS = 20;

    @Override
    public void onInitialize() {
        ServerTickEvents.END_WORLD_TICK.register(world -> {
            if (world.getTime() % UPDATE_INTERVAL_TICKS != 0L) return;
            project(world);
        });
    }

    static int project(ServerWorld world) {
        if (world == null) return 0;
        int projected = 0;
        for (var projection : WildEcologyProjectionRegistry.collect(world)) {
            if (projection.socialRole() != WildSocialRole.ALPHA) continue;
            var actor = projection.actor();
            if (actor.isRemoved() || actor.isInvisible()) continue;
            if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

            world.spawnParticles(
                    ParticleTypes.END_ROD,
                    actor.getX(),
                    actor.getY() + actor.getHeight() + 0.35D,
                    actor.getZ(),
                    2,
                    0.18D,
                    0.08D,
                    0.18D,
                    0.005D);
            projected++;
        }
        return projected;
    }
}

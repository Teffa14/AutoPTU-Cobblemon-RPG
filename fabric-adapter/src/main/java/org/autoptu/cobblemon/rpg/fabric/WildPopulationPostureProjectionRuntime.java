package org.autoptu.cobblemon.rpg.fabric;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

/**
 * Keeps vanilla posture presentation from leaking through canonical WILD hibernation.
 *
 * <p>This runtime owns presentation only. It never derives PTU movement legality,
 * statuses, damage, or battle outcomes from Minecraft state.</p>
 */
public final class WildPopulationPostureProjectionRuntime {
    private WildPopulationPostureProjectionRuntime() {}

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerWorld world : server.getWorlds()) {
            for (PokemonEntity entity : world.getEntitiesByClass(PokemonEntity.class, entity -> true)) {
                var projection = WildEcologyProjectionSource.resolve(world, entity.getUuid());
                if (projection.isEmpty() || projection.get().isInteractionActive()) {
                    continue;
                }
                if (entity.isSneaking()) {
                    entity.setSneaking(false);
                }
            }
        }
    }
}

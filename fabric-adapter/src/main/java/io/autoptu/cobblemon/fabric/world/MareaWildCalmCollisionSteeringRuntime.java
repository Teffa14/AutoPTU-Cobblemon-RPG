package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.entity.ai.pathing.Path;

/**
 * Transitional compatibility facade for Marea CALM continuity while WORLD-013 migrates the final
 * regional caller to the world-wide navigation runtime. This class owns no Fabric initializer,
 * tick loop, actor discovery, PTU movement legality, RNG, combat state, or outcomes.
 */
final class MareaWildCalmCollisionSteeringRuntime {
    private MareaWildCalmCollisionSteeringRuntime() {
    }

    static Path findLeashSafeNativePath(
            PokemonEntity actor,
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double[] target
    ) {
        return WildCalmCollisionNavigationRuntime.findLeashSafeNativePath(
                actor, centerX, centerZ, leashRadiusBlocks, target);
    }

    static boolean navigationTargetInsideLeash(
            double centerX,
            double centerZ,
            int leashRadiusBlocks,
            double targetX,
            double targetZ
    ) {
        return WildCalmCollisionNavigationRuntime.navigationTargetInsideLeash(
                centerX, centerZ, leashRadiusBlocks, targetX, targetZ);
    }

    static boolean stableCalmSurfaceNeighborhood(int surfaceY, int... adjacentSurfaceY) {
        return WildCalmCollisionNavigationRuntime.stableCalmSurfaceNeighborhood(surfaceY, adjacentSurfaceY);
    }
}

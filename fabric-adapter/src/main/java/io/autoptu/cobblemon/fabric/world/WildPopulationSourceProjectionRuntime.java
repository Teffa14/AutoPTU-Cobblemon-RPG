package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.minecraft.server.world.ServerWorld;

/**
 * Explicit projection boundary for one server-authored ecology source.
 *
 * <p>This is intended for authored world/bootstrap and runtime-evidence surfaces that need to
 * materialize one ecology source without reintroducing region-owned lifecycle logic. Normal
 * player-driven activation remains owned by {@link WildPopulationRuntime}.</p>
 */
public final class WildPopulationSourceProjectionRuntime {
    private WildPopulationSourceProjectionRuntime() {}

    public static int ensureProjected(ServerWorld world, String sourceId) {
        if (world == null) throw new IllegalArgumentException("world is required");
        if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId is required");

        // Explicit source projection is a production entry point, not a region-owned lifecycle.
        // Ensure the global lifecycle is installed before any presentation actor can be revealed.
        WildPopulationRuntime.register();

        String normalizedSourceId = sourceId.strip();
        int visible = 0;
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (descriptor == null || !normalizedSourceId.equals(descriptor.sourceId())) continue;
            if (!descriptor.worldEligibility().accepts(world)) continue;
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                // The source-level bootstrap must preserve the same server-authored visibility gate as
                // normal population activation. A runtime/admin projection request cannot reveal an actor
                // that its ecology descriptor currently marks ineligible for world presentation.
                if (!descriptor.projectionEligibility().test(encounter)) continue;
                if (WildPopulationRuntime.ensureProjected(world, encounter) != null) visible++;
            }
        }
        return visible;
    }
}

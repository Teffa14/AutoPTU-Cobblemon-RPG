package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;

import java.util.Optional;

/**
 * Temporary compatibility bridge for legacy Marea fixture runtimes.
 *
 * <p>All authored migration data lives in {@link MareaWildEcologyContent}; all projection semantics
 * live in {@link WildPopulationProjectionProfile} and {@link WildPopulationContentRegistry}. This
 * bridge owns no schedule, lifecycle, RNG, PTU rule or Minecraft authority and is not a Fabric
 * production entrypoint. Remove it after the remaining legacy Marea fixture callers consume the
 * registered content resolver directly.</p>
 */
@Deprecated(forRemoval = true)
final class MareaWildMigrationProjection {
    private static final WildPopulationContentRegistry.ProjectedSiteResolver RESOLVER =
            WildPopulationContentRegistry.projectionResolver(MareaWildEcologyContent.projectionProfiles());

    private MareaWildMigrationProjection() {}

    static Optional<String> projectedSiteId(
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            long worldTick
    ) {
        return RESOLVER.projectedSiteId(population, worldTick);
    }
}

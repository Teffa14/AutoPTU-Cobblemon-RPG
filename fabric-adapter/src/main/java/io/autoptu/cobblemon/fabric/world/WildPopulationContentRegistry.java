package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;

import java.util.Optional;

/**
 * Temporary compatibility facade for older Marea presentation runtimes.
 *
 * <p>This type owns no registry and stores no content. Projection policy is resolved exclusively from
 * {@link WildEcologyDescriptorRegistry}; callers without an authored descriptor fall back to the canonical
 * population home site. Remove this facade after the remaining Marea callers migrate directly.</p>
 */
@Deprecated(forRemoval = true)
public final class WildPopulationContentRegistry {
    private WildPopulationContentRegistry() {}

    public static Optional<String> projectedSiteId(
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            long worldTick
    ) {
        if (population == null) throw new IllegalArgumentException("population is required");
        return WildEcologyDescriptorRegistry.descriptorFor(population)
                .flatMap(descriptor -> descriptor.projectedSiteId(population, worldTick))
                .or(() -> Optional.of(population.siteId()));
    }
}

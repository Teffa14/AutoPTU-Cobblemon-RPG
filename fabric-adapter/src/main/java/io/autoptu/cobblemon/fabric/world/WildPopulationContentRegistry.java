package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Temporary compatibility facade for older Marea presentation runtimes and projection-profile tests.
 *
 * <p>This type owns no registry and stores no content. Production projection policy is resolved exclusively
 * from {@link WildEcologyDescriptorRegistry}; callers without an authored descriptor fall back to the canonical
 * population home site. The list resolver is a pure fixture helper and does not publish runtime policy.</p>
 */
@Deprecated(forRemoval = true)
public final class WildPopulationContentRegistry {
    @FunctionalInterface
    public interface ProjectedSiteResolver {
        Optional<String> projectedSiteId(
                CanonicalWildPopulationCatalogue.PopulationDefinition population,
                long worldTick
        );
    }

    private WildPopulationContentRegistry() {}

    public static ProjectedSiteResolver projectionResolver(List<WildPopulationProjectionProfile> profiles) {
        if (profiles == null) throw new IllegalArgumentException("profiles are required");
        Map<String, WildPopulationProjectionProfile> byPopulation = new LinkedHashMap<>();
        for (WildPopulationProjectionProfile profile : profiles) {
            if (profile == null) throw new IllegalArgumentException("projection profile is required");
            WildPopulationProjectionProfile previous = byPopulation.putIfAbsent(profile.populationId(), profile);
            if (previous != null) {
                throw new IllegalArgumentException("multiple projection profiles for population: " + profile.populationId());
            }
        }
        Map<String, WildPopulationProjectionProfile> immutableProfiles = Map.copyOf(byPopulation);
        return (population, worldTick) -> {
            if (population == null) throw new IllegalArgumentException("population is required");
            WildPopulationProjectionProfile profile = immutableProfiles.get(population.populationId());
            return profile == null
                    ? Optional.of(population.siteId())
                    : profile.projectedSiteId(population, worldTick);
        };
    }

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

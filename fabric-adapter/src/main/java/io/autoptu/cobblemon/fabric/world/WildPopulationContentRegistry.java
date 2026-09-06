package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.fabric.battle.CanonicalWildEncounterBlueprintSource;
import net.minecraft.server.world.ServerWorld;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Compatibility view over server-owned visible-wild population policy.
 *
 * <p>Normal production content is registered once through {@link WildEcologyDescriptorRegistry}. The legacy
 * register method remains for isolated tests/fixtures while generic runtimes migrate without changing their
 * already-proven lifecycle boundary.</p>
 */
public final class WildPopulationContentRegistry {
    @FunctionalInterface
    public interface WorldEligibility {
        boolean accepts(ServerWorld world);
    }

    @FunctionalInterface
    public interface ProjectedSiteResolver {
        Optional<String> projectedSiteId(
                CanonicalWildPopulationCatalogue.PopulationDefinition population,
                long worldTick
        );
    }

    public record Source(
            String sourceId,
            Predicate<CanonicalWildPopulationCatalogue.PopulationDefinition> populationSelector,
            WorldEligibility worldEligibility,
            ProjectedSiteResolver projectedSiteResolver,
            CanonicalWildEncounterBlueprintSource blueprintSource,
            Predicate<CanonicalWildEncounterCatalogue.EncounterDefinition> projectionEligibility
    ) {
        public Source {
            if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId is required");
            sourceId = sourceId.strip();
            if (populationSelector == null) throw new IllegalArgumentException("populationSelector is required");
            if (worldEligibility == null) throw new IllegalArgumentException("worldEligibility is required");
            if (projectedSiteResolver == null) throw new IllegalArgumentException("projectedSiteResolver is required");
            if (blueprintSource == null) throw new IllegalArgumentException("blueprintSource is required");
            if (projectionEligibility == null) throw new IllegalArgumentException("projectionEligibility is required");
        }
    }

    private static final Map<String, Source> LEGACY_SOURCES = new LinkedHashMap<>();

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
        return sourceFor(population)
                .map(source -> source.projectedSiteResolver().projectedSiteId(population, worldTick))
                .orElseGet(() -> Optional.of(population.siteId()));
    }

    public static synchronized void register(Source source) {
        if (source == null) throw new IllegalArgumentException("source is required");
        Source previous = LEGACY_SOURCES.putIfAbsent(source.sourceId(), source);
        if (previous != null && previous != source) {
            throw new IllegalStateException("wild population source already registered: " + source.sourceId());
        }
    }

    public static synchronized Optional<Source> sourceFor(
            CanonicalWildPopulationCatalogue.PopulationDefinition population
    ) {
        if (population == null) return Optional.empty();
        var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
        if (descriptor != null) return Optional.of(fromDescriptor(descriptor));

        Source match = null;
        for (Source source : LEGACY_SOURCES.values()) {
            if (!source.populationSelector().test(population)) continue;
            if (match != null) {
                throw new IllegalStateException("multiple wild population sources match " + population.populationId()
                        + ": " + match.sourceId() + ", " + source.sourceId());
            }
            match = source;
        }
        return Optional.ofNullable(match);
    }

    public static Optional<Source> sourceFor(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        if (encounter == null) return Optional.empty();
        var population = CanonicalWildPopulationCatalogue.DEFAULT.population(encounter.populationId()).orElse(null);
        return sourceFor(population);
    }

    private static Source fromDescriptor(WildEcologyDescriptorRegistry.Descriptor descriptor) {
        return new Source(
                descriptor.sourceId(),
                descriptor.populationSelector(),
                descriptor.worldEligibility()::accepts,
                descriptor::projectedSiteId,
                descriptor.blueprintSource(),
                descriptor.projectionEligibility());
    }

    static synchronized int sourceCount() {
        return WildEcologyDescriptorRegistry.descriptorCount() + LEGACY_SOURCES.size();
    }
}

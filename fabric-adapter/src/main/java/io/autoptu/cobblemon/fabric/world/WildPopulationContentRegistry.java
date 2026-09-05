package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.fabric.battle.CanonicalWildEncounterBlueprintSource;
import net.minecraft.server.world.ServerWorld;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Server-owned registration boundary between authored WILD content and the global population runtime.
 *
 * <p>Region modules contribute selectors, world/projection policy and an already-canonical blueprint source.
 * They do not own activation cadence, presence reconciliation, actor lifecycle or interaction binding.</p>
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

    private static final Map<String, Source> SOURCES = new LinkedHashMap<>();

    private WildPopulationContentRegistry() {}

    public static synchronized void register(Source source) {
        if (source == null) throw new IllegalArgumentException("source is required");
        Source previous = SOURCES.putIfAbsent(source.sourceId(), source);
        if (previous != null && previous != source) {
            throw new IllegalStateException("wild population source already registered: " + source.sourceId());
        }
    }

    public static synchronized Optional<Source> sourceFor(
            CanonicalWildPopulationCatalogue.PopulationDefinition population
    ) {
        if (population == null) return Optional.empty();
        Source match = null;
        for (Source source : SOURCES.values()) {
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

    static synchronized int sourceCount() {
        return SOURCES.size();
    }
}

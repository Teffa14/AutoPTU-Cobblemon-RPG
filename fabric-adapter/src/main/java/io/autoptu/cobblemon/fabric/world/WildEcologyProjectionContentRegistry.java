package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Server-owned authored content boundary for visible-wild ambient projection.
 *
 * <p>Region content supplies only population selection and an ambient behavior profile. Actor identity,
 * projected sites, presentation anchors and loaded Cobblemon entities are resolved by the global projection
 * runtime from canonical server state.</p>
 */
public final class WildEcologyProjectionContentRegistry {
    public record Source(
            String sourceId,
            Predicate<CanonicalWildPopulationCatalogue.PopulationDefinition> populationSelector,
            WildBehaviorProfile behaviorProfile
    ) {
        public Source {
            if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId is required");
            sourceId = sourceId.strip();
            if (populationSelector == null) throw new IllegalArgumentException("populationSelector is required");
            if (behaviorProfile == null) throw new IllegalArgumentException("behaviorProfile is required");
        }
    }

    private static final Map<String, Source> SOURCES = new LinkedHashMap<>();

    private WildEcologyProjectionContentRegistry() {}

    public static synchronized void register(Source source) {
        if (source == null) throw new IllegalArgumentException("source is required");
        Source previous = SOURCES.putIfAbsent(source.sourceId(), source);
        if (previous != null && previous != source) {
            throw new IllegalStateException("wild ecology projection content source already registered: " + source.sourceId());
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
                throw new IllegalStateException("multiple wild ecology projection content sources match "
                        + population.populationId() + ": " + match.sourceId() + ", " + source.sourceId());
            }
            match = source;
        }
        return Optional.ofNullable(match);
    }

    static synchronized int sourceCount() {
        return SOURCES.size();
    }
}

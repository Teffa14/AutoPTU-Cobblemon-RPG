package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

/** Compatibility view for visible-wild ambient projection content. */
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

    private static final Map<String, Source> LEGACY_SOURCES = new LinkedHashMap<>();

    private WildEcologyProjectionContentRegistry() {}

    public static synchronized void register(Source source) {
        if (source == null) throw new IllegalArgumentException("source is required");
        Source previous = LEGACY_SOURCES.putIfAbsent(source.sourceId(), source);
        if (previous != null && previous != source) {
            throw new IllegalStateException("wild ecology projection content source already registered: " + source.sourceId());
        }
    }

    public static synchronized Optional<Source> sourceFor(
            CanonicalWildPopulationCatalogue.PopulationDefinition population
    ) {
        if (population == null) return Optional.empty();
        var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
        if (descriptor != null) {
            return Optional.of(new Source(
                    descriptor.sourceId(),
                    descriptor.populationSelector(),
                    descriptor.behaviorProfile()));
        }

        Source match = null;
        for (Source source : LEGACY_SOURCES.values()) {
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
        return WildEcologyDescriptorRegistry.descriptorCount() + LEGACY_SOURCES.size();
    }
}

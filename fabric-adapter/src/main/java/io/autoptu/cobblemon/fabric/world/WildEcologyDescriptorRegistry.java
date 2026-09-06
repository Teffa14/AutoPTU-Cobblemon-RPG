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
 * Single server-authored registration boundary for visible-wild ecology.
 *
 * <p>One descriptor owns the population selector, Minecraft-world eligibility, temporal projection calendar,
 * canonical blueprint source, projection-content eligibility, ambient behavior and Minecraft presentation policy.
 * Generic Wild* runtimes consume this descriptor and continue to own lifecycle, projection and interaction behavior.</p>
 */
public final class WildEcologyDescriptorRegistry {
    @FunctionalInterface
    public interface WorldEligibility {
        boolean accepts(ServerWorld world);
    }

    @FunctionalInterface
    public interface PresentationProfileResolver {
        WildPresentationProfile resolve(CanonicalWildEncounterCatalogue.EncounterDefinition encounter);
    }

    public record Descriptor(
            String sourceId,
            Predicate<CanonicalWildPopulationCatalogue.PopulationDefinition> populationSelector,
            WorldEligibility worldEligibility,
            List<WildPopulationProjectionProfile> projectionProfiles,
            CanonicalWildEncounterBlueprintSource blueprintSource,
            Predicate<CanonicalWildEncounterCatalogue.EncounterDefinition> projectionEligibility,
            WildBehaviorProfile behaviorProfile,
            PresentationProfileResolver presentationProfileResolver
    ) {
        public Descriptor {
            if (sourceId == null || sourceId.isBlank()) throw new IllegalArgumentException("sourceId is required");
            sourceId = sourceId.strip();
            if (populationSelector == null) throw new IllegalArgumentException("populationSelector is required");
            if (worldEligibility == null) throw new IllegalArgumentException("worldEligibility is required");
            if (projectionProfiles == null) throw new IllegalArgumentException("projectionProfiles are required");
            projectionProfiles = List.copyOf(projectionProfiles);
            if (blueprintSource == null) throw new IllegalArgumentException("blueprintSource is required");
            if (projectionEligibility == null) throw new IllegalArgumentException("projectionEligibility is required");
            if (behaviorProfile == null) throw new IllegalArgumentException("behaviorProfile is required");
            if (presentationProfileResolver == null) {
                throw new IllegalArgumentException("presentationProfileResolver is required");
            }

            Map<String, WildPopulationProjectionProfile> profilesByPopulation = new LinkedHashMap<>();
            for (WildPopulationProjectionProfile profile : projectionProfiles) {
                if (profile == null) throw new IllegalArgumentException("projection profile is required");
                WildPopulationProjectionProfile previous = profilesByPopulation.putIfAbsent(profile.populationId(), profile);
                if (previous != null) {
                    throw new IllegalArgumentException("multiple projection profiles for population: " + profile.populationId());
                }
            }
        }

        /** Compatibility constructor for ecology content that has not authored special visual roles. */
        public Descriptor(
                String sourceId,
                Predicate<CanonicalWildPopulationCatalogue.PopulationDefinition> populationSelector,
                WorldEligibility worldEligibility,
                List<WildPopulationProjectionProfile> projectionProfiles,
                CanonicalWildEncounterBlueprintSource blueprintSource,
                Predicate<CanonicalWildEncounterCatalogue.EncounterDefinition> projectionEligibility,
                WildBehaviorProfile behaviorProfile
        ) {
            this(
                    sourceId,
                    populationSelector,
                    worldEligibility,
                    projectionProfiles,
                    blueprintSource,
                    projectionEligibility,
                    behaviorProfile,
                    ignored -> WildPresentationProfile.STANDARD);
        }

        public Optional<String> projectedSiteId(
                CanonicalWildPopulationCatalogue.PopulationDefinition population,
                long worldTick
        ) {
            if (population == null) throw new IllegalArgumentException("population is required");
            for (WildPopulationProjectionProfile profile : projectionProfiles) {
                if (profile.populationId().equals(population.populationId())) {
                    return profile.projectedSiteId(population, worldTick);
                }
            }
            return Optional.of(population.siteId());
        }

        public WildPresentationProfile presentationProfile(
                CanonicalWildEncounterCatalogue.EncounterDefinition encounter
        ) {
            if (encounter == null) throw new IllegalArgumentException("encounter is required");
            WildPresentationProfile profile = presentationProfileResolver.resolve(encounter);
            if (profile == null) {
                throw new IllegalStateException("presentation profile resolver returned null for "
                        + encounter.canonicalEncounterId());
            }
            return profile;
        }
    }

    private static final Map<String, Descriptor> DESCRIPTORS = new LinkedHashMap<>();

    private WildEcologyDescriptorRegistry() {}

    public static synchronized void register(Descriptor descriptor) {
        if (descriptor == null) throw new IllegalArgumentException("descriptor is required");
        Descriptor previous = DESCRIPTORS.putIfAbsent(descriptor.sourceId(), descriptor);
        if (previous != null && previous != descriptor) {
            throw new IllegalStateException("wild ecology descriptor already registered: " + descriptor.sourceId());
        }
    }

    public static synchronized Optional<Descriptor> descriptorFor(
            CanonicalWildPopulationCatalogue.PopulationDefinition population
    ) {
        if (population == null) return Optional.empty();
        Descriptor match = null;
        for (Descriptor descriptor : DESCRIPTORS.values()) {
            if (!descriptor.populationSelector().test(population)) continue;
            if (match != null) {
                throw new IllegalStateException("multiple wild ecology descriptors match " + population.populationId()
                        + ": " + match.sourceId() + ", " + descriptor.sourceId());
            }
            match = descriptor;
        }
        return Optional.ofNullable(match);
    }

    public static Optional<Descriptor> descriptorFor(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        if (encounter == null) return Optional.empty();
        var population = CanonicalWildPopulationCatalogue.DEFAULT.population(encounter.populationId()).orElse(null);
        return descriptorFor(population);
    }

    public static Optional<String> projectedSiteId(
            CanonicalWildPopulationCatalogue.PopulationDefinition population,
            long worldTick
    ) {
        if (population == null) throw new IllegalArgumentException("population is required");
        return descriptorFor(population)
                .map(descriptor -> descriptor.projectedSiteId(population, worldTick))
                .orElseGet(() -> Optional.of(population.siteId()));
    }

    static synchronized int descriptorCount() {
        return DESCRIPTORS.size();
    }
}

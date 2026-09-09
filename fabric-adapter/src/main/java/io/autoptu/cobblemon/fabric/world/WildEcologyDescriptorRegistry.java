package io.autoptu.cobblemon.fabric.world;

import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.ecology.MigrationPhase;
import io.autoptu.cobblemon.fabric.battle.CanonicalWildEncounterBlueprintSource;
import net.minecraft.server.world.ServerWorld;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Single server-authored registration boundary for visible-wild ecology.
 *
 * <p>One descriptor owns the population selector, Minecraft-world eligibility, temporal projection calendar,
 * canonical blueprint source, projection-content eligibility, ambient behavior profile, authored social role,
 * and explicit presentation capabilities. Generic Wild* runtimes consume this descriptor and continue to own
 * lifecycle, projection and interaction behavior.</p>
 */
public final class WildEcologyDescriptorRegistry {
    @FunctionalInterface
    public interface WorldEligibility {
        boolean accepts(ServerWorld world);
    }

    /**
     * Presentation-only capabilities authored by the RPG ecology layer.
     *
     * <p>These flags may drive synchronized Cobblemon entity visuals and Minecraft herd context only. They do
     * not authorize Cobblemon persistent alpha state, stat changes, movesets, encounter outcomes, aggression,
     * battle AI or any PTU legality.</p>
     */
    public record PresentationCapabilities(boolean nativeAlphaVisual, boolean herdLeaderPresentation) {
        public static final PresentationCapabilities NONE = new PresentationCapabilities(false, false);
        public static final PresentationCapabilities ALPHA_HERD_LEADER = new PresentationCapabilities(true, true);
    }

    public record Descriptor(
            String sourceId,
            Predicate<CanonicalWildPopulationCatalogue.PopulationDefinition> populationSelector,
            WorldEligibility worldEligibility,
            List<WildPopulationProjectionProfile> projectionProfiles,
            CanonicalWildEncounterBlueprintSource blueprintSource,
            Predicate<CanonicalWildEncounterCatalogue.EncounterDefinition> projectionEligibility,
            WildBehaviorProfile behaviorProfile,
            Function<CanonicalWildEncounterCatalogue.EncounterDefinition, WildSocialRole> socialRoleResolver,
            Function<CanonicalWildEncounterCatalogue.EncounterDefinition, PresentationCapabilities> presentationCapabilitiesResolver
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
            if (socialRoleResolver == null) throw new IllegalArgumentException("socialRoleResolver is required");
            if (presentationCapabilitiesResolver == null) throw new IllegalArgumentException("presentationCapabilitiesResolver is required");

            Map<String, WildPopulationProjectionProfile> profilesByPopulation = new LinkedHashMap<>();
            for (WildPopulationProjectionProfile profile : projectionProfiles) {
                if (profile == null) throw new IllegalArgumentException("projection profile is required");
                WildPopulationProjectionProfile previous = profilesByPopulation.putIfAbsent(profile.populationId(), profile);
                if (previous != null) {
                    throw new IllegalArgumentException("multiple projection profiles for population: " + profile.populationId());
                }
            }
        }

        /** Compatibility constructor for authored social roles that do not request special presentation. */
        public Descriptor(
                String sourceId,
                Predicate<CanonicalWildPopulationCatalogue.PopulationDefinition> populationSelector,
                WorldEligibility worldEligibility,
                List<WildPopulationProjectionProfile> projectionProfiles,
                CanonicalWildEncounterBlueprintSource blueprintSource,
                Predicate<CanonicalWildEncounterCatalogue.EncounterDefinition> projectionEligibility,
                WildBehaviorProfile behaviorProfile,
                Function<CanonicalWildEncounterCatalogue.EncounterDefinition, WildSocialRole> socialRoleResolver
        ) {
            this(sourceId, populationSelector, worldEligibility, projectionProfiles, blueprintSource,
                    projectionEligibility, behaviorProfile, socialRoleResolver,
                    encounter -> PresentationCapabilities.NONE);
        }

        /** Compatibility constructor for ecology sources without authored social roles. */
        public Descriptor(
                String sourceId,
                Predicate<CanonicalWildPopulationCatalogue.PopulationDefinition> populationSelector,
                WorldEligibility worldEligibility,
                List<WildPopulationProjectionProfile> projectionProfiles,
                CanonicalWildEncounterBlueprintSource blueprintSource,
                Predicate<CanonicalWildEncounterCatalogue.EncounterDefinition> projectionEligibility,
                WildBehaviorProfile behaviorProfile
        ) {
            this(sourceId, populationSelector, worldEligibility, projectionProfiles, blueprintSource,
                    projectionEligibility, behaviorProfile,
                    encounter -> WildSocialRole.MEMBER,
                    encounter -> PresentationCapabilities.NONE);
        }

        public Optional<String> projectedSiteId(
                CanonicalWildPopulationCatalogue.PopulationDefinition population,
                long worldTick
        ) {
            if (population == null) throw new IllegalArgumentException("population is required");
            for (WildPopulationProjectionProfile profile : projectionProfiles) {
                if (profile.populationId().equals(population.populationId())) return profile.projectedSiteId(population, worldTick);
            }
            return Optional.of(population.siteId());
        }

        public Optional<MigrationPhase> projectionPhase(
                CanonicalWildPopulationCatalogue.PopulationDefinition population,
                long worldTick
        ) {
            if (population == null) throw new IllegalArgumentException("population is required");
            if (worldTick < 0L) throw new IllegalArgumentException("worldTick must be >= 0");
            for (WildPopulationProjectionProfile profile : projectionProfiles) {
                if (profile.populationId().equals(population.populationId())) return Optional.of(profile.resolve(population, worldTick).phase());
            }
            return Optional.empty();
        }

        public WildSocialRole socialRole(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
            if (encounter == null) throw new IllegalArgumentException("encounter is required");
            WildSocialRole role = socialRoleResolver.apply(encounter);
            if (role == null) throw new IllegalStateException("wild ecology social role resolver returned null for " + encounter.canonicalEncounterId());
            return role;
        }

        public PresentationCapabilities presentationCapabilities(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
            if (encounter == null) throw new IllegalArgumentException("encounter is required");
            PresentationCapabilities capabilities = presentationCapabilitiesResolver.apply(encounter);
            if (capabilities == null) throw new IllegalStateException("wild ecology presentation capability resolver returned null for " + encounter.canonicalEncounterId());
            return capabilities;
        }
    }

    private static final Map<String, Descriptor> DESCRIPTORS = new LinkedHashMap<>();
    private WildEcologyDescriptorRegistry() {}

    public static synchronized void register(Descriptor descriptor) {
        if (descriptor == null) throw new IllegalArgumentException("descriptor is required");
        Descriptor previous = DESCRIPTORS.putIfAbsent(descriptor.sourceId(), descriptor);
        if (previous != null && previous != descriptor) throw new IllegalStateException("wild ecology descriptor already registered: " + descriptor.sourceId());
    }

    public static synchronized Optional<Descriptor> descriptorFor(CanonicalWildPopulationCatalogue.PopulationDefinition population) {
        if (population == null) return Optional.empty();
        Descriptor match = null;
        for (Descriptor descriptor : DESCRIPTORS.values()) {
            if (!descriptor.populationSelector().test(population)) continue;
            if (match != null) throw new IllegalStateException("multiple wild ecology descriptors match " + population.populationId() + ": " + match.sourceId() + ", " + descriptor.sourceId());
            match = descriptor;
        }
        return Optional.ofNullable(match);
    }

    public static Optional<Descriptor> descriptorFor(CanonicalWildEncounterCatalogue.EncounterDefinition encounter) {
        if (encounter == null) return Optional.empty();
        var population = CanonicalWildPopulationCatalogue.DEFAULT.population(encounter.populationId()).orElse(null);
        return descriptorFor(population);
    }

    public static Optional<String> projectedSiteId(CanonicalWildPopulationCatalogue.PopulationDefinition population, long worldTick) {
        if (population == null) throw new IllegalArgumentException("population is required");
        return descriptorFor(population).map(descriptor -> descriptor.projectedSiteId(population, worldTick)).orElseGet(() -> Optional.of(population.siteId()));
    }

    static synchronized int descriptorCount() { return DESCRIPTORS.size(); }
}

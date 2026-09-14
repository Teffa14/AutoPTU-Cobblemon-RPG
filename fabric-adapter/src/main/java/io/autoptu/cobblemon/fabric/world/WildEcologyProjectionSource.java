package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Canonical projection source and immutable model for server-owned visible wild actors. */
final class WildEcologyProjectionSource {
    record ProjectedActor(
            PokemonEntity actor,
            String populationKey,
            String habitatDisplayName,
            double habitatCenterX,
            double habitatCenterZ,
            int habitatLeashRadiusBlocks,
            WildBehaviorProfile behaviorProfile,
            WildSocialRole socialRole,
            WildEcologyDescriptorRegistry.PresentationCapabilities presentationCapabilities
    ) {
        ProjectedActor {
            Objects.requireNonNull(actor, "actor");
            populationKey = Objects.requireNonNull(populationKey, "populationKey").strip();
            habitatDisplayName = Objects.requireNonNull(habitatDisplayName, "habitatDisplayName").strip();
            Objects.requireNonNull(behaviorProfile, "behaviorProfile");
            Objects.requireNonNull(socialRole, "socialRole");
            Objects.requireNonNull(presentationCapabilities, "presentationCapabilities");
            if (populationKey.isEmpty()) throw new IllegalArgumentException("populationKey must not be blank");
            if (habitatDisplayName.isEmpty()) throw new IllegalArgumentException("habitatDisplayName must not be blank");
            if (!Double.isFinite(habitatCenterX) || !Double.isFinite(habitatCenterZ)) {
                throw new IllegalArgumentException("habitat center must be finite");
            }
            if (habitatLeashRadiusBlocks <= 0) {
                throw new IllegalArgumentException("habitat leash radius must be positive");
            }
        }

        ProjectedActor(PokemonEntity actor, String populationKey, String habitatDisplayName, double habitatCenterX,
                       double habitatCenterZ, int habitatLeashRadiusBlocks, WildBehaviorProfile behaviorProfile,
                       WildSocialRole socialRole) {
            this(actor, populationKey, habitatDisplayName, habitatCenterX, habitatCenterZ, habitatLeashRadiusBlocks,
                    behaviorProfile, socialRole, WildEcologyDescriptorRegistry.PresentationCapabilities.NONE);
        }

        ProjectedActor(PokemonEntity actor, String populationKey, String habitatDisplayName, double habitatCenterX,
                       double habitatCenterZ, int habitatLeashRadiusBlocks, WildBehaviorProfile behaviorProfile) {
            this(actor, populationKey, habitatDisplayName, habitatCenterX, habitatCenterZ, habitatLeashRadiusBlocks,
                    behaviorProfile, WildSocialRole.MEMBER, WildEcologyDescriptorRegistry.PresentationCapabilities.NONE);
        }

        ProjectedActor(PokemonEntity actor, String populationKey, double habitatCenterX, double habitatCenterZ,
                       int habitatLeashRadiusBlocks, WildBehaviorProfile behaviorProfile) {
            this(actor, populationKey, populationKey, habitatCenterX, habitatCenterZ, habitatLeashRadiusBlocks,
                    behaviorProfile, WildSocialRole.MEMBER, WildEcologyDescriptorRegistry.PresentationCapabilities.NONE);
        }
    }

    private WildEcologyProjectionSource() {}

    static List<ProjectedActor> collect(ServerWorld world) {
        Objects.requireNonNull(world, "world");
        return projectedActors(world);
    }

    private static List<ProjectedActor> projectedActors(ServerWorld world) {
        List<ProjectedActor> projected = new ArrayList<>();
        Set<UUID> projectedActorIds = new HashSet<>();
        Set<Object> duplicateEncounterIds = duplicateEncounterIds();
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (descriptor == null || !descriptor.worldEligibility().accepts(world)) continue;
            var projectedSiteId = descriptor.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;
            var site = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId.get())
                    .orElseThrow(() -> new IllegalStateException(
                            "missing projected canonical wild population site: " + projectedSiteId.get()));
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                // A canonical encounter identity must resolve to exactly one authored population member. If content
                // accidentally publishes the same identity more than once, projecting whichever entry happens to be
                // visited first would make Minecraft iteration order an authority source. Reject every ambiguous
                // occurrence instead; content must be corrected server-side before that actor can re-enter ecology.
                if (duplicateEncounterIds.contains(encounter.canonicalEncounterId())) continue;
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || !actor.isAlive()) continue;

                var binding = VisibleWildPokemonEncounterRuntime.binding(actor.getUuid()).orElse(null);
                if (binding == null
                        || binding.presentationEntity() != actor
                        || !binding.canonicalEncounterId().equals(encounter.canonicalEncounterId())) continue;

                // Encounter/battle handoff intentionally hides the canonical presentation entity while keeping
                // its binding alive. Preserve that inactive actor in this projection so downstream Minecraft
                // runtimes can revoke locomotion, habitat cues and social-role presentation coherently. An actor
                // that is invisible while still interaction-active is unexpected presentation state and remains
                // fail-closed. No PTU battle state or legality is inferred from visibility here.
                boolean interactionActive = VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid());
                if (actor.isInvisible() && interactionActive) continue;
                if (!projectedActorIds.add(actor.getUuid())) continue;

                BlockPos anchor = WildPopulationRuntime.projectedPresentationAnchor(encounter, projectedSiteId.get());
                projected.add(new ProjectedActor(
                        actor, population.siteId(), site.displayName(), anchor.getX() + 0.5D, anchor.getZ() + 0.5D,
                        population.habitatLeashRadiusBlocks(), descriptor.behaviorProfile(), descriptor.socialRole(encounter),
                        descriptor.presentationCapabilities(encounter)));
            }
        }
        return List.copyOf(projected);
    }

    private static Set<Object> duplicateEncounterIds() {
        Set<Object> seen = new HashSet<>();
        Set<Object> duplicates = new HashSet<>();
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                Object encounterId = encounter.canonicalEncounterId();
                if (!seen.add(encounterId)) duplicates.add(encounterId);
            }
        }
        return duplicates;
    }
}
package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Canonical projection source for server-owned visible wild actors. */
final class WildEcologyProjectionSource {
    private WildEcologyProjectionSource() {}

    static Iterable<WildEcologyProjectionRegistry.ProjectedActor> projectedActors(ServerWorld world) {
        if (world == null) return List.of();
        List<WildEcologyProjectionRegistry.ProjectedActor> projected = new ArrayList<>();
        Set<UUID> projectedActorIds = new HashSet<>();
        Set<Object> projectedEncounterIds = new HashSet<>();
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (descriptor == null || !descriptor.worldEligibility().accepts(world)) continue;
            var projectedSiteId = descriptor.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;
            var site = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId.get())
                    .orElseThrow(() -> new IllegalStateException("missing projected canonical wild population site: " + projectedSiteId.get()));
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                if (!projectedEncounterIds.add(encounter.canonicalEncounterId())) continue;
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
                projected.add(new WildEcologyProjectionRegistry.ProjectedActor(
                        actor, population.siteId(), site.displayName(), anchor.getX() + 0.5D, anchor.getZ() + 0.5D,
                        population.habitatLeashRadiusBlocks(), descriptor.behaviorProfile(), descriptor.socialRole(encounter),
                        descriptor.presentationCapabilities(encounter)));
            }
        }
        return List.copyOf(projected);
    }
}

package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/** Global projection source for server-owned visible wild actors. */
final class WildEcologyProjectionSource {
    private WildEcologyProjectionSource() {}

    static Iterable<WildEcologyProjectionRegistry.ProjectedActor> projectedActors(ServerWorld world) {
        if (world == null) return List.of();
        List<WildEcologyProjectionRegistry.ProjectedActor> projected = new ArrayList<>();
        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            var descriptor = WildEcologyDescriptorRegistry.descriptorFor(population).orElse(null);
            if (descriptor == null || !descriptor.worldEligibility().accepts(world)) continue;
            var projectedSiteId = descriptor.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;
            var site = CanonicalWorldMapCatalogue.DEFAULT.site(projectedSiteId.get())
                    .orElseThrow(() -> new IllegalStateException("missing projected canonical wild population site: " + projectedSiteId.get()));
            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;
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

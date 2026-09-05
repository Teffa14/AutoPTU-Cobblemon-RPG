package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * Authored projection source for the current Marea fixture content.
 *
 * This class supplies data only. All ambient behavior, navigation, yielding and habitat feedback
 * are owned by region-agnostic Wild* runtimes consuming WildEcologyProjectionRegistry.
 */
final class MareaWildEcologyProjectionSource {
    static final WildBehaviorProfile BEHAVIOR_PROFILE = new WildBehaviorProfile(
            14.0D,
            7.0D,
            3,
            5,
            80L,
            60L,
            0.001D,
            14.0D,
            35.0F,
            0.025D,
            1.0D,
            2.5D,
            0.018D,
            6.0D,
            0.012D,
            0.08D,
            0.04D,
            1.5D);

    private MareaWildEcologyProjectionSource() {
    }

    static Iterable<WildEcologyProjectionRegistry.ProjectedActor> projectedActors(ServerWorld world) {
        if (world == null) return List.of();
        List<WildEcologyProjectionRegistry.ProjectedActor> projected = new ArrayList<>();

        for (var population : CanonicalWildPopulationCatalogue.DEFAULT.populations()) {
            if (!population.siteId().startsWith("ouros.marea.")) continue;
            var projectedSiteId = MareaWildMigrationProjection.projectedSiteId(population, world.getTime());
            if (projectedSiteId.isEmpty()) continue;

            for (var encounter : CanonicalWildPopulationCatalogue.DEFAULT.members(population)) {
                var boundUuid = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId());
                if (boundUuid.isEmpty()) continue;
                var loaded = world.getEntity(boundUuid.get());
                if (!(loaded instanceof PokemonEntity actor) || actor.isRemoved() || actor.isInvisible()) continue;
                if (!VisibleWildPokemonEncounterRuntime.isInteractionActive(actor.getUuid())) continue;

                BlockPos anchor = MareaVisibleWildPokemonRuntime.projectedPresentationAnchor(encounter, projectedSiteId.get());
                projected.add(new WildEcologyProjectionRegistry.ProjectedActor(
                        actor,
                        population.siteId(),
                        anchor.getX() + 0.5D,
                        anchor.getZ() + 0.5D,
                        population.habitatLeashRadiusBlocks(),
                        BEHAVIOR_PROFILE));
            }
        }
        return List.copyOf(projected);
    }
}

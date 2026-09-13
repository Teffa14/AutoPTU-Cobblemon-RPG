package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.List;
import java.util.Objects;

/**
 * Immutable projection model and canonical collector for server-authored visible-wild ecology.
 *
 * <p>The previous mutable source registry has been removed. Every projected actor now comes through
 * {@link WildEcologyProjectionSource}, which resolves only canonical populations and their single
 * {@link WildEcologyDescriptorRegistry} descriptor. This keeps Minecraft presentation extensible
 * through authored descriptors without permitting a second runtime registration authority.</p>
 */
public final class WildEcologyProjectionRegistry {
    public record ProjectedActor(
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
        public ProjectedActor {
            Objects.requireNonNull(actor, "actor");
            populationKey = Objects.requireNonNull(populationKey, "populationKey").strip();
            habitatDisplayName = Objects.requireNonNull(habitatDisplayName, "habitatDisplayName").strip();
            Objects.requireNonNull(behaviorProfile, "behaviorProfile");
            Objects.requireNonNull(socialRole, "socialRole");
            Objects.requireNonNull(presentationCapabilities, "presentationCapabilities");
            if (populationKey.isEmpty()) throw new IllegalArgumentException("populationKey must not be blank");
            if (habitatDisplayName.isEmpty()) throw new IllegalArgumentException("habitatDisplayName must not be blank");
            if (!Double.isFinite(habitatCenterX) || !Double.isFinite(habitatCenterZ)) throw new IllegalArgumentException("habitat center must be finite");
            if (habitatLeashRadiusBlocks <= 0) throw new IllegalArgumentException("habitat leash radius must be positive");
        }

        public ProjectedActor(PokemonEntity actor, String populationKey, String habitatDisplayName, double habitatCenterX,
                              double habitatCenterZ, int habitatLeashRadiusBlocks, WildBehaviorProfile behaviorProfile,
                              WildSocialRole socialRole) {
            this(actor, populationKey, habitatDisplayName, habitatCenterX, habitatCenterZ, habitatLeashRadiusBlocks,
                    behaviorProfile, socialRole, WildEcologyDescriptorRegistry.PresentationCapabilities.NONE);
        }

        public ProjectedActor(PokemonEntity actor, String populationKey, String habitatDisplayName, double habitatCenterX,
                              double habitatCenterZ, int habitatLeashRadiusBlocks, WildBehaviorProfile behaviorProfile) {
            this(actor, populationKey, habitatDisplayName, habitatCenterX, habitatCenterZ, habitatLeashRadiusBlocks,
                    behaviorProfile, WildSocialRole.MEMBER, WildEcologyDescriptorRegistry.PresentationCapabilities.NONE);
        }

        public ProjectedActor(PokemonEntity actor, String populationKey, double habitatCenterX, double habitatCenterZ,
                              int habitatLeashRadiusBlocks, WildBehaviorProfile behaviorProfile) {
            this(actor, populationKey, populationKey, habitatCenterX, habitatCenterZ, habitatLeashRadiusBlocks,
                    behaviorProfile, WildSocialRole.MEMBER, WildEcologyDescriptorRegistry.PresentationCapabilities.NONE);
        }
    }

    private WildEcologyProjectionRegistry() {}

    static List<ProjectedActor> collect(ServerWorld world) {
        Objects.requireNonNull(world, "world");
        Iterable<ProjectedActor> projected = WildEcologyProjectionSource.projectedActors(world);
        if (projected == null) return List.of();
        java.util.ArrayList<ProjectedActor> actors = new java.util.ArrayList<>();
        for (ProjectedActor actor : projected) if (actor != null) actors.add(actor);
        return List.copyOf(actors);
    }
}

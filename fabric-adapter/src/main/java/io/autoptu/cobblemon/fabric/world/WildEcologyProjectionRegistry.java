package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Global registry of server-authored visible-wild ecology projections.
 *
 * Region/species content registers projection sources here. Generic ecology runtimes consume the
 * resulting actor/habitat/profile tuples without knowing species names, map names or PTU facts.
 */
public final class WildEcologyProjectionRegistry {
    public record ProjectedActor(
            PokemonEntity actor,
            String populationKey,
            double habitatCenterX,
            double habitatCenterZ,
            int habitatLeashRadiusBlocks,
            WildBehaviorProfile behaviorProfile
    ) {
        public ProjectedActor {
            Objects.requireNonNull(actor, "actor");
            populationKey = Objects.requireNonNull(populationKey, "populationKey").strip();
            Objects.requireNonNull(behaviorProfile, "behaviorProfile");
            if (populationKey.isEmpty()) {
                throw new IllegalArgumentException("populationKey must not be blank");
            }
            if (!Double.isFinite(habitatCenterX) || !Double.isFinite(habitatCenterZ)) {
                throw new IllegalArgumentException("habitat center must be finite");
            }
            if (habitatLeashRadiusBlocks <= 0) {
                throw new IllegalArgumentException("habitat leash radius must be positive");
            }
        }
    }

    @FunctionalInterface
    public interface ProjectionSource {
        Iterable<ProjectedActor> projectedActors(ServerWorld world);
    }

    private static final Map<String, ProjectionSource> SOURCES = new LinkedHashMap<>();

    private WildEcologyProjectionRegistry() {
    }

    public static synchronized void register(String sourceId, ProjectionSource source) {
        String normalized = Objects.requireNonNull(sourceId, "sourceId").strip();
        if (normalized.isEmpty()) throw new IllegalArgumentException("sourceId must not be blank");
        Objects.requireNonNull(source, "source");
        ProjectionSource existing = SOURCES.putIfAbsent(normalized, source);
        if (existing != null && existing != source) {
            throw new IllegalStateException("wild ecology projection source already registered: " + normalized);
        }
    }

    static synchronized List<ProjectionSource> sourcesSnapshot() {
        return List.copyOf(SOURCES.values());
    }

    static List<ProjectedActor> collect(ServerWorld world) {
        Objects.requireNonNull(world, "world");
        List<ProjectedActor> actors = new ArrayList<>();
        for (ProjectionSource source : sourcesSnapshot()) {
            Iterable<ProjectedActor> projected = source.projectedActors(world);
            if (projected == null) continue;
            for (ProjectedActor actor : projected) {
                if (actor != null) actors.add(actor);
            }
        }
        return List.copyOf(actors);
    }
}

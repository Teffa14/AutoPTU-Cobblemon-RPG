package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.minecraft.server.world.ServerWorld;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Server-owned projection boundary for already-authored complete wild encounter content.
 *
 * <p>The caller supplies only a canonical population or encounter identifier. This service resolves
 * every species/mechanical field from the authored AutoPTU catalogues and delegates publication,
 * world-eligibility checks, durable binding recovery and Cobblemon actor materialization to
 * {@link WildPopulationRuntime}. It never accepts client-provided species, level, stats, moves, HP,
 * accuracy/evasion, movement or battle outcomes.</p>
 */
public final class AuthoritativeWildEncounterProjectionService {
    public ProjectionResult project(ServerWorld world, String authoredId) {
        Objects.requireNonNull(world, "world");
        String normalized = requireId(authoredId);

        var encounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(normalized);
        if (encounter.isPresent()) {
            PokemonEntity actor = WildPopulationRuntime.ensureProjected(world, encounter.get());
            if (actor == null) {
                throw new IllegalStateException("Minecraft could not materialize the authored wild presentation actor");
            }
            return new ProjectionResult(
                    ProjectionKind.BLUEPRINT,
                    normalized,
                    List.of(new ProjectedEncounter(encounter.get().canonicalEncounterId(), actor.getUuidAsString()))
            );
        }

        var population = CanonicalWildPopulationCatalogue.DEFAULT.population(normalized);
        if (population.isPresent()) {
            List<ProjectedEncounter> projected = new ArrayList<>();
            for (var member : CanonicalWildPopulationCatalogue.DEFAULT.members(population.get())) {
                PokemonEntity actor = WildPopulationRuntime.ensureProjected(world, member);
                if (actor == null) {
                    throw new IllegalStateException(
                            "Minecraft could not materialize authored wild presentation actor "
                                    + member.canonicalEncounterId());
                }
                projected.add(new ProjectedEncounter(member.canonicalEncounterId(), actor.getUuidAsString()));
            }
            return new ProjectionResult(ProjectionKind.TABLE, normalized, projected);
        }

        throw new IllegalArgumentException(
                "unknown server-authored wild table/blueprint id; no gameplay data was synthesized");
    }

    private static String requireId(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("server-authored wild table/blueprint id is required");
        }
        return value.strip();
    }

    public enum ProjectionKind {
        TABLE,
        BLUEPRINT
    }

    public record ProjectedEncounter(String canonicalEncounterId, String presentationEntityUuid) {
        public ProjectedEncounter {
            canonicalEncounterId = requireId(canonicalEncounterId);
            presentationEntityUuid = requireId(presentationEntityUuid);
        }
    }

    public record ProjectionResult(ProjectionKind kind, String authoredId, List<ProjectedEncounter> encounters) {
        public ProjectionResult {
            kind = Objects.requireNonNull(kind, "kind");
            authoredId = requireId(authoredId);
            encounters = encounters == null ? List.of() : List.copyOf(encounters);
            if (encounters.isEmpty()) throw new IllegalArgumentException("projection result requires at least one encounter");
        }
    }
}

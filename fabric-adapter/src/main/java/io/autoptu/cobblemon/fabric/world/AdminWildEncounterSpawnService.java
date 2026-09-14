package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Operator-facing bridge into the normal server-owned visible-WILD projection pipeline.
 *
 * <p>The selector may name one canonical encounter blueprint directly or one authored population
 * table that currently resolves to exactly one encounter. The service never accepts species, level,
 * stats, HP, moves, items, abilities, RNG or battle results. Those values remain in the canonical
 * server-owned blueprint and AutoPTU-Java authority boundaries.</p>
 */
public final class AdminWildEncounterSpawnService {
    public enum SelectorKind { BLUEPRINT, TABLE }

    public record SpawnResult(
            SelectorKind selectorKind,
            String selector,
            String canonicalEncounterId,
            String populationId,
            String speciesId,
            PokemonEntity presentationEntity
    ) {}

    public SpawnResult spawn(ServerWorld world, String selector) {
        if (world == null) throw new IllegalArgumentException("world is required");
        String requested = requireId(selector, "selector");

        var directEncounter = CanonicalWildEncounterCatalogue.DEFAULT.encounter(requested).orElse(null);
        var population = CanonicalWildPopulationCatalogue.DEFAULT.populations().stream()
                .filter(candidate -> candidate.populationId().equals(requested))
                .findFirst()
                .orElse(null);

        if (directEncounter != null && population != null) {
            throw new IllegalStateException(
                    "canonical wild selector matches both a blueprint and a table; rename authored content to keep operator spawn unambiguous: "
                            + requested);
        }
        if (directEncounter != null) {
            return project(world, requested, SelectorKind.BLUEPRINT, directEncounter);
        }
        if (population == null) {
            throw new IllegalArgumentException("unknown canonical wild table or blueprint: " + requested);
        }

        List<CanonicalWildEncounterCatalogue.EncounterDefinition> members =
                CanonicalWildPopulationCatalogue.DEFAULT.members(population);
        if (members.isEmpty()) {
            throw new IllegalStateException("canonical wild table has no authored encounters: " + requested);
        }
        if (members.size() != 1) {
            throw new IllegalStateException(
                    "canonical wild table is ambiguous for operator spawn; select a blueprint instead: " + requested);
        }
        return project(world, requested, SelectorKind.TABLE, members.get(0));
    }

    private static SpawnResult project(
            ServerWorld world,
            String selector,
            SelectorKind selectorKind,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        PokemonEntity actor = WildPopulationRuntime.ensureProjected(world, encounter);
        if (actor == null) {
            throw new IllegalStateException("Minecraft rejected canonical wild presentation spawn");
        }
        return new SpawnResult(
                selectorKind,
                selector,
                encounter.canonicalEncounterId(),
                encounter.populationId(),
                encounter.speciesId(),
                actor
        );
    }

    private static String requireId(String value, String field) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(field + " is required");
        return value.strip();
    }
}

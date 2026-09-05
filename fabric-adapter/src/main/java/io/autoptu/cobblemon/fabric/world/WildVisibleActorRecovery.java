package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.authority.CanonicalWildEncounterCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWildPopulationCatalogue;
import io.autoptu.cobblemon.authority.CanonicalWorldMapCatalogue;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

/**
 * Region-agnostic recovery for persistent visible-wild presentation actors.
 *
 * <p>The canonical encounter binding owns identity. Minecraft chunk loading only recovers the
 * persisted presentation entity with that UUID; it never supplies PTU species, stats, HP, moves,
 * abilities, legality, RNG or outcomes.</p>
 */
public final class WildVisibleActorRecovery {
    private WildVisibleActorRecovery() {
    }

    public static PokemonEntity recoverBoundActor(
            ServerWorld world,
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        if (world == null) throw new IllegalArgumentException("world is required");
        if (encounter == null) throw new IllegalArgumentException("encounter is required");

        PokemonEntity actor = loadedActor(world, encounter.canonicalEncounterId());
        if (actor != null) return actor;
        if (VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounter.canonicalEncounterId()).isEmpty()) return null;

        BlockPos home = canonicalHomeAnchor(encounter);
        int leash = populationFor(encounter).habitatLeashRadiusBlocks();
        int minChunkX = Math.floorDiv(home.getX() - leash, 16);
        int maxChunkX = Math.floorDiv(home.getX() + leash, 16);
        int minChunkZ = Math.floorDiv(home.getZ() - leash, 16);
        int maxChunkZ = Math.floorDiv(home.getZ() + leash, 16);
        for (int x = minChunkX; x <= maxChunkX; x++) {
            for (int z = minChunkZ; z <= maxChunkZ; z++) {
                world.getChunk(x, z);
            }
        }
        return loadedActor(world, encounter.canonicalEncounterId());
    }

    public static BlockPos canonicalHomeAnchor(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        if (encounter == null) throw new IllegalArgumentException("encounter is required");
        var site = CanonicalWorldMapCatalogue.DEFAULT.site(encounter.siteId())
                .orElseThrow(() -> new IllegalStateException(
                        "missing canonical wild encounter home site: " + encounter.siteId()));
        return new BlockPos(site.x(), site.y(), site.z()).add(
                encounter.presentationOffsetX(),
                encounter.presentationOffsetY(),
                encounter.presentationOffsetZ());
    }

    static PokemonEntity loadedActor(ServerWorld world, String encounterId) {
        var bound = VisibleWildPokemonEncounterRuntime.boundEntityUuid(encounterId);
        if (bound.isEmpty()) return null;
        var entity = world.getEntity(bound.get());
        return entity instanceof PokemonEntity pokemon && !pokemon.isRemoved() ? pokemon : null;
    }

    private static CanonicalWildPopulationCatalogue.PopulationDefinition populationFor(
            CanonicalWildEncounterCatalogue.EncounterDefinition encounter
    ) {
        return CanonicalWildPopulationCatalogue.DEFAULT.population(encounter.populationId())
                .orElseThrow(() -> new IllegalStateException(
                        "missing canonical wild population for recovery: " + encounter.populationId()));
    }
}

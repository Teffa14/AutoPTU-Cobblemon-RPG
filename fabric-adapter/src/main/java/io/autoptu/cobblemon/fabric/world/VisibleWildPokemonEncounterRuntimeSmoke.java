package io.autoptu.cobblemon.fabric.world;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dedicated-server proof for the visible-wild interaction/binding surface. */
public final class VisibleWildPokemonEncounterRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveVisibleWildEncounterSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live visible wild encounter smoke passed";
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private VisibleWildPokemonEncounterRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(VisibleWildPokemonEncounterRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        String canonicalSpeciesId = "sentret";
        Species species = PokemonSpecies.INSTANCE.getByName(canonicalSpeciesId);
        if (species == null) {
            throw new IllegalStateException("Cobblemon Sentret presentation species was not available");
        }

        Pokemon presentation = new Pokemon();
        presentation.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, presentation, CobblemonEntities.POKEMON);
        BlockPos pos = world.getSpawnPos().up(6);
        entity.refreshPositionAndAngles(
                pos.getX() + 0.5D,
                pos.getY(),
                pos.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        if (!world.spawnEntity(entity)) {
            throw new IllegalStateException("could not spawn visible wild presentation actor");
        }

        try {
            String encounterId = "smoke-visible-wild:canonical-001";
            VisibleWildPokemonEncounterRuntime.bind(
                    entity,
                    encounterId,
                    canonicalSpeciesId,
                    "smoke-zone",
                    "visible-roaming-wild"
            );

            var binding = VisibleWildPokemonEncounterRuntime.binding(entity.getUuid()).orElseThrow();
            if (!encounterId.equals(binding.canonicalEncounterId())) {
                throw new IllegalStateException("visible wild binding lost canonical encounter identity");
            }
            if (!canonicalSpeciesId.equals(binding.canonicalWildSpeciesId())) {
                throw new IllegalStateException("visible wild binding lost canonical species identity");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            VisibleWildPokemonEncounterRuntime.unbind(entity.getUuid());
            if (!entity.isRemoved()) entity.discard();
        }
    }
}

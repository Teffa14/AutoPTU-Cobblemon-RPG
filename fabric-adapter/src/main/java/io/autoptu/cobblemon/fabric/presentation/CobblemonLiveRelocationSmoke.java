package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.battlecore.PresentationEntityHandleRegistry;
import io.autoptu.cobblemon.battlecore.RegistryBackedPresentationEntityGateway;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in production-runtime smoke for the first live entity-bound presentation operation.
 *
 * The smoke creates a disposable Cobblemon PokemonEntity, resolves it through the opaque UUID
 * presentation boundary, registers that resolved handle, applies an already-authoritative world
 * relocation through the normal presentation gateway, and verifies the resulting server position.
 * It never reads Pokemon battle data to make a PTU decision.
 */
public final class CobblemonLiveRelocationSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveRelocationSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Cobblemon relocation smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String RESERVATION_ID = "ci-live-relocation";

    private CobblemonLiveRelocationSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(CobblemonLiveRelocationSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos spawn = world.getSpawnPos().up(2);

        Species species = PokemonSpecies.INSTANCE.getByName("pikachu");
        if (species == null) throw new IllegalStateException("Cobblemon Pikachu species is unavailable");

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.setAiDisabled(true);
        entity.setPersistent();
        entity.refreshPositionAndAngles(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
        if (!world.spawnEntity(entity)) {
            throw new IllegalStateException("failed to spawn live Cobblemon PokemonEntity");
        }

        String presentationEntityId = entity.getUuidAsString();
        PokemonEntity resolved = new CobblemonPokemonEntityLookup()
                .find(server, presentationEntityId)
                .orElseThrow(() -> new IllegalStateException("spawned PokemonEntity was not resolvable by UUID"));
        if (resolved != entity) {
            throw new IllegalStateException("presentation UUID resolved to a different PokemonEntity");
        }

        PresentationEntityHandleRegistry<PokemonEntity> registry = new PresentationEntityHandleRegistry<>();
        registry.register(RESERVATION_ID, presentationEntityId, resolved);
        RegistryBackedPresentationEntityGateway<PokemonEntity> gateway =
                new RegistryBackedPresentationEntityGateway<>(registry, new CobblemonPresentationEntityBackend());

        String dimensionId = world.getRegistryKey().getValue().toString();
        WorldBlockCoordinate origin = new WorldBlockCoordinate(
                dimensionId,
                spawn.getX(),
                spawn.getY(),
                spawn.getZ()
        );
        WorldBlockCoordinate destination = new WorldBlockCoordinate(
                dimensionId,
                spawn.getX() + 4,
                spawn.getY(),
                spawn.getZ() + 3
        );

        gateway.relocate(RESERVATION_ID, presentationEntityId, origin, destination);

        assertNear(destination.x() + 0.5D, entity.getX(), "x");
        assertNear(destination.y(), entity.getY(), "y");
        assertNear(destination.z() + 0.5D, entity.getZ(), "z");

        LOGGER.info("{}: entity={} destination={},{},{}", SUCCESS_LOG,
                presentationEntityId, destination.x(), destination.y(), destination.z());
        entity.discard();
        registry.releaseReservation(RESERVATION_ID);
    }

    private static void assertNear(double expected, double actual, String axis) {
        if (Math.abs(expected - actual) > 0.001D) {
            throw new IllegalStateException(
                    "live relocation " + axis + " mismatch: expected=" + expected + " actual=" + actual);
        }
    }
}

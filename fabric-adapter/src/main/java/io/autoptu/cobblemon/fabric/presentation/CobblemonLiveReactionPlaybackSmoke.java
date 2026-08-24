package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.battlecore.BattlePresentationCommand;
import io.autoptu.cobblemon.battlecore.PresentationEntityHandleRegistry;
import io.autoptu.cobblemon.battlecore.RegistryBackedPresentationEntityGateway;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Opt-in runtime smoke for ordered semantic reaction playback against a live Cobblemon entity.
 *
 * The cue and relocation are already-authoritative presentation outputs. This fixture verifies
 * entity binding and application order only. It does not evaluate Telepathy, threatened areas,
 * movement legality, action economy, hit cancellation, damage, abilities, items or terrain rules.
 */
public final class CobblemonLiveReactionPlaybackSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveReactionPlaybackSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Cobblemon reaction playback smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String RESERVATION_ID = "ci-live-reaction-playback";

    private CobblemonLiveReactionPlaybackSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(CobblemonLiveReactionPlaybackSmoke::run);
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
                spawn.getX() + 2,
                spawn.getY(),
                spawn.getZ() + 1
        );

        BattlePresentationCommand cue = new BattlePresentationCommand(
                41,
                0,
                BattlePresentationCommand.Kind.RULE_EFFECT_CUE,
                "pokemon-ally",
                Map.of(
                        "phase", "pre_damage_interrupt",
                        "authoritative", "true",
                        "effect", "reaction"
                )
        );

        gateway.showCue(RESERVATION_ID, presentationEntityId, cue);
        assertNear(origin.x() + 0.5D, entity.getX(), "cue x");
        assertNear(origin.y(), entity.getY(), "cue y");
        assertNear(origin.z() + 0.5D, entity.getZ(), "cue z");

        gateway.relocate(RESERVATION_ID, presentationEntityId, origin, destination);
        assertNear(destination.x() + 0.5D, entity.getX(), "relocation x");
        assertNear(destination.y(), entity.getY(), "relocation y");
        assertNear(destination.z() + 0.5D, entity.getZ(), "relocation z");

        LOGGER.info("{}: entity={} sequence={} destination={},{},{}", SUCCESS_LOG,
                presentationEntityId, cue.sequence(), destination.x(), destination.y(), destination.z());
        entity.discard();
        registry.releaseReservation(RESERVATION_ID);
    }

    private static void assertNear(double expected, double actual, String axis) {
        if (Math.abs(expected - actual) > 0.001D) {
            throw new IllegalStateException(
                    "live reaction playback " + axis + " mismatch: expected=" + expected + " actual=" + actual);
        }
    }
}

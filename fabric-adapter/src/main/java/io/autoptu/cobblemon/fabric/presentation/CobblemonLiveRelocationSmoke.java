package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.battlecore.EntityBoundBattleWorldRelocation;
import io.autoptu.cobblemon.battlecore.GatewayBackedBattleEntityBoundPresentationConsumer;
import io.autoptu.cobblemon.battlecore.PresentationEntityHandleRegistry;
import io.autoptu.cobblemon.battlecore.RegistryBackedPresentationEntityGateway;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in production-server smoke for the first real Cobblemon presentation operation.
 *
 * The Pokémon exists only as a presentation fixture. The destination is supplied as an already-
 * authoritative relocation output and no Cobblemon state is read to decide PTU legality.
 */
public final class CobblemonLiveRelocationSmoke {
    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String ENVIRONMENT_FLAG = "AUTOPTU_LIVE_RELOCATION_SMOKE";

    private CobblemonLiveRelocationSmoke() {
    }

    public static void registerIfRequested() {
        if (!"1".equals(System.getenv(ENVIRONMENT_FLAG))) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(CobblemonLiveRelocationSmoke::run);
        LOGGER.info("AutoPTU live Cobblemon relocation smoke armed");
    }

    static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PokemonEntity spawned = PokemonProperties.Companion
                .parse("pikachu level=5")
                .createEntity(world);
        spawned.setPosition(0.5D, 100.0D, 0.5D);
        if (!world.spawnEntity(spawned)) {
            throw new IllegalStateException("live relocation smoke could not spawn Cobblemon PokemonEntity");
        }

        String presentationEntityId = spawned.getUuid().toString();
        PokemonEntity resolved = new CobblemonPokemonEntityLookup()
                .find(server, presentationEntityId)
                .orElseThrow(() -> new IllegalStateException("spawned PokemonEntity was not found by presentation UUID"));
        if (resolved != spawned) {
            throw new IllegalStateException("presentation UUID lookup returned a different PokemonEntity instance");
        }

        String reservationId = "live-relocation-smoke";
        PresentationEntityHandleRegistry<PokemonEntity> registry = new PresentationEntityHandleRegistry<>();
        registry.register(reservationId, presentationEntityId, resolved);
        var gateway = new RegistryBackedPresentationEntityGateway<>(
                registry,
                new CobblemonPresentationEntityBackend()
        );
        var consumer = new GatewayBackedBattleEntityBoundPresentationConsumer(gateway);

        WorldBlockCoordinate origin = new WorldBlockCoordinate("minecraft:overworld", 0, 100, 0);
        WorldBlockCoordinate destination = new WorldBlockCoordinate("minecraft:overworld", 4, 100, 3);
        consumer.relocateEntity(
                reservationId,
                new EntityBoundBattleWorldRelocation(
                        1L,
                        0,
                        "smoke-pikachu",
                        presentationEntityId,
                        origin,
                        destination
                )
        );

        assertCoordinate("x", destination.x() + 0.5D, spawned.getX());
        assertCoordinate("y", destination.y(), spawned.getY());
        assertCoordinate("z", destination.z() + 0.5D, spawned.getZ());

        LOGGER.info(
                "AutoPTU live Cobblemon relocation smoke PASS: entity={} position=({}, {}, {})",
                presentationEntityId,
                spawned.getX(),
                spawned.getY(),
                spawned.getZ()
        );
        spawned.discard();
    }

    private static void assertCoordinate(String axis, double expected, double actual) {
        if (Math.abs(expected - actual) > 0.0001D) {
            throw new IllegalStateException(
                    "live relocation smoke " + axis + " mismatch: expected " + expected + " but was " + actual);
        }
    }
}

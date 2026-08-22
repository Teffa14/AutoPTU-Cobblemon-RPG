package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import io.autoptu.cobblemon.battlecore.EntityBoundBattleWorldRelocation;
import io.autoptu.cobblemon.battlecore.GatewayBackedBattleEntityBoundPresentationConsumer;
import io.autoptu.cobblemon.battlecore.PresentationEntityHandleRegistry;
import io.autoptu.cobblemon.battlecore.RegistryBackedPresentationEntityGateway;
import io.autoptu.cobblemon.battlecore.WorldBlockCoordinate;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
    private static final int MAX_LOOKUP_TICKS = 20;
    private static PendingSmoke pending;

    private CobblemonLiveRelocationSmoke() {
    }

    public static void registerIfRequested() {
        if (!"1".equals(System.getenv(ENVIRONMENT_FLAG))) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(CobblemonLiveRelocationSmoke::spawnFixture);
        ServerTickEvents.END_SERVER_TICK.register(CobblemonLiveRelocationSmoke::advance);
        LOGGER.info("AutoPTU live Cobblemon relocation smoke armed");
    }

    static void spawnFixture(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        PokemonEntity spawned = PokemonProperties.Companion
                .parse("pikachu level=5")
                .createEntity(world);
        spawned.setPosition(0.5D, 100.0D, 0.5D);
        if (!world.spawnEntity(spawned)) {
            throw new IllegalStateException("live relocation smoke could not spawn Cobblemon PokemonEntity");
        }

        pending = new PendingSmoke(spawned.getUuid().toString(), 0);
        LOGGER.info("AutoPTU live Cobblemon relocation smoke spawned fixture: {}", pending.presentationEntityId());
    }

    static void advance(MinecraftServer server) {
        PendingSmoke current = pending;
        if (current == null) {
            return;
        }

        var resolved = new CobblemonPokemonEntityLookup().find(server, current.presentationEntityId());
        if (resolved.isEmpty()) {
            int nextAttempt = current.lookupTicks() + 1;
            if (nextAttempt >= MAX_LOOKUP_TICKS) {
                pending = null;
                throw new IllegalStateException(
                        "spawned PokemonEntity was not indexed by presentation UUID after "
                                + MAX_LOOKUP_TICKS + " server ticks");
            }
            pending = new PendingSmoke(current.presentationEntityId(), nextAttempt);
            return;
        }

        pending = null;
        relocateAndVerify(resolved.orElseThrow(), current.presentationEntityId());
    }

    private static void relocateAndVerify(PokemonEntity entity, String presentationEntityId) {
        String reservationId = "live-relocation-smoke";
        PresentationEntityHandleRegistry<PokemonEntity> registry = new PresentationEntityHandleRegistry<>();
        registry.register(reservationId, presentationEntityId, entity);
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

        assertCoordinate("x", destination.x() + 0.5D, entity.getX());
        assertCoordinate("y", destination.y(), entity.getY());
        assertCoordinate("z", destination.z() + 0.5D, entity.getZ());

        LOGGER.info(
                "AutoPTU live Cobblemon relocation smoke PASS: entity={} position=({}, {}, {})",
                presentationEntityId,
                entity.getX(),
                entity.getY(),
                entity.getZ()
        );
        entity.discard();
    }

    private static void assertCoordinate(String axis, double expected, double actual) {
        if (Math.abs(expected - actual) > 0.0001D) {
            throw new IllegalStateException(
                    "live relocation smoke " + axis + " mismatch: expected " + expected + " but was " + actual);
        }
    }

    private record PendingSmoke(String presentationEntityId, int lookupTicks) {
    }
}

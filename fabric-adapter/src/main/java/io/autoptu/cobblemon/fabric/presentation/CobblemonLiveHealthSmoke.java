package io.autoptu.cobblemon.fabric.presentation;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import io.autoptu.cobblemon.battlecore.EntityBoundBattleHealthProjection;
import io.autoptu.cobblemon.battlecore.GatewayBackedBattleEntityBoundPresentationConsumer;
import io.autoptu.cobblemon.battlecore.PresentationEntityHandleRegistry;
import io.autoptu.cobblemon.battlecore.RegistryBackedPresentationEntityGateway;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Opt-in production-runtime smoke for authoritative HP presentation on a real PokemonEntity.
 *
 * The HP value is a test fixture standing in for an already-authoritative Java battle output.
 * Cobblemon only receives the display mirror; its health is never used to calculate or validate PTU HP.
 */
public final class CobblemonLiveHealthSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveHealthSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Cobblemon HP projection smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");
    private static final String RESERVATION_ID = "ci-live-health";
    private static final int AUTHORITATIVE_TARGET_HP = 5;
    private static final int AUTHORITATIVE_DAMAGE = 3;

    private CobblemonLiveHealthSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(CobblemonLiveHealthSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos spawn = world.getSpawnPos().up(2).east(2);

        Species species = PokemonSpecies.INSTANCE.getByName("pikachu");
        if (species == null) throw new IllegalStateException("Cobblemon Pikachu species is unavailable");

        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.setAiDisabled(true);
        entity.setPersistent();
        entity.refreshPositionAndAngles(spawn.getX() + 0.5D, spawn.getY(), spawn.getZ() + 0.5D, 0.0F, 0.0F);
        if (!world.spawnEntity(entity)) {
            throw new IllegalStateException("failed to spawn live Cobblemon PokemonEntity for HP smoke");
        }

        String presentationEntityId = entity.getUuidAsString();
        PokemonEntity resolved = new CobblemonPokemonEntityLookup()
                .find(server, presentationEntityId)
                .orElseThrow(() -> new IllegalStateException("HP smoke PokemonEntity was not resolvable by UUID"));

        PresentationEntityHandleRegistry<PokemonEntity> registry = new PresentationEntityHandleRegistry<>();
        registry.register(RESERVATION_ID, presentationEntityId, resolved);
        RegistryBackedPresentationEntityGateway<PokemonEntity> gateway =
                new RegistryBackedPresentationEntityGateway<>(registry, new CobblemonPresentationEntityBackend());
        GatewayBackedBattleEntityBoundPresentationConsumer consumer =
                new GatewayBackedBattleEntityBoundPresentationConsumer(gateway);

        consumer.projectHealth(
                RESERVATION_ID,
                new EntityBoundBattleHealthProjection(
                        1L,
                        0,
                        "smoke-pikachu-health",
                        presentationEntityId,
                        AUTHORITATIVE_DAMAGE,
                        AUTHORITATIVE_TARGET_HP
                )
        );

        int displayedHp = entity.getPokemon().getCurrentHealth();
        if (displayedHp != AUTHORITATIVE_TARGET_HP) {
            throw new IllegalStateException(
                    "live HP projection mismatch: expected=" + AUTHORITATIVE_TARGET_HP
                            + " actual=" + displayedHp);
        }

        LOGGER.info(
                "{}: entity={} targetHp={} damage={}",
                SUCCESS_LOG,
                presentationEntityId,
                AUTHORITATIVE_TARGET_HP,
                AUTHORITATIVE_DAMAGE
        );
        entity.discard();
        registry.releaseReservation(RESERVATION_ID);
    }
}

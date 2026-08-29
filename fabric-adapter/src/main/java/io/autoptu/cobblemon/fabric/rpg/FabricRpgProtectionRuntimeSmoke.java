package io.autoptu.cobblemon.fabric.rpg;

import com.cobblemon.mod.common.CobblemonEntities;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.entity.pokemon.PokemonEntity;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Live dedicated-server proof that Minecraft damage cannot decide Ouros actor HP/death. */
public final class FabricRpgProtectionRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveRpgProtectionSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live RPG actor damage protection smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricRpgProtectionRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricRpgProtectionRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos origin = world.getSpawnPos().up(30);
        PokemonEntity pokemon = spawnPokemon(world, origin);
        VillagerEntity ranger = spawnRanger(world, origin.east(2));
        try {
            assertMinecraftDamageRejected(world, pokemon, "Pokemon presentation actor");
            assertMinecraftDamageRejected(world, ranger, "canonical NPC presentation actor");
            LOGGER.info(SUCCESS_LOG);
        } finally {
            if (!pokemon.isRemoved()) pokemon.discard();
            if (!ranger.isRemoved()) ranger.discard();
        }
    }

    private static PokemonEntity spawnPokemon(ServerWorld world, BlockPos position) {
        Species species = PokemonSpecies.INSTANCE.getByName("bulbasaur");
        if (species == null) throw new IllegalStateException("Bulbasaur species unavailable for protection smoke");
        Pokemon pokemon = new Pokemon();
        pokemon.setSpecies(species);
        PokemonEntity entity = new PokemonEntity(world, pokemon, CobblemonEntities.POKEMON);
        entity.refreshPositionAndAngles(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
        if (!world.spawnEntity(entity)) throw new IllegalStateException("Pokemon protection smoke actor failed to spawn");
        return entity;
    }

    private static VillagerEntity spawnRanger(ServerWorld world, BlockPos position) {
        VillagerEntity ranger = new VillagerEntity(EntityType.VILLAGER, world);
        ranger.refreshPositionAndAngles(position.getX() + 0.5D, position.getY(), position.getZ() + 0.5D, 0.0F, 0.0F);
        FabricNpcDialogueRuntime.bind(ranger, "cedar-ranger");
        if (!world.spawnEntity(ranger)) throw new IllegalStateException("NPC protection smoke actor failed to spawn");
        return ranger;
    }

    private static void assertMinecraftDamageRejected(ServerWorld world, net.minecraft.entity.LivingEntity entity, String label) {
        float before = entity.getHealth();
        boolean accepted = entity.damage(world.getDamageSources().lava(), Math.max(4.0F, before + 1.0F));
        if (accepted) throw new IllegalStateException(label + " accepted Minecraft lava damage");
        if (entity.isDead() || entity.isRemoved()) throw new IllegalStateException(label + " died from Minecraft damage");
        if (Float.compare(before, entity.getHealth()) != 0) {
            throw new IllegalStateException(label + " health changed from Minecraft damage");
        }
    }
}

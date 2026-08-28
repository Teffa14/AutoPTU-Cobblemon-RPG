package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Live dedicated-server proof that the real Cobblemon PC is the accepted storage-terminal surface. */
public final class FabricPokemonStorageTerminalRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.livePokemonStorageTerminalSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live canonical Cobblemon PC terminal smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricPokemonStorageTerminalRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricPokemonStorageTerminalRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos pos = world.getSpawnPos().up(26);
        BlockState original = world.getBlockState(pos);
        Block cobblemonPc = Registries.BLOCK.get(FabricPokemonStorageTerminalRuntime.COBBLEMON_PC_ID);

        if (!FabricPokemonStorageTerminalRuntime.COBBLEMON_PC_ID.equals(Registries.BLOCK.getId(cobblemonPc))) {
            throw new IllegalStateException("Cobblemon 1.7.3 PC block is not registered as cobblemon:pc");
        }

        try {
            world.setBlockState(pos, cobblemonPc.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricPokemonStorageTerminalRuntime.isCanonicalPc(world, pos)) {
                throw new IllegalStateException("real Cobblemon PC was not recognized as canonical storage terminal");
            }

            world.setBlockState(pos, Blocks.CHEST.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricPokemonStorageTerminalRuntime.isCanonicalPc(world, pos)) {
                throw new IllegalStateException("vanilla storage block was accepted as canonical Pokemon PC");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(pos, original, Block.NOTIFY_ALL);
        }
    }
}

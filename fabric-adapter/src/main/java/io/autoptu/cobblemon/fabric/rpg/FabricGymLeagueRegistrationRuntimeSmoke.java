package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Dedicated-server proof that Gym/League registration uses only its explicit namespaced desk identity. */
public final class FabricGymLeagueRegistrationRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveGymLeagueDeskSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live Gym League registration desk smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricGymLeagueRegistrationRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)
                && !Boolean.getBoolean(FabricHealingStationRuntimeSmoke.ENABLE_PROPERTY)) {
            return;
        }
        ServerLifecycleEvents.SERVER_STARTED.register(FabricGymLeagueRegistrationRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos desk = world.getSpawnPos().up(34);
        BlockState original = world.getBlockState(desk);
        try {
            world.setBlockState(desk, FabricRpgContent.GYM_LEAGUE_REGISTRATION_DESK.getDefaultState(), Block.NOTIFY_ALL);
            if (!FabricGymLeagueRegistrationRuntime.isRegistrationDesk(world, desk)) {
                throw new IllegalStateException("namespaced Gym/League registration desk was not recognized");
            }

            world.setBlockState(desk, Blocks.CARTOGRAPHY_TABLE.getDefaultState(), Block.NOTIFY_ALL);
            if (FabricGymLeagueRegistrationRuntime.isRegistrationDesk(world, desk)) {
                throw new IllegalStateException("vanilla cartography table was accepted as canonical registration desk");
            }
            LOGGER.info(SUCCESS_LOG);
        } finally {
            world.setBlockState(desk, original, Block.NOTIFY_ALL);
        }
    }
}

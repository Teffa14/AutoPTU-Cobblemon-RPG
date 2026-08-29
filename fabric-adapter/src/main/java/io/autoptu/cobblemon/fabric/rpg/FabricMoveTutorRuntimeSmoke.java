package io.autoptu.cobblemon.fabric.rpg;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Live dedicated-server proof for the physical move-tutor actor binding. */
public final class FabricMoveTutorRuntimeSmoke {
    public static final String ENABLE_PROPERTY = "autoptu.liveMoveTutorSmoke";
    public static final String SUCCESS_LOG = "AutoPTU live canonical move tutor shell smoke passed";

    private static final Logger LOGGER = LoggerFactory.getLogger("autoptu-cobblemon-rpg");

    private FabricMoveTutorRuntimeSmoke() {}

    public static void registerIfEnabled() {
        if (!Boolean.getBoolean(ENABLE_PROPERTY)) return;
        ServerLifecycleEvents.SERVER_STARTED.register(FabricMoveTutorRuntimeSmoke::run);
    }

    private static void run(MinecraftServer server) {
        ServerWorld world = server.getOverworld();
        BlockPos pos = world.getSpawnPos().up(28);
        VillagerEntity tutor = new VillagerEntity(EntityType.VILLAGER, world);
        tutor.refreshPositionAndAngles(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D, 0.0F, 0.0F);
        FabricMoveTutorRuntime.bind(tutor);
        if (!FabricMoveTutorRuntime.isTutor(tutor)) {
            throw new IllegalStateException("bound move tutor villager was not recognized");
        }

        VillagerEntity ordinary = new VillagerEntity(EntityType.VILLAGER, world);
        if (FabricMoveTutorRuntime.isTutor(ordinary)) {
            throw new IllegalStateException("ordinary villager was incorrectly recognized as move tutor");
        }
        LOGGER.info(SUCCESS_LOG);
    }
}
